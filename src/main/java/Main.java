import javax.swing.SwingUtilities;

public class Main{
    public static void main(String [] args){
        new AppBackend().setupDatabase(); // setups local db
        AppBackend.syncConfigurationTable(); // syncs config table to local from cloud
        CloudDatabaseUpload.syncLocalDataToRemote(); // syncs local table to cloud
        SwingUtilities.invokeLater(MyFrame::new); //invokes the main app (gui)
    }
}
