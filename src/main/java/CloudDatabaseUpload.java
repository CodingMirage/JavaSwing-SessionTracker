package org.Miniproject;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudDatabaseUpload {
    //Uploads the local batch into cloud
    private static void uploadBatch(List<Map<String, String>> records) {

        try {
            String projectUrl = ConfigLoader.getProperty("PROJECT_URL");
            String anonKey = ConfigLoader.getProperty("ANON_KEY");
            String functionName = ConfigLoader.getProperty("CLOUD_FUNCTION_INSERT");

            String apiUrl = projectUrl + "/functions/v1/client-api";

            HttpURLConnection connection =
                    (HttpURLConnection) URI.create(apiUrl).toURL().openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + anonKey);
            connection.setRequestProperty("X-Function", functionName);

            connection.setDoOutput(true);

            Gson gson = new Gson();
            String jsonPayload = gson.toJson(records);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();

            InputStream stream =
                    (responseCode == 201) ? connection.getInputStream() : connection.getErrorStream();

            String response = "";

            if (stream != null) {
                response = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (responseCode == 201) {
                System.out.println("Batch uploaded to cloud. Rows synced: " + records.size());
            } else {
                System.err.println("Cloud batch sync failed.");
                System.err.println("HTTP Code: " + responseCode);
                System.err.println("Server Response: " + response);
            }

        } catch (Exception e) {
            System.err.println("Error uploading batch to cloud.");
            e.printStackTrace();
        }
    }

    // Syncs local SQLite database to remote cloud database by creating the payload and calling uploadBatch
    protected static void syncLocalDataToRemote() {

        String appDataPath = System.getenv("APPDATA") + "\\SessionTracker";
        String dbPath = appDataPath + "\\" + ConfigLoader.getProperty("LOCAL_DB");

        String localDbUrl = "jdbc:sqlite:" + dbPath;

        String selectSql = "SELECT * FROM " + ConfigLoader.getProperty("LOCAL_TABLE");

        List<Map<String, String>> batchRecords = new ArrayList<>();

        try (Connection localConn = DriverManager.getConnection(localDbUrl);
             PreparedStatement ps = localConn.prepareStatement(selectSql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                Map<String, String> payloadMap = new HashMap<>();
                payloadMap.put("name", rs.getString(1));
                payloadMap.put("usn", rs.getString(2));
                payloadMap.put("login_time", rs.getString(3));
                payloadMap.put("logout_time", rs.getString(4));
                payloadMap.put("details", rs.getString(5));
                payloadMap.put("session_id", rs.getString(6));

                batchRecords.add(payloadMap);
            }

            if (!batchRecords.isEmpty()) {
                uploadBatch(batchRecords);
            }

        } catch (SQLException e) {
            System.err.println("Database error occurred.");
            System.err.println("Message: " + e.getMessage());
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("ErrorCode: " + e.getErrorCode());
        }
    }
}