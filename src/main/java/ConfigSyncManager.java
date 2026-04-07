package org.Miniproject;

import java.io.File;

import javax.swing.*;
import java.awt.*;

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
}
