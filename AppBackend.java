import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


public class AppBackend {
    protected void setupDatabase() {
        String appDataPath = System.getenv("APPDATA") + "\\SessionTracker";
        String dbPath = appDataPath + "\\user_sessions.db"; // Path to SQLite database
        String dbUrl = "jdbc:sqlite:" + dbPath;

        try {
            // Create the directory for the database if it doesn't exist
            File dbDir = new File(appDataPath);
            boolean dirsCreated = dbDir.mkdirs();  // Returns true if the directories were created

            if (dirsCreated) {
                System.out.println("Directories were successfully created.");
            } else {
                System.out.println("Directories already exist or could not be created.");
            }

            // Connect to the database
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                if (conn != null) {
                    try (Statement stmt = conn.createStatement()) {
                        // Creates the sessions table if it doesn't already exist
                        String createTableSQL = """
                        CREATE TABLE IF NOT EXISTS sessions (
                            name TEXT NOT NULL,
                            usn TEXT NOT NULL,
                            login_time TEXT NOT NULL,
                            logout_time TEXT,
                            sem TEXT,
                            dept TEXT,
                            batch TEXT,
                            session_id TEXT primary key
                        );
                    """;
                        stmt.execute(createTableSQL);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }
    }


    protected void insertData(String name,String usn) {
        String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String dbUrl = "jdbc:sqlite:"+System.getenv("APPDATA")+"\\SessionTracker"+"\\user_sessions.db";
        String sessionId = UUID.randomUUID().toString();
        String insertSQL = """
            INSERT INTO sessions (name, usn, login_time, sem, dept, batch, session_id)
            VALUES (?, ?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, name);
            pstmt.setString(2, usn);
            pstmt.setString(3, loginTime);
            int year = LocalDate.now().getYear()%2000 - Integer.parseInt(usn.substring(3, 5)) + 1;
            int sem = LocalDate.now().getMonthValue() < 6 ? year*2-2/*even sem*/ : year*2 -1/*odd*/ ;
            int usnNumber = Integer.parseInt(usn.substring(7));
            String batch = (usnNumber < 30 || (usnNumber>60 && usnNumber<90)) ? "I" : "II" ;
            System.out.println(batch);
            pstmt.setInt(4, sem);
            if (usn.contains("IS")) pstmt.setString(5, "ISE");
            else if(usn.contains("CS")) pstmt.setString(5, "CSE");
            else if(usn.contains("AI")) pstmt.setString(5, "AIML");
            else if(usn.contains("CD")) pstmt.setString(5, "DS");
            else if(usn.contains("EC")) pstmt.setString(5, "ECE");
            else if(usn.contains("ME")) pstmt.setString(5, "MECH");
            else if (usn.contains("CV")) pstmt.setString(5, "CIVIL");
            else pstmt.setString(5, "Invalid");
            pstmt.setString(6, batch);
            pstmt.setString(7, sessionId);
            
            pstmt.executeUpdate();




        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }
    }
}
