import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.File;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class AppBackend {
    static Map<String, List<String>> configMap = new LinkedHashMap<>();

    protected void setupDatabase() {
        String localDbUrl = ConfigLoader.getLocalDBUrl();

        // Creates Configuration Table if doesn't exit
        OptionsManager.createConfigurationTableLocal(localDbUrl);
        CloudDatabaseUpload.fetchConfigurationFromCloud();

        try {
            // Connect to the database
            try (Connection conn = DriverManager.getConnection(localDbUrl)) {
                if (conn != null) {
                    try (Statement stmt = conn.createStatement()) {
                        // Creates the sessions table if it doesn't already exist
                        OptionsManager.createRecordsTableLocal(conn);                                
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }

        configMap = CloudDatabaseUpload.loadConfigMap(localDbUrl);

    }

    protected String[] getData(String name, String usn, String sub) {

        int year = LocalDate.now().getYear() % 2000 - Integer.parseInt(usn.substring(3, 5)) + 1;
        int sem = LocalDate.now().getMonthValue() < 6 ? year * 2 - 2/* even sem */ : year * 2 - 1/* odd */ ;
        int usnNumber = Integer.parseInt(usn.substring(7));
        String batch = (usnNumber < 30 || (usnNumber > 60 && usnNumber < 90)) ? "I" : "II";

        String deptCode = usn.substring(5, 7);
        List<String> depts = configMap.get("Department");

        String dept = new String();
        for(String dep : depts) {
            if (dep.toLowerCase().contains(deptCode.toLowerCase())) {
                dept = dep;
                break;
            }
        }

        return new String[]{ name, usn, String.valueOf(sem), dept, sub, batch };
    }

    protected void insertData(String[] StudentData) {

        String localDbUrl = ConfigLoader.getLocalDBUrl();
        String user = ConfigLoader.config.getProperty("JDBC_USERNAME_cloud");
        String pass = ConfigLoader.config.getProperty("JDBC_PASSWORD_cloud");

        String insertSQL = """
                INSERT INTO {TABLE} (name, usn, login_time, details,logout_time, session_id)
                VALUES (?, ?, ?, ?, ?, ?);
                """.replace("{TABLE}", ConfigLoader.config.getProperty("LOCAL_TABLE"));

        try (Connection conn = DriverManager.getConnection(localDbUrl, user, pass);
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, StudentData[0]);
            pstmt.setString(2, StudentData[1]);
            pstmt.setString(3, StudentData[2]);
            pstmt.setString(4, StudentData[3]);
            pstmt.setString(5, StudentData[4]);
            pstmt.setString(6, StudentData[5]);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }
    }
}
