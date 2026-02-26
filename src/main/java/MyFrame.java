import javax.security.auth.Subject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


class EditStudentDialog extends JDialog {
    private JTextField nameField;
    private JTextField usnField;
    static List<JComboBox<String>> dynamicCombos = new ArrayList<>();

    private boolean confirmed = false;

    public EditStudentDialog(JFrame parent, Map<String, String> stuData) {
        super(parent, "Edit Student Data", true);

        // Create UI components
        nameField = new JTextField(stuData.get("name"));
        usnField = new JTextField(stuData.get("usn"));
        nameField.setEditable(false);
        usnField.setEditable(false);

        JPanel panel = new JPanel(new GridLayout(8, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);

        panel.add(new JLabel("USN:"));
        panel.add(usnField);
        
        for (Map.Entry<String, List<String>> entry : AppBackend.configMap.entrySet()) {
            String categoryName = entry.getKey();
            List<String> options = entry.getValue();
            
            JComboBox<String> combo = new JComboBox<>(options.toArray(new String[0]));
            combo.setName(categoryName); 
            
            combo.setSelectedItem(stuData.get(categoryName));

            if (categoryName.equals("LabName")) {
                combo.setSelectedItem(ConfigLoader.getLabName());
                combo.setEnabled(false);
            } else if (categoryName.equals("SysNo")) {
                combo.setSelectedItem(ConfigLoader.getSysNo());
                combo.setEnabled(false);
            }
            // Add component to panel
            panel.add(new JLabel(categoryName)); 
            panel.add(combo);

            dynamicCombos.add(combo);
        }

        

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
            for (JComboBox<String> combo : dynamicCombos) {
				String category = combo.getName();
				String selected = combo.getSelectedItem().toString().trim();
                json.add("\""+ category +"\":\"" + selected+ "\"");
			}

            return new String[]{
                nameField.getText().toUpperCase(),
                usnField.getText().toUpperCase(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), //toString()
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
        p1.syncRefresh.addActionListener(this);
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
        if ("sync_refresh".equals(cmd)) {
            System.out.println("Syncing Configuration");
            HelperFunctions.performSyncWithProgress(
                getOwner(), 
                ()->CloudDatabaseUpload.fetchConfigurationFromCloud(), 
                ()-> {
                    List<String> subjects = AppBackend.configMap.getOrDefault("Subject", new ArrayList<>());
                    
                    // Update the EXISTING JComboBox
                    p1.labSub.setModel(new DefaultComboBoxModel<>(subjects.toArray(new String[0])));
                    
                    if (!subjects.isEmpty()) {
                        p1.labSub.setSelectedIndex(subjects.size() - 1);
                    }
                    
                    p1.revalidate();
                    p1.repaint();
                    System.out.println("Configuration Values Refreshed");
                }
            ); 
        }
        if("login_submit".equals(cmd)) {
            String Username = p1.tf1.getText().trim();
            String USN = p1.tf2.getText();
            String sub = p1.labSub.getSelectedItem().toString();
            if(ValueCheck(USN,Username)) {
                AppBackend ab1 = new AppBackend();
                Map<String,String> stuData = ab1.getData(Username, USN.toUpperCase(), sub);

                EditStudentDialog verificationDialog =  new EditStudentDialog(this, stuData);
                verificationDialog.setVisible(true);
                if (!verificationDialog.isConfirmed()) return;
                
                String[] updatedStudentData = verificationDialog.getEditedData();
                new AppBackend().insertData(updatedStudentData);

                this.setVisible(false); // hide the UI so it doesn't seem freezed 
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
