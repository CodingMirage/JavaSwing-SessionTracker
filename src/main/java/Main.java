import javax.swing.SwingUtilities;

public class Main{
    public static void main(String [] args){
        new AppBackend().setupDatabase();
        CloudDatabaseUpload.syncLocalDataToRemote();
        SwingUtilities.invokeLater(MyFrame::new);
    }
}