package banking.gui;

import javax.swing.*;
import java.awt.*;
import banking.agents.UserAgent;
import jade.lang.acl.ACLMessage;

public class BankUI implements UserGUI {

    private JFrame frame;
    private JPanel root;
    private CardLayout cards;

    private JTextField userField, pinField, amountField;
    private JTextArea messages; // JTextArea global pentru toate mesajele

    private final UserAgent agent;

    public BankUI(UserAgent agent) {
        this.agent = agent;
        createUI();
    }

    /* ===================== UI INIT ===================== */
    private void createUI() {
        frame = new JFrame("Central Bank");
        frame.setSize(500, 380);
        frame.setMinimumSize(new Dimension(440, 340));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        // JTextArea global
        messages = new JTextArea(6, 40);
        messages.setEditable(false);
        messages.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane(messages);
        sp.setBorder(BorderFactory.createTitledBorder("Messages"));

        // CardLayout pentru panel-uri
        cards = new CardLayout();
        root = new JPanel(cards);
        root.add(loginPanel(), "login");
        root.add(transactionPanel(), "txn");

        frame.setLayout(new BorderLayout());
        frame.add(root, BorderLayout.CENTER);
        frame.add(sp, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    /* ===================== PANELS ===================== */
    private JPanel loginPanel() {
        JPanel p = basePanel("Central Bank Login");

        userField = new JTextField();
        pinField = new JTextField();
        amountField = new JTextField();

        JButton login = primary("Login");
        JButton create = primary("Create Account");
        JButton back = secondary("Back to Main Menu");

        login.addActionListener(e -> login());
        create.addActionListener(e -> createAccount());
        back.addActionListener(e -> back());

        addRow(p, "Username:", userField, 1);
        addRow(p, "PIN (4 digits):", pinField, 2);
        addRow(p, "Initial amount (optional):", amountField, 3);

        addButtons(p, login, create, 4);
        addButtons(p, back, null, 5);

        return p;
    }

    private JPanel transactionPanel() {
        JPanel p = basePanel("Account Operations");

        amountField = new JTextField();

        JButton deposit = primary("Deposit");
        JButton withdraw = primary("Withdraw");
        JButton logout = secondary("Logout");

        deposit.addActionListener(e -> send("DEPOSIT"));
        withdraw.addActionListener(e -> send("WITHDRAW"));
        logout.addActionListener(e -> logout());

        addRow(p, "Amount:", amountField, 1);
        addButtons(p, deposit, withdraw, 2);
        addButtons(p, logout, null, 3);

        return p;
    }

    /* ===================== ACTIONS ===================== */
    private void login() {
        String u = userField.getText().trim();
        String p = pinField.getText().trim();

        if (u.isEmpty() || !p.matches("\\d{4}")) {
            error("Invalid username or PIN");
            return;
        }
        agent.sendToBank("AUTH;" + u + ";" + p);
    }

    private void createAccount() {
        String u = userField.getText().trim();
        String p = pinField.getText().trim();
        String a = amountField.getText().trim();

        if (u.isEmpty() || !p.matches("\\d{4}")) {
            error("Invalid username or PIN");
            return;
        }
        if (!a.matches("\\d+")) a = "0";

        agent.sendToBank("CREATE;" + u + ";" + p + ";" + a);
        amountField.setText("");
    }

    private void send(String type) {
        String a = amountField.getText().trim();
        if (!a.matches("\\d+")) {
            error("Invalid amount");
            return;
        }
        agent.sendToBank(type + ";" + userField.getText() + ";" + pinField.getText() + ";" + a);
        amountField.setText("");
    }

    private void logout() {
        cards.show(root, "login");
        clearFields();
        info("Logged out");
    }

    private void back() {
        frame.dispose();
        agent.setGUI(null);
        SwingUtilities.invokeLater(() -> new MainUI(agent));
    }

    /* ===================== RESPONSES ===================== */
    @Override
    public void processBankResponse(ACLMessage msg) {
        SwingUtilities.invokeLater(() -> {
            String c = msg.getContent();
            info("Bank: " + c);

            if (c.startsWith("AUTH_OK") || c.startsWith("ACCOUNT_CREATED")) {
                cards.show(root, "txn");
            }

            if (c.contains(";")) {
                String[] p = c.split(";");
                if (p.length == 2) {
                    success("Balance: " + p[1]);
                }
            }
        });
    }

    /* ===================== HELPERS ===================== */
    private JPanel basePanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));

        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.BOLD, 18));
        GridBagConstraints c = gbc(0, 0);
        c.gridwidth = 2;
        p.add(t, c);

        return p;
    }

    private void addRow(JPanel p, String label, JComponent field, int y) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        p.add(new JLabel(label), gbc(0, y));
        p.add(field, gbc(1, y));
    }

    private void addButtons(JPanel p, JButton b1, JButton b2, int y) {
        p.add(b1, gbc(0, y));
        if (b2 != null) p.add(b2, gbc(1, y));
    }

    private GridBagConstraints gbc(int x, int y) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        return c;
    }

    private JButton primary(String t) {
        JButton b = new JButton(t);
        b.setBackground(new Color(46, 125, 50));
        b.setForeground(Color.WHITE);
        return b;
    }

    private JButton secondary(String t) {
        return new JButton(t);
    }

    private void info(String m) {
        messages.append("ℹ " + m + "\n");
        messages.setCaretPosition(messages.getDocument().getLength());
    }

    private void success(String m) {
        messages.append("✔ " + m + "\n");
        messages.setCaretPosition(messages.getDocument().getLength());
    }

    private void error(String m) {
        messages.append("✖ " + m + "\n");
        messages.setCaretPosition(messages.getDocument().getLength());
    }

    private void clearFields() {
        userField.setText("");
        pinField.setText("");
        amountField.setText("");
    }

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
}