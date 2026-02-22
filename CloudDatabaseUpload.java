import java.sql.*;

public class CloudDatabaseUpload {

    protected static void insertDataRemote(String name, String usn, String login_time, String logout_time, String details, String sessionId) {
        String URL = ConfigLoader.getProperty("JDBC_URL_CLOUD");
        String User = ConfigLoader.getProperty("JDBC_USERNAME_CLOUD");
        String Password = ConfigLoader.getProperty("JDBC_PASSWORD_CLOUD");
        String cloudTable = ConfigLoader.getProperty("CLOUD_TABLE");
        try (Connection con = DriverManager.getConnection(URL, User, Password)) {
            // An upsert statement to insert and update local table into cloud (cloud syncing)
            String insertData =
                   """
                   INSERT INTO %s (name, usn, login_time, logout_time, details, session_id)
                   VALUES (?, ?, ?, ?, ?, ?)
                   ON CONFLICT (session_id)
                   DO UPDATE SET
                   logout_time = COALESCE(EXCLUDED.logout_time, %s.logout_time);
                   """.formatted(cloudTable, cloudTable);

            try(PreparedStatement ps = con.prepareStatement(insertData)) {
                ps.setString(1, name);
                ps.setString(2, usn);
                ps.setString(3, login_time);
                ps.setString(4, logout_time);
                ps.setString(5,details);
                ps.setString(6, sessionId);
                ps.execute();
            }
        } catch (SQLException e) {
            System.err.println("Database error occurred.");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
        }

    }

    // Below function gets the values from the local db and calls the CloudDatabaseUpload function
    protected static void syncLocalDataToRemote() {
        String appDataPath = System.getenv("APPDATA") + "\\SessionTracker";
        String dbPath = appDataPath + "\\"+ConfigLoader.getProperty("LOCAL_DB"); // Path to SQLite database
        String localDbUrl = "jdbc:sqlite:"+ dbPath; // Your local database;
        String selectSql = "SELECT * FROM "+ConfigLoader.getProperty("LOCAL_TABLE");

        try (Connection localConn = DriverManager.getConnection(localDbUrl);
             PreparedStatement ps = localConn.prepareStatement(selectSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                insertDataRemote(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6));

            }
        } catch (SQLException e) {
            System.err.println("Database error occurred.");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
        }
    }
}
