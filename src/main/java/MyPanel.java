import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;


class MyPanel extends JPanel {
    JLabel l1,l2,l3,l4,l5;

    // added >
    JLabel labLabel; 
    JComboBox<String> labSub; 
    JPanel Subject; 
    JButton syncRefresh; 
    // < added

    JButton jb1,jb2,jb3,jb4;
    JTextField tf1,tf2;
    JPasswordField tf3;
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
        jb4.setBounds(130,300,150,25);
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
    MyPanel(String pname) {
        this.setName(pname);
        this.setLayout(null);
        this.setPreferredSize(new Dimension(420,420));
        tf1 = new JTextField();
        tf1.setBounds(130,120,150,25);
        tf2 = new JTextField();
        tf2.setBounds(130,170,150,25);
        l1 = new JLabel("Name");
        l1.setBounds(130,95,150,30);
        l2 = new JLabel("USN");
        l2.setBounds(130,145,150,30);

        // added >

        Subject = new JPanel(new FlowLayout());
        labLabel = new JLabel("Lab: "); Subject.add(labLabel);
        List<String> subjects = AppBackend.configMap.getOrDefault("Subject", new ArrayList<>());
        labSub = new JComboBox<>(subjects.toArray(new String[0]));
        labSub.setMaximumRowCount(12); // Maximum items displayed before ScrollPane acts 
        labSub.setSelectedIndex(subjects.size()-1); Subject.add(labSub);

        Subject.setBounds(130, 205, 180, 30);
        // < added

        // Refresh button
        syncRefresh = new JButton("\u27F3");
        syncRefresh.setActionCommand("sync_refresh");
        syncRefresh.setBounds(280,270,50,25);
        this.add(syncRefresh);

        l5 = new JLabel("LOGIN");
        l5.setBounds(155,20,150,50);
        l5.setFont(l5.getFont().deriveFont(Font.BOLD));
        l5.setFont(l5.getFont().deriveFont(30f));
        jb1 = new JButton("Submit");
        jb1.setBounds(130,270,100,25);   // 220 -> 270
        jb1.setActionCommand("login_submit");
        jb2 = new JButton("Log In as Admin ?");
        jb2.setBounds(130,330,150,25);   // 300 -> 330
        jb2.setBorderPainted(false);
        jb2.setContentAreaFilled(false);
        jb2.setForeground(Color.blue);
        jb2.setActionCommand("admin_panel");
        this.add(jb1);
        this.add(jb2);
        this.add(tf1);
        this.add(tf2);
        this.add(l1);
        this.add(l2);
        this.add(Subject);
        this.add(l5);
    }
}
