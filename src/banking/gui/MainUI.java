package banking.gui;

import javax.swing.*;
import java.awt.*;
import banking.agents.UserAgent;

public class MainUI {

    private final UserAgent agent;

    public MainUI(UserAgent agent) {
        this.agent = agent;
        createUI();
    }

    private void createUI() {
        JFrame f = new JFrame("Banking System");
        f.setSize(380, 220);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JComboBox<String> combo = new JComboBox<>();
        combo.addItem("Central Bank");
        agent.getAvailableATMs().forEach(combo::addItem);

        JButton connect = new JButton("Connect");
        JButton exit = new JButton("Exit");

        connect.addActionListener(e -> {
            f.dispose();
            String sel = (String) combo.getSelectedItem();
            if ("Central Bank".equals(sel))
                agent.setGUI(new BankUI(agent));
            else
                agent.setGUI(new ATMUI(sel, agent));
        });

        exit.addActionListener(e -> {
            f.dispose();
            agent.requestShutdown();
        });

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10,10,10,10);
        c.gridx = 0; c.gridy = 0;
        p.add(new JLabel("Select service:"), c);
        c.gridy++;
        p.add(combo, c);
        c.gridy++;
        p.add(connect, c);
        c.gridy++;
        p.add(exit, c);

        f.setContentPane(p);
        f.setVisible(true);
    }
}
