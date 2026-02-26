import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

// JSON parsing
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;


class HelperFunctions {
    public static void performSyncWithProgress(Window parent, Runnable syncTask, Runnable onComplete) {
        JDialog syncDialog = new JDialog(parent, "Data Sync", Dialog.ModalityType.APPLICATION_MODAL);
        syncDialog.setUndecorated(true);
        
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        
        JLabel label = new JLabel("\u27F3 Syncing with Cloud... Please wait.", JLabel.CENTER);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // Spinning effect
        
        panel.add(label, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        syncDialog.add(panel);
        syncDialog.pack();
        syncDialog.setLocationRelativeTo(parent);

        // Start the background thread
        new Thread(() -> {
            try {
                syncTask.run();  
            } finally {
                SwingUtilities.invokeLater(() -> {
                    syncDialog.dispose();
                    onComplete.run(); // when background thread finishes 
                });
            }
        }).start();

        // Show the dialog until background thread is running to avoid user interaction 
        syncDialog.setVisible(true);
    }

    public static void refreshSubjects() {

    }

}

class CloudAPI {
    
    public static Object callEdgeFunction(String func, String jsonData) {

        String anonKey = ConfigLoader.getAnonKey();

        final String BASE_URL = ConfigLoader.getProjectUrl()+"/functions/v1/client-api";

        // Parse the json to List of records
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL))
            .header("Authorization", "Bearer " + anonKey)
            .header("X-Function", func)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonData))
            .build();

        try {
            // Receive response after sending request
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 400) {
                System.err.println("Cloud Error (" + response.statusCode() + "): " + response.body());
                return null;
            }
            
            String body = response.body();
            // Parse the body into list
            if (body.trim().startsWith("[")) {
                return gson.fromJson(body, listType);
            } else {
                return gson.fromJson(body, Map.class);
            }
        } catch (Exception e) {
            System.err.println("Network/Parsing Error: " + e.getMessage());
            return null;
        }
    }
}


class ConfigLoader {

    // A single, static instance of Properties accessible application-wide
    static final Properties config = new Properties();

    // --- Persistence Setup (Safe Location ) ---
    private static final String APP_DIR_NAME = "SessionTracker";
    private static final String CONFIG_FILE_NAME = "config.properties";

    protected static final Path CONFIG_DIR_PATH = Paths.get(System.getProperty("user.home"), "." + APP_DIR_NAME);
    private static final Path CONFIG_FILE_PATH = CONFIG_DIR_PATH.resolve(CONFIG_FILE_NAME);
    
    // The internal name of the template file inside the JAR
    private static final String CONFIG_TEMPLATE_NAME = "config.properties";

    static {
        File persistentConfigFile = CONFIG_FILE_PATH.toFile(); 

        boolean dirsCreated = persistentConfigFile.getParentFile().mkdirs();  // Returns true if the directories were created

        // Ensure the application directory exists before checking for the file
        if (dirsCreated) {
            System.out.println("Created Application Folder at " + CONFIG_DIR_PATH);
        }


        System.out.println("Checking for persistent config file");

        // --- PHASE 1: Try to read the persistent (user-edited) file ---
        if (persistentConfigFile.exists()) {
            try (FileReader reader = new FileReader(persistentConfigFile)) {
                config.load(reader);
                System.out.println("Persistent configuration loaded successfully.");
            } catch (IOException e) {
                // If we found it but couldn't read it (e.g., permissions issue)
                System.err.println("Error reading existing persistent config file: " + e.getMessage());
            }
        } else {
            // --- PHASE 2: If persistent file is missing, load the default template ---
            System.out.println("Persistent file not found. Loading default template.");
            
            // USE CLASSLOADER for reading the resource from inside the JAR
            try (InputStream templateStream = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_TEMPLATE_NAME)) {
                
                if (templateStream == null) {
                    throw new FileNotFoundException("\u26A0 CRITICAL: Default template file " +  CONFIG_TEMPLATE_NAME + " not found inside the application!");
                } else {
                    config.load(templateStream);
                    System.out.println("Default configuration loaded successfully.");
                    
                    // Immediately create the persistent file based on the template content
                    saveProperties();
                    System.out.println("New persistent config file created from deafult template.");
                }

            } catch (IOException e) {
                System.err.println("Error reading template or creating new file: " + e.getMessage());
            }
        }

        // ---- PHASE 3: Initialise Database if not exist ---
        File dbFile = CONFIG_DIR_PATH.resolve(config.getProperty("JDBC_URL_local")).toFile();
        
        if (!dbFile.exists()) {
            System.out.println("First run detected: Initializing Database...");

            // Initialize the SQLite table structure
            OptionsManager.createConfigurationTableLocal(getLocalDBUrl()); 
            
            // Refresh the memory map immediately so the UI is ready
            AppBackend.configMap = CloudDatabaseUpload.loadConfigMap(getLocalDBUrl());
        } 
    }

    /*
    Delete Application Folder created.
     */
    public static void deleteConfigFolder(Path path) {
        if (!Files.exists(path)) {
            System.out.println("No configuration folder found at: " + path);
            return;
        }
        try {
            Files.walk(path)
                .sorted(Comparator.reverseOrder()) // Delete children before parents
                .map(Path::toFile)
                .forEach(File::delete);
            System.out.println("\u26A0 CRITICAL: Deleted Application Configuration Folder");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to delete Application Configuration Folder");
        }
    }

    /**
     * Saves the current state of the properties object to the config file.
     */
    public static void saveProperties() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE_PATH.toFile())) { // Use try-with-resources
            config.store(writer, "Configuration settings updated by user interface");
        } catch (IOException ioException) {
            ioException.printStackTrace();
            // Use a standard Swing way to notify the user of a critical failure
            JOptionPane.showMessageDialog(null, "Failed to write config file.", "File Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String getLocalDBUrl() {
        final String DB_FILE_NAME = config.getProperty("JDBC_URL_local");

        return "jdbc:sqlite:" + CONFIG_DIR_PATH.resolve(DB_FILE_NAME).toString();
    }

    public static String getAnonKey() {
        return config.getProperty("anon.key");
    }

    public static String getProjectUrl() {
        return config.getProperty("project.url");
    }

    public static String getLabName() {
        return config.getProperty("lab.name");
    }

    public static String getSysNo() {
        return config.getProperty("sys.no");
    }
}

class OptionsManager {

    public static void createConfigurationTableLocal(String JDBC_URL_local) {
        String confTable = ConfigLoader.config.getProperty("CONFIGURATION_TABLE");

        // SQLite uses INTEGER PRIMARY KEY AUTOINCREMENT instead of SERIAL 
        // SQLite uses DATETIME instead of TIMESTAMP WITH TIME ZONE
        // SQLite uses CURRENT_TIMESTAMP instead of NOW()
        String sql = "CREATE TABLE IF NOT EXISTS " + confTable + " (" +
                    " id INTEGER PRIMARY KEY AUTOINCREMENT," + 
                    " category TEXT NOT NULL," + 
                    " item_value TEXT NOT NULL," + 
                    " created_at DATETIME DEFAULT CURRENT_TIMESTAMP," + 
                    " UNIQUE(category, item_value)" + 
                    ");";

        try (Connection conn = DriverManager.getConnection(JDBC_URL_local);
            Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("✓ Local Configuration Table initialized.");
        } catch (SQLException e) {
            System.err.println("\u26A0 CRITICAL: FAILED TO CREATE LOCAL CONFIGURATION TABLE");
            e.printStackTrace();
        }
    }

    public static void createRecordsTableLocal(Connection localConn) {
        String localTable = ConfigLoader.config.getProperty("LOCAL_TABLE");
        try (Statement stmt = localConn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS " + localTable + " (" +
                    "session_id TEXT PRIMARY KEY, " +
                    "login_time TEXT, " +
                    "logout_time TEXT, " +
                    "usn TEXT, " +
                    "name TEXT, " +
                    "details TEXT" + // JSON stored as TEXT in SQLite
                    ");");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}

