import javax.swing.*;
import java.awt.*;

class MyPanel extends JPanel {
    JLabel l1,l2,l3,l4,l5,l6;
    JButton jb1,jb2,jb3,jb4;
    JTextField tf1,tf2;
    JPasswordField tf3;
    JComboBox<String> cb1;
    // Below mypanel for admin login page
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
    // Below mypanel for normal login page
    MyPanel(String pname) {
        this.setName(pname);
        this.setLayout(null);
        this.setPreferredSize(new Dimension(420,420));
        tf1 = new JTextField();
        tf1.setBounds(130,120,150,25);
        tf2 = new JTextField();
        tf2.setBounds(130,170,150,25);
        String[] options = AppBackend.getDepartmentOrSubjectValues("Subject").toArray(new String[0]);
        cb1 = new JComboBox<>(options);
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
        this.add(jb1);
        this.add(jb2);
        this.add(tf1);
        this.add(tf2);
        this.add(cb1);
        this.add(l1);
        this.add(l2);
        this.add(l5);
        this.add(l6);
    }
}
