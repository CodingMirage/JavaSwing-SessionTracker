package org.Miniproject;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

class MyPanel extends JPanel {
    JLabel l1,l2,l3,l4,l5,l6;              //you can change the variable names if you want
    JButton jb1,jb2,jb3,jb4,jb5;           //for me naming variables is a tedious tasks
    JTextField tf1,tf2;                    //hence I avoid it as much as possible :)
    JPasswordField tf3;
    JComboBox<String> cb1;

    // Below panel for admin login page

    MyPanel() {
        this.setLayout(null);
        this.setPreferredSize(new Dimension(420,420));
        l4 = new JLabel("ADMIN LOGIN");
        l4.setBounds(135,20,150,50);
        l4.setFont(l4.getFont().deriveFont(Font.BOLD));
        l4.setFont(l4.getFont().deriveFont(20f));
        l3 = new JLabel("Password");
        l3.setBounds(130,95,150,30);
        tf3 = new JPasswordField(15);
        tf3.setBounds(130,120,150,30);
        jb3 = new JButton("Submit");
        jb3.setBounds(130,170,150,30);
        jb3.setActionCommand("admin_submit");
        jb4 = new JButton("Back to Log in ?");
        jb4.setBounds(130,250,150,25);
        jb4.setBorderPainted(false);
        jb4.setContentAreaFilled(false);
        jb4.setForeground(Color.blue);
        jb4.setActionCommand("back");
        this.add(l3);
        this.add(l4);
        this.add(jb3);
        this.add(tf3);
        this.add(jb4);
    }

    // Below panel for normal login page

    MyPanel(String pname) {
        this.setName(pname);
        this.setLayout(null);
        this.setPreferredSize(new Dimension(420,420));
        tf1 = new JTextField();
        tf1.setBounds(130,120,150,25);
        tf2 = new JTextField();
        tf2.setBounds(130,170,150,25);
        String[] sub_options = AppBackend.getConfigValues("Subject").toArray(new String[0]); //fetches subject from Configuration table
        cb1 = new JComboBox<>(sub_options);
        cb1.setBounds(130,220,150,25);
        l1 = new JLabel("Name");
        l1.setBounds(130,95,150,30);
        l2 = new JLabel("USN");
        l2.setBounds(130,145,150,30);
        l5 = new JLabel("LOGIN");
        l5.setBounds(155,20,150,50);
        l5.setFont(l5.getFont().deriveFont(Font.BOLD));
        l5.setFont(l5.getFont().deriveFont(30f));
        l6 = new JLabel("Subject Code");
        l6.setBounds(130,195,150,30);
        jb1 = new JButton("Submit");
        jb1.setBounds(130,270,150,25);
        jb1.setActionCommand("login_submit");
        jb2 = new JButton("Log In as Admin ?");
        jb2.setBounds(130,350,150,25);
        jb2.setBorderPainted(false);
        jb2.setContentAreaFilled(false);
        jb2.setForeground(Color.blue);
        jb2.setActionCommand("admin_panel");
        ImageIcon icon = new ImageIcon(getClass().getResource("/images/sync_logo.png"));    //location of the icon relative to the project directory
        Image img = icon.getImage().getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        jb5 = new JButton(new ImageIcon(img));
        jb5.setBounds(305, 220, 25, 25);
        jb5.setActionCommand("Sync");
        jb5.setFocusPainted(false);
        this.add(jb1);
        this.add(jb2);
        this.add(jb5);
        this.add(tf1);
        this.add(tf2);
        this.add(cb1);
        this.add(l1);
        this.add(l2);
        this.add(l5);
        this.add(l6);
    }

    //Dialog box for Confirmation

    public static void createDialog(Frame parent,String name,String usn,String subcode) {

        JDialog dialog = new JDialog(parent, "Confirmation!", true);
        dialog.setSize(400,250);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(10,10));

        ArrayList<String> Details = AppBackend.getDetails(usn);   //fetches details for confirmation

        String[] sem_options = AppBackend.getConfigValues("Sem").toArray(new String[0]);
        JComboBox<String> cb1 = new JComboBox<>(sem_options);
        cb1.setSelectedItem(Details.get(2));

        String[] batch_options = AppBackend.getConfigValues("Batch").toArray(new String[0]);
        JComboBox<String> cb2 = new JComboBox<>(batch_options);
        cb2.setSelectedItem(Details.get(4));

        JPanel form = new JPanel(new GridLayout(6,2,10,10));
        form.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));

        form.add(new JLabel("Name:"));
        form.add(new JLabel(name));

        form.add(new JLabel("USN:"));
        form.add(new JLabel(usn));

        form.add(new JLabel("Subject code:"));
        form.add(new JLabel(subcode));

        form.add(new JLabel("Department:"));
        form.add(new JLabel(Details.get(3)));

        form.add(new JLabel("Semester:"));
        form.add(cb1);

        form.add(new JLabel("Batch:"));
        form.add(cb2);

        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        Details.add(0,name);
        Details.add(1,usn);
        Details.add(2,subcode);

        okButton.addActionListener(e -> {
            Details.set(5, (String) cb1.getSelectedItem());
            Details.set(7, (String) cb2.getSelectedItem());
            new AppBackend().insertData(Details);           //inserts in local db
            CloudDatabaseUpload.syncLocalDataToRemote();    //syncs with cloud
            System.exit(0);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getRootPane().setDefaultButton(okButton);
        dialog.setVisible(true);
    }
}


