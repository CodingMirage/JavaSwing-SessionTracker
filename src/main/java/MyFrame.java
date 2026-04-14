package org.Miniproject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


class MyFrame extends JFrame implements ActionListener {
    MyPanel p1,p2;
    MyFrame() {
        p1 = new MyPanel("Log-In");
        p1.jb1.addActionListener(this);
        p1.jb2.addActionListener(this);
        p1.jb5.addActionListener(this);
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(p1);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        this.getRootPane().setDefaultButton(p1.jb1);
    }
    @Override
    public void actionPerformed(ActionEvent ae){
        String cmd = ae.getActionCommand();
        if("login_submit".equals(cmd)) {
            String Username = p1.tf1.getText().trim();
            String USN = p1.tf2.getText().toUpperCase();
            String Subcode = (String) p1.cb1.getSelectedItem();
            if(ValueCheck(USN,Username)) {
                MyPanel.createDialog(this,Username,USN,Subcode);  //loads the dialog
            }
            else
                JOptionPane.showMessageDialog(this,"Invalid input");
        }
        else if("Sync".equals(cmd)) {
            ConfigSyncManager.resetSyncFlag();
            ConfigSyncManager.syncOnce();
            p1.refreshSubjects();
        }
        else if("admin_panel".equals(cmd)) {    //changes panel to admin login
            this.getContentPane().remove(p1);
            p2 = new MyPanel();
            p2.jb3.addActionListener(this);
            p2.jb4.addActionListener(this);
            this.getContentPane().add(p2);
            this.revalidate();
            this.repaint();
            this.getRootPane().setDefaultButton(p2.jb3);
        }
        else if("admin_submit".equals(cmd)) {
            String password = new String(p2.tf3.getPassword());
            if (password.equals(ConfigLoader.getProperty("ADMIN_PASSWORD")))  //checks for admin password
                System.exit(0);
            else
                JOptionPane.showMessageDialog(this, "Wrong password");
        }
        else if("back".equals(cmd)) {
            this.getContentPane().remove(p2);
            this.getContentPane().add(p1);
            this.revalidate();
            this.repaint();
            this.getRootPane().setDefaultButton(p1.jb1);
        }
    }
    protected boolean ValueCheck(String usn,String name) {
        if(usn.isEmpty() || name.isEmpty() || usn.length() != 10)
            return false;
        String pattern = "^1[A-Z]{2}\\d{2}[A-Z]{2}\\d{3}$";  //regex only for 10-digit USN (vtu pattern)
        return usn.matches(pattern);

    }
}
