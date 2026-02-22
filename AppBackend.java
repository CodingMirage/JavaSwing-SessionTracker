import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AppBackend {

    private static final String appDataPath = System.getenv("APPDATA") + "\\SessionTracker";
    private static final String dbPath = appDataPath + "\\"+ConfigLoader.getProperty("LOCAL_DB"); // Path to SQLite database
    private static final String dbUrl = "jdbc:sqlite:" + dbPath;
    private static final String URL = ConfigLoader.getProperty("JDBC_URL_CLOUD");
    private static final String User = ConfigLoader.getProperty("JDBC_USERNAME_CLOUD");
    private static final String Password = ConfigLoader.getProperty("JDBC_PASSWORD_CLOUD");
    private static final String localTable = ConfigLoader.getProperty("LOCAL_TABLE");
    private static final String cloudConfigTable = ConfigLoader.getProperty("CLOUD_TABLE_CONFIG");
    private static final String localConfigTable = ConfigLoader.getProperty("LOCAL_TABLE_CONFIG");

    protected void setupDatabase() {
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
                                    CREATE TABLE IF NOT EXISTS %s (
                                        name TEXT NOT NULL,
                                        usn TEXT NOT NULL,
                                        login_time TEXT NOT NULL,
                                        logout_time TEXT,
                                        details TEXT,
                                        session_id TEXT primary key
                                    );
                                """.formatted(localTable);
                        stmt.execute(createTableSQL);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error occurred.");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
        }
    }

    // used to sync the config table from cloud to local
    public static void syncConfigurationTable() {
        try (
                Connection pgConn = DriverManager.getConnection(URL, User, Password);
                Connection sqliteConn = DriverManager.getConnection(dbUrl)
        ) {

            sqliteConn.setAutoCommit(false);

            Statement pgStmt = pgConn.createStatement();
            ResultSet rs = pgStmt.executeQuery("SELECT * FROM %s".formatted(cloudConfigTable));

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            Statement sqliteStmt = sqliteConn.createStatement();
            sqliteStmt.executeUpdate("DROP TABLE IF EXISTS %s".formatted(localConfigTable));

            // Build CREATE TABLE dynamically
            StringBuilder createSQL = new StringBuilder();
            createSQL.append("CREATE TABLE %s (".formatted(localConfigTable));

            for (int i = 1; i <= columnCount; i++) {
                String columnName = meta.getColumnName(i);
                String columnType = mapPostgresToSQLite(meta.getColumnTypeName(i));

                createSQL.append(columnName)
                        .append(" ")
                        .append(columnType);

                if (i < columnCount) createSQL.append(", ");
            }

            createSQL.append(")");

            sqliteStmt.executeUpdate(createSQL.toString());

            // Build INSERT dynamically
            StringBuilder insertSQL = new StringBuilder();
            insertSQL.append("INSERT INTO %s VALUES (".formatted(localConfigTable));

            for (int i = 1; i <= columnCount; i++) {
                insertSQL.append("?");
                if (i < columnCount) insertSQL.append(",");
            }
            insertSQL.append(")");

            PreparedStatement insertStmt = sqliteConn.prepareStatement(insertSQL.toString());

            // Insert rows dynamically
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    insertStmt.setObject(i, rs.getObject(i));
                }
                insertStmt.executeUpdate();
            }

            sqliteConn.commit();
            System.out.println("Dynamic sync complete!");

        } catch (SQLException e) {
            System.err.println("Database error occurred.");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
        }
    }

    private static String mapPostgresToSQLite(String pgType) {

        pgType = pgType.toLowerCase();

        return switch (pgType) {
            case "uuid", "varchar", "text", "timestamp", "timestamptz", "date" -> "TEXT";
            case "bool" -> "INTEGER";
            case "int2", "int4", "int8" -> "INTEGER";
            case "numeric", "float4", "float8" -> "REAL";
            default -> "TEXT";
        };
    }

    // Below function gets department and subject values from the config table created in local
    public static List<String> getDepartmentOrSubjectValues(String query) {
        List<String> departmentOrSubjectValues = new ArrayList<>();
        String sql = "SELECT item_value FROM %s WHERE category = ?".formatted(localConfigTable);

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, query);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    departmentOrSubjectValues.add(rs.getString("item_value"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error occurred.");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
        }

        return departmentOrSubjectValues;
    }

    // Below function inserts data into the local db name, usn and subcode are taken from user
    protected void insertData(String name,String usn,String subcode) {
        String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sessionId = UUID.randomUUID().toString();
        String insertSQL = """
            INSERT INTO %s (name, usn, login_time, details, session_id)
            VALUES (?, ?, ?, ?, ?);
        """.formatted(localTable);

        String sys_no = ConfigLoader.getProperty("SYSTEM_NO");
        String[] sys_info = sys_no.split("-");

        // Creates key-value pairs of department shorthand and names
        List<String> dept = getDepartmentOrSubjectValues("Department");
        Map<String, String> deptMap = new HashMap<>();
        for (String s : dept) {
            String[] parts = s.split(" ");
            if (parts.length == 2) {
                // Normalize key: remove parentheses and lowercase
                String key = parts[1].replace("(", "").replace(")", "").toLowerCase();
                deptMap.put(key, parts[0]);
            }
        }
        String deptSh = usn.substring(5, 7).toLowerCase(); // Respective department is chosen from the shorthand present in USN
        String Department = deptMap.getOrDefault(deptSh, "Invalid");




        int year = LocalDate.now().getYear()%2000 - Integer.parseInt(usn.substring(3, 5)) + 1;
        int sem = LocalDate.now().getMonthValue() < 6 ? year*2-2/*even sem*/ : year*2 -1/*odd*/ ;
        int usnNumber = Integer.parseInt(usn.substring(7));
        String batch = (usnNumber < 30 || (usnNumber>60 && usnNumber<90)) ? "I" : "II" ;

        String details = String.format(
                "{\"LabName\":\"%s\", \"SysNo\":\"%s\", \"Sem\":\"%s\", \"Department\":\"%s\", \"Subject\":\"%s\", \"Batch\":\"%s\"}",
                sys_info[0], sys_info[1], sem, Department, subcode, batch
        );

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, name);
            pstmt.setString(2, usn);
            pstmt.setString(3, loginTime);
            pstmt.setString(4, details);
            pstmt.setString(5, sessionId);
            pstmt.executeUpdate();




        } catch (SQLException e) {
            System.err.println("Database error occurred.");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
        }
    }
}
