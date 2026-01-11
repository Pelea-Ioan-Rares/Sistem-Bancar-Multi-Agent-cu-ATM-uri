package banking.gui;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import banking.agents.UserAgent;
import jade.lang.acl.ACLMessage;

public class ATMUI implements UserGUI {

    private JFrame frame;
    private JPanel root;
    private CardLayout cards;

    private JTextField userField, pinField, amountField;
    private JTextArea messages;

    private final String atmName;
    private final UserAgent agent;

    public ATMUI(String atmName, UserAgent agent) {
        this.atmName = atmName;
        this.agent = agent;
        createUI();
        showAdAtStart();
    }

    /* ===================== UI INIT ===================== */

    private void createUI() {
        frame = new JFrame("ATM - " + atmName);
        frame.setSize(500, 420);
        frame.setMinimumSize(new Dimension(450, 380));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        cards = new CardLayout();
        root = new JPanel(cards);

        root.add(loginPanel(), "login");
        root.add(transactionPanel(), "txn");

        frame.add(root, BorderLayout.CENTER);
        frame.add(messageArea(), BorderLayout.SOUTH);

        cards.show(root, "login");
        frame.setVisible(true);
    }

    private JScrollPane messageArea() {
        messages = new JTextArea(6, 40);
        messages.setEditable(false);
        messages.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);

        JScrollPane sp = new JScrollPane(messages);
        sp.setBorder(BorderFactory.createTitledBorder("Messages"));
        return sp;
    }

    /* ===================== AD DIALOG ===================== */

    private void showAdAtStart() {
        SwingUtilities.invokeLater(() -> {
            String adText = fetchAdText();

            JDialog dialog = new JDialog(frame, "Promoție ATM", true);
            dialog.setLayout(new BorderLayout(10, 10));

            JTextArea adArea = new JTextArea(adText);
            adArea.setEditable(false);
            adArea.setLineWrap(true);
            adArea.setWrapStyleWord(true);
            adArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
            adArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton close = new JButton("Închide");
            close.addActionListener(e -> dialog.dispose());

            dialog.add(new JScrollPane(adArea), BorderLayout.CENTER);
            dialog.add(close, BorderLayout.SOUTH);

            dialog.setSize(420, 220);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }

    private String fetchAdText() {
        String fallback =
                "💰 Promoție ATM!\n\n" +
                "Fă cumpărături de minim 500 lei și poți câștiga o vacanță în Dubai!";

        try {
            URL url = new URL("http://localhost:8001/ad");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() != 200) return fallback;

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            String json = sb.toString();
            int start = json.indexOf("\"description\":\"");
            if (start == -1) return fallback;
            start += 15;
            int end = json.indexOf("\"", start);
            if (end == -1) return fallback;

            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");

        } catch (Exception e) {
            return fallback;
        }
    }

    /* ===================== PANELS ===================== */

    private JPanel loginPanel() {
        JPanel p = basePanel("ATM Login");

        userField = new JTextField();
        pinField = new JTextField();

        JButton login = primary("Login");
        JButton back = secondary("Back");

        login.addActionListener(e -> login());
        back.addActionListener(e -> back());

        addRow(p, "Username:", userField, 0);
        addRow(p, "PIN (4 digits):", pinField, 1);
        addButtons(p, login, back, 2);

        return p;
    }

    private JPanel transactionPanel() {
        JPanel p = basePanel("Transactions");

        amountField = new JTextField();

        JButton deposit = primary("Deposit");
        JButton withdraw = primary("Withdraw");
        JButton logout = secondary("Logout");

        deposit.addActionListener(e -> send("DEPOSIT"));
        withdraw.addActionListener(e -> send("WITHDRAW"));
        logout.addActionListener(e -> logout());

        addRow(p, "Amount:", amountField, 0);
        addButtons(p, deposit, withdraw, 1);
        addButtons(p, logout, null, 2);

        return p;
    }

    /* ===================== ACTIONS ===================== */

    private void login() {
        String u = userField.getText().trim();
        String p = pinField.getText().trim();

        if (u.isEmpty() || !p.matches("\\d{4}")) {
            error("Date de autentificare invalide");
            return;
        }

        agent.sendToATM(atmName, "AUTH;" + u + ";" + p);
    }

    private void send(String type) {
        String amt = amountField.getText().trim();

        if (!amt.matches("\\d+")) {
            error("Sumă invalidă");
            return;
        }

        agent.sendToATM(atmName,
                type + ";" + userField.getText() + ";" +
                        pinField.getText() + ";" + amt);

        amountField.setText("");
    }

    private void logout() {
        cards.show(root, "login");
        clearFields();
        info("Delogat cu succes");
    }

    private void back() {
        clearFields();
        agent.setGUI(null);
        new MainUI(agent);
        frame.dispose();
    }

    /* ===================== RESPONSES ===================== */

    @Override
    public void processBankResponse(ACLMessage msg) {
        SwingUtilities.invokeLater(() -> {
            String c = msg.getContent();
            info("ATM → " + c);

            if (c.startsWith("AUTH_OK")) {
                cards.show(root, "txn");
            } else if (c.contains(";")) {
                success("Sold curent: " + c.split(";")[1]);
            }
        });
    }

    /* ===================== HELPERS ===================== */

    private JPanel basePanel(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
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
        b.setBackground(new Color(33, 150, 243));
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
        frame.dispose();
    }
}
