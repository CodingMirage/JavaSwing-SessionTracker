package org.Miniproject;

import com.google.gson.Gson;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AppBackend {

    private static final String appDataPath = System.getenv("APPDATA") + "\\SessionTracker";
    private static final String dbPath = appDataPath + "\\"+ ConfigLoader.getProperty("LOCAL_DB"); // Path to SQLite database
    private static final String dbUrl = "jdbc:sqlite:" + dbPath;
    private static final String projectUrl = ConfigLoader.getProperty("PROJECT_URL");
    private static final String anonKey = ConfigLoader.getProperty("ANON_KEY");
    private static final String localTable = ConfigLoader.getProperty("LOCAL_TABLE");
    private static final String syncFunction = ConfigLoader.getProperty("CLOUD_FUNCTION_SYNC");
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

    static class ConfigItem {
        String category;
        String item_value;
    }

    // used to sync the config table from cloud to local
    public static void syncConfigurationTable() {


        try (Connection sqliteConn = DriverManager.getConnection(dbUrl)) {

            sqliteConn.setAutoCommit(false);


            String apiUrl = projectUrl + "/functions/v1/client-api";

            HttpURLConnection connection =
                    (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();

            connection.setRequestMethod("POST"); // still POST
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + anonKey);
            connection.setRequestProperty("X-Function", syncFunction);

            int responseCode = connection.getResponseCode();

            if (responseCode != 200) {
                System.err.println("Failed to fetch config from cloud. HTTP Code: " + responseCode);
                return;
            }

            InputStream stream = connection.getInputStream();
            String jsonResponse = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            Gson gson = new Gson();
            ConfigItem[] items = gson.fromJson(jsonResponse, ConfigItem[].class);


            Statement sqliteStmt = sqliteConn.createStatement();

            // Recreate table
            sqliteStmt.executeUpdate("DROP TABLE IF EXISTS " + localConfigTable);
            sqliteStmt.executeUpdate("""
                CREATE TABLE %s (
                    category TEXT,
                    item_value TEXT
                )
                """.formatted(localConfigTable));
            sqliteConn.commit();

            PreparedStatement insertStmt = sqliteConn.prepareStatement(
                    "INSERT INTO %s (category, item_value) VALUES (?, ?)".formatted(localConfigTable)
            );

            for (ConfigItem item : items) {
                insertStmt.setString(1, item.category);
                insertStmt.setString(2, item.item_value);
                insertStmt.executeUpdate();
            }

            sqliteConn.commit();
            System.out.println("Config table synced successfully!");

        } catch (Exception e) {
            System.err.println("Error syncing configuration table.");
            e.printStackTrace();
        }
    }

    // Below function gets department and subject values from the config table created in local
    public static List<String> getConfigValues(String query) {
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

    //This function collects all the required details
    public static ArrayList<String> getDetails(String usn) {

        String sys_no = ConfigLoader.getProperty("SYSTEM_NO");
        String Lab_name = ConfigLoader.getProperty("LAB_NAME");

        // Creates key-value pairs of department shorthand and names
        List<String> dept = getConfigValues("Department");
        Map<String, String> deptMap = new HashMap<>();

        for (String s : dept) {
            int start = s.indexOf("(");
            int end = s.indexOf(")");

            if (start != -1 && end != -1) {
                String key = s.substring(start + 1, end).toLowerCase();
                deptMap.put(key, s);
            }
        }
        String deptSh = usn.substring(5, 7).toLowerCase();
        String Department = deptMap.getOrDefault(deptSh, "Invalid");

        int year = LocalDate.now().getYear()%2000 - Integer.parseInt(usn.substring(3, 5)) + 1;
        int sem = LocalDate.now().getMonthValue() < 6 ? year*2-2/*even sem*/ : year*2 -1/*odd*/ ;
        int usnNumber = Integer.parseInt(usn.substring(7));
        String batch = (usnNumber < 30 || (usnNumber>60 && usnNumber<90)) ? "I" : "II" ;

        return new ArrayList<>(List.of(Lab_name, sys_no, String.valueOf(sem), Department, batch));
    }

    // Below function inserts data into the local db name, usn and subcode are taken from user
    protected void insertData(ArrayList<String> Details) {
        String loginTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String sessionId = UUID.randomUUID().toString();
        String insertSQL = """
            INSERT INTO %s (name, usn, login_time, details, session_id)
            VALUES (?, ?, ?, ?, ?);
        """.formatted(localTable);


        String details_json = String.format(
                "{\"LabName\":\"%s\", \"SysNo\":\"%s\", \"Sem\":\"%s\", \"Department\":\"%s\", \"Subject\":\"%s\", \"Batch\":\"%s\"}",
                Details.get(3), Details.get(4), Details.get(5), Details.get(6), Details.get(2), Details.get(7)
        );

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, Details.get(0));
            pstmt.setString(2, Details.get(1));
            pstmt.setString(3, loginTime);
            pstmt.setString(4, details_json);
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
