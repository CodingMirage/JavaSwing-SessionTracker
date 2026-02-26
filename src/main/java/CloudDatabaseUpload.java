import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// JSON parsing
import com.google.gson.Gson;    

public class CloudDatabaseUpload {

    public static void fetchConfigurationFromCloud() {
        String localDbUrl = ConfigLoader.getLocalDBUrl();
        String confTable = ConfigLoader.config.getProperty("CONFIGURATION_TABLE");

        // Fetch data from cloud
        Object response = CloudAPI.callEdgeFunction("fetch-all-config", "{}");
        
        if (!(response instanceof List)) {
            System.err.println("Configuration Sync Failed: Invalid response or Cloud Unreachable.");
            return; 
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cloudData = (List<Map<String, Object>>) response;

        try (Connection localConn = DriverManager.getConnection(localDbUrl)) {
            localConn.setAutoCommit(false);

            // Clear local table
            try (Statement st = localConn.createStatement()) {
                st.executeUpdate("DELETE FROM " + confTable);
            }

            // Insert (sync)
            String insertLocalSql = "INSERT INTO " + confTable + " (category, item_value) VALUES (?, ?)";
            try (PreparedStatement insertStmt = localConn.prepareStatement(insertLocalSql)) {
                for (Map<String, Object> row : cloudData) {
                    String category = String.valueOf(row.get("category"));
                    String itemValue = String.valueOf(row.get("item_value"));
                    
                    insertStmt.setString(1, category);
                    insertStmt.setString(2, itemValue.toUpperCase());
                    insertStmt.addBatch();
                }
                
                insertStmt.executeBatch();
                localConn.commit();
                System.out.println("✓ Synced " + cloudData.size() + " config items from Cloud.");
            }
        } catch (SQLException e) {
            System.err.println("Database Error during config sync: " + e.getMessage());
        }

        // Update memory cache so UI reflects changes immediately
        AppBackend.configMap = loadConfigMap(localDbUrl);
    }

    protected static void syncLocalDataToRemote() {
        String localDbUrl = ConfigLoader.getLocalDBUrl();
        String localTable = ConfigLoader.config.getProperty("LOCAL_TABLE");
        String deleteSql = "DELETE FROM " + localTable + " WHERE session_id = ?";

        List<Map<String, Object>> recordsToSync = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(localDbUrl)) {
            conn.setAutoCommit(false); 

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                String selectSql = "SELECT name, usn, login_time, logout_time, details, session_id FROM " + localTable;
                
                // Fetch data from local
                try (ResultSet rs = conn.createStatement().executeQuery(selectSql)) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("name", rs.getString("name"));
                        row.put("usn", rs.getString("usn"));
                        row.put("login_time", rs.getString("login_time"));
                        row.put("logout_time", rs.getString("logout_time"));
                        row.put("details", rs.getString("details"));
                        row.put("session_id", rs.getString("session_id"));
                        recordsToSync.add(row);

                        // Prepare the deletion batch while reading
                        deleteStmt.setString(1, rs.getString("session_id"));
                        deleteStmt.addBatch();
                    }
                }

                if (recordsToSync.isEmpty()) return;

                // Attempt Cloud Upload
                String jsonPayload = new Gson().toJson(recordsToSync);
                Object response = CloudAPI.callEdgeFunction("insert-records", jsonPayload);

                // ONLY Commit local deletion if Cloud confirmed success
                if (response != null) {
                    deleteStmt.executeBatch();
                    conn.commit(); 
                    System.out.println("✓ Successfully synced and cleared " + recordsToSync.size() + " records.");
                } else {
                    conn.rollback();
                    System.err.println("⚠ Sync Failed: Cloud unreachable. Records kept in local storage.");
                }
            }
        } catch (SQLException e) { 
            System.err.println("DB Error during sync: " + e.getMessage());
            e.printStackTrace(); 
        }
    }    
    
    protected static Map<String, List<String>> loadConfigMap(String JDBC_URL_local) {
		Map<String, List<String>> map = new LinkedHashMap<>(); // preserves the order of categories
		String confTable = ConfigLoader.config.getProperty("CONFIGURATION_TABLE");
		
		try (Connection conn = DriverManager.getConnection(JDBC_URL_local);
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT category, item_value FROM " + confTable + " ORDER BY category, item_value")) {

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
