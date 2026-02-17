import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;


class EditStudentDialog extends JDialog {
    private JTextField nameField;
    private JTextField usnField;
    private JComboBox<String> semComboBox;
    private JComboBox<String> deptComboBox;
    private JComboBox<String> labSubComboBox;
    private JComboBox<String> batchComboBox;

    private boolean confirmed = false;

    public EditStudentDialog(JFrame parent, String[] stuData) {
        super(parent, "Edit Student Data", true); // `true` makes it modal

        // Create UI components
        nameField = new JTextField(stuData[0]);
        usnField = new JTextField(stuData[1]);
        
        String[] sem = AppBackend.configMap.getOrDefault("Sem", new ArrayList<>()).toArray(new String[0]);
        semComboBox = new JComboBox<>(sem);
        semComboBox.setSelectedItem(stuData[2]);
    
        String[] departments = AppBackend.configMap.getOrDefault("Department", new ArrayList<>()).toArray(new String[0]);
        deptComboBox = new JComboBox<>(departments);
        deptComboBox.setSelectedItem(stuData[3]);

        String[] labSubjects = AppBackend.configMap.getOrDefault("Subject", new ArrayList<>()).toArray(new String[0]);
        labSubComboBox = new JComboBox<>(labSubjects);
        labSubComboBox.setSelectedItem(stuData[4]);

        String[] batches = AppBackend.configMap.getOrDefault("Batch", new ArrayList<>()).toArray(new String[0]);
        batchComboBox = new JComboBox<>(batches);
        batchComboBox.setSelectedItem(stuData[5]);
        
        // Add components to a panel
        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("USN:"));
        panel.add(usnField);
        panel.add(new JLabel("Lab"));
        panel.add(labSubComboBox);
        panel.add(new JLabel("Semester:"));
        panel.add(semComboBox);
        panel.add(new JLabel("Batch:"));
        panel.add(batchComboBox);
        panel.add(new JLabel("Department:"));
        panel.add(deptComboBox);

        // Add action buttons
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = true;
                dispose(); // Close the dialog
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose(); // Close the dialog
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // Add panels to the dialog
        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack(); // Resize the dialog to fit the components
        setLocationRelativeTo(parent); // Center on the parent frame
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String[] getEditedData() {
        if (confirmed) {
            StringJoiner json = new StringJoiner(",", "{", "}");
            json.add("\"Subject\":\"" + (String) labSubComboBox.getSelectedItem()+ "\"");
            json.add("\"Department\":\"" + (String) deptComboBox.getSelectedItem()+ "\"");
            json.add("\"Batch\":\"" + (String) batchComboBox.getSelectedItem() + "\"");
            json.add("\"Semester\":\"" + (String) semComboBox.getSelectedItem() + "\"");
            return new String[]{
                nameField.getText().toUpperCase(),
                usnField.getText().toUpperCase(),
                LocalDateTime.now().toString(), //.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                json.toString(),
                null,
                UUID.randomUUID().toString()  
            };
        }
        return null; // Return null if the user canceled
    }
}


class MyFrame extends JFrame implements ActionListener {
    MyPanel p1,p2;
    MyFrame() {
        p1 = new MyPanel("Log-In");
        p1.jb1.addActionListener(this);
        p1.jb2.addActionListener(this);
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(p1);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }
    @Override
    public void actionPerformed(ActionEvent ae){
        String cmd = ae.getActionCommand();
        if("login_submit".equals(cmd)) {
            String Username = p1.tf1.getText().trim();
            String USN = p1.tf2.getText();
            String sub = p1.labSub.getSelectedItem().toString();
            if(ValueCheck(USN,Username)) {
                AppBackend ab1 = new AppBackend();
                String[] stuData = ab1.getData(Username, USN.toUpperCase(), sub);

                EditStudentDialog verificationDialog =  new EditStudentDialog(this, stuData);
                verificationDialog.setVisible(true);
                if (!verificationDialog.isConfirmed()) return;
                
                String[] updatedStudentData = verificationDialog.getEditedData();

                new AppBackend().insertData(updatedStudentData);
                CloudDatabaseUpload.syncLocalDataToRemote();
                System.exit(0);
            }
            else
                JOptionPane.showMessageDialog(this,"Invalid input");
        }
        else if("admin_panel".equals(cmd)) {
            this.getContentPane().remove(p1);
            p2 = new MyPanel();
            p2.jb3.addActionListener(this);
            p2.jb4.addActionListener(this);
            this.getContentPane().add(p2);
            this.revalidate();
            this.repaint();
        }
        else if("admin_submit".equals(cmd)) {
            String password = new String(p2.tf3.getPassword());
            if (password.equals("admin"))
                System.exit(0);
            else
                JOptionPane.showMessageDialog(this, "Wrong password");
        }
        else if("back".equals(cmd)) {
            this.getContentPane().remove(p2);
            this.getContentPane().add(p1);
            this.revalidate();
            this.repaint();
        }
    }
    protected boolean ValueCheck(String usn,String name) {
        if(usn.isEmpty() || name.isEmpty() || usn.length() != 10)
            return false;
        String pattern = "^1VI\\d{2}[A-Z]{2}\\d{3}$";
        return usn.toUpperCase().matches(pattern);

    }
}
