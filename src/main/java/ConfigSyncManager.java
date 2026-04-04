package org.Miniproject;

import java.io.File;

public class ConfigSyncManager {

    private static final String APP_FOLDER = System.getenv("APPDATA") + "\\SessionTracker";
    private static final String FLAG_FILE = "config_synced.flag";

    /**
     * Runs the configuration sync only the first time.
     * Returns true if sync was performed, false if skipped.
     */
    public static void syncOnce() {
        try {
            File flag = new File(APP_FOLDER, FLAG_FILE);
            if (flag.exists()) {
                System.out.println("Configuration already synced. Skipping first-time sync.");
                return;
            }

            // Calling existing sync method
            AppBackend.syncConfigurationTable();

            // Creating flag file and checking result
            boolean created = flag.createNewFile();
            if (!created) {
                System.err.println("Warning: Failed to create flag file. First-time sync may run again.");
            } else {
                System.out.println("First-time configuration sync completed. Flag file created.");
            }


        } catch (Exception e) {
            System.err.println("Failed to perform first-time configuration sync.");
            e.printStackTrace();
        }
    }

    // Resets the first-time sync flag
    public static void resetSyncFlag() {
        try {
            File flag = new File(APP_FOLDER, FLAG_FILE);
            if (flag.exists()) {
                boolean deleted = flag.delete();
                if (deleted) {
                    System.out.println("Configuration sync flag reset. Next run will sync again.");
                } else {
                    System.err.println("Failed to delete configuration sync flag. Manual deletion may be required.");
                }
            } else {
                System.out.println("No flag file found. Nothing to reset.");
            }
        } catch (Exception e) {
            System.err.println("Failed to reset configuration sync flag.");
            e.printStackTrace();
        }
    }
}
