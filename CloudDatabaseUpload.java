import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CloudDatabaseUpload {

    protected static void insertDataRemote(String name, String usn, String login_time, String logout_time, String details, String sessionId) {
        String JDBC_URL = "your_DB_URL_goes_in_here";
        try (Connection con = DriverManager.getConnection(JDBC_URL)) {
            String insertData = " INSERT INTO sessions (name, usn, login_time, logout_time, details, session_id) VALUES (?, ?, ?, ?, ?, ?); ";

            try(PreparedStatement ps = con.prepareStatement(insertData)) {
                ps.setString(1, name);
                ps.setString(2, usn);
                ps.setString(3, login_time);
                ps.setString(4, logout_time);
                ps.setString(5, details);
                ps.setString(6, sessionId);
                ps.execute();
            }
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }

    }

    protected static void syncLocalDataToRemote() {
        String localDbUrl = ConfigLoader.getLocalDBUrl();
        String cloudDbUrl = ConfigLoader.config.getProperty("JDBC_URL_cloud");
        String user = ConfigLoader.config.getProperty("JDBC_USERNAME_cloud");
        String pass = ConfigLoader.config.getProperty("JDBC_PASSWORD_cloud");

        String selectSql = "SELECT name, usn, login_time, logout_time, details, session_id FROM " + ConfigLoader.config.getProperty("LOCAL_TABLE");
        String insertSql = "INSERT INTO " + ConfigLoader.config.getProperty("CLOUD_TABLE") + " (name, usn, login_time, logout_time, details, session_id) VALUES (?, ?, ?, ?, ?, ?);"; 

        try (
            Connection localConn = DriverManager.getConnection(localDbUrl);
            Connection cloudConn = DriverManager.getConnection(cloudDbUrl, user, pass);
            PreparedStatement selectStmt = localConn.prepareStatement(selectSql);
            PreparedStatement insertStmt = cloudConn.prepareStatement(insertSql);
            ResultSet rs = selectStmt.executeQuery();
        ) {
            int count = 0;
            while (rs.next()) {
                insertStmt.setString(1, rs.getString("name"));
                insertStmt.setString(2, rs.getString("usn"));
                insertStmt.setString(3, rs.getString("login_time"));
                insertStmt.setString(4, rs.getString("logout_time"));
                insertStmt.setString(5, rs.getString("details"));
                insertStmt.setString(6, rs.getString("session_id"));
                insertStmt.addBatch();
                count++;
            }
            
            if (count > 0) {
                insertStmt.executeBatch();
                System.out.println("Successfully synced " + count + " records to Cloud.");
            }

        } catch (SQLException e) {
            System.err.println("\u26A0 Sync Error: " + e.getMessage());
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }
    }

    public static void fetchConfigurationFromCloud() {
        String localDbUrl = ConfigLoader.getLocalDBUrl();
        String cloudDbUrl = ConfigLoader.config.getProperty("JDBC_URL_cloud");
        String user = ConfigLoader.config.getProperty("JDBC_USERNAME_cloud");
        String pass = ConfigLoader.config.getProperty("JDBC_PASSWORD_cloud");
        
        // Select everything from Cloud
        String selectCloudSql = "SELECT category, item_value FROM " + ConfigLoader.config.getProperty("CONFIGURATION_TABLE");
            
        // Clear and Insert into Local
        String deleteLocalSql = "DELETE FROM " + ConfigLoader.config.getProperty("CONFIGURATION_TABLE");
        String insertLocalSql = "INSERT INTO "+ ConfigLoader.config.getProperty("CONFIGURATION_TABLE")+ " (category, item_value) VALUES (?, ?)";

        try (
            Connection cloudConn = DriverManager.getConnection(cloudDbUrl,user,pass);
            Connection localConn = DriverManager.getConnection(localDbUrl);
            PreparedStatement selectStmt = cloudConn.prepareStatement(selectCloudSql);
            PreparedStatement deleteStmt = localConn.prepareStatement(deleteLocalSql);
            PreparedStatement insertStmt = localConn.prepareStatement(insertLocalSql);
            ResultSet rs = selectStmt.executeQuery()
        ) {
            localConn.setAutoCommit(false);
            
            deleteStmt.executeUpdate();

            int count = 0;
            while (rs.next()) {
                insertStmt.setString(1, rs.getString("category"));
                insertStmt.setString(2, rs.getString("item_value"));
                insertStmt.addBatch();
                count++;
            }

            if (count > 0) {
                insertStmt.executeBatch();
                localConn.commit();
                System.out.println("Successfully synced config table from Cloud.");
            }
            
        } catch (SQLException e) {
            System.err.println("Configuration Sync Error: " + e.getMessage());
        }
    }

    protected static Map<String, List<String>> loadConfigMap(String JDBC_URL_local) {
		Map<String, List<String>> map = new LinkedHashMap<>(); // preserves the order of categories
		String confTable = ConfigLoader.config.getProperty("CONFIGURATION_TABLE");
		
		try (Connection conn = DriverManager.getConnection(JDBC_URL_local);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT category, item_value FROM " + confTable + " ORDER BY category, id")) {

			while (rs.next()) {
				String category = rs.getString("category");
				String value = rs.getString("item_value");
				
				// If it's a new category, initialize an empty list 
				map.computeIfAbsent(category, k -> {
					return new ArrayList<>();
				});
				
				map.get(category).add(value);
			}
		} catch (SQLException e) {
			System.err.println("Error loading configMap from local DB");
			e.printStackTrace();
		}
		return map;
	}

}
