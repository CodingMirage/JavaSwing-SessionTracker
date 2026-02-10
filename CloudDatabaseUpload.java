import java.sql.*;

public class CloudDatabaseUpload {

    protected static void insertDataRemote(String name, String usn, String login_time, String logout_time, String sem, String dept, String batch, String sub, String sessionId) {
        String JDBC_URL = "your_DB_URL_goes_in_here";
        try (Connection con = DriverManager.getConnection(JDBC_URL)) {
            String insertData = " INSERT INTO sessions (name, usn, login_time, logout_time, sem, dept, sub, batch, session_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?); ";

            try(PreparedStatement ps = con.prepareStatement(insertData)) {
                ps.setString(1, name);
                ps.setString(2, usn);
                ps.setString(3, login_time);
                ps.setString(4, logout_time);
                ps.setString(5, sem);
                ps.setString(6, dept);
                ps.setString(7, sub);
                ps.setString(8, batch);
                ps.setString(9, sessionId);
                ps.execute();
            }
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }

    }

    protected static void syncLocalDataToRemote() {
        String appDataPath = System.getenv("APPDATA") + "\\SessionTracker";
        String dbPath = appDataPath + "\\user_sessions.db"; // Path to SQLite database
        String localDbUrl = "jdbc:sqlite:"+ dbPath; // Your local database;
        String selectSql = "SELECT * FROM sessions";

        try (Connection localConn = DriverManager.getConnection(localDbUrl);
             PreparedStatement ps = localConn.prepareStatement(selectSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                insertDataRemote(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8),rs.getString(9));

            }
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }
    }
}
