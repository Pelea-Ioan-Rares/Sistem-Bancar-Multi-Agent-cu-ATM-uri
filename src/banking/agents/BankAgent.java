package banking.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;

import banking.model.Account;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import banking.util.SecurityUtil;

public class BankAgent extends Agent {

    private Map<String, Account> accounts = new HashMap<>();
    private static final String DATA_FILE = "accounts.dat";

    @Override
    protected void setup() {
        System.out.println("BankAgent started");

        registerInDF();
        loadData();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    handleMessage(msg);
                } else {
                    block();
                }
            }
        });
    }

    /* ---------------- DF ---------------- */

    private void registerInDF() {
        try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());

            ServiceDescription sd = new ServiceDescription();
            sd.setType("bank-service");
            sd.setName("CentralBank");

            dfd.addServices(sd);
            DFService.register(this, dfd);

            System.out.println("BankAgent registered in DF");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ---------------- MESSAGE HANDLING ---------------- */

    private void handleMessage(ACLMessage msg) {
        String content = msg.getContent();
        String[] parts = content.split(";");

        ACLMessage reply = msg.createReply();

        try {
            switch (parts[0]) {

                case "CREATE":
                    reply.setContent(handleCreate(parts));
                    break;

                case "AUTH":
                    reply.setContent(handleAuth(parts));
                    break;

                case "DEPOSIT":
                    reply.setContent(handleDeposit(parts));
                    break;

                case "WITHDRAW":
                    reply.setContent(handleWithdraw(parts));
                    break;
                case "SHUTDOWN":
                    saveData();
                    reply.setContent("BANK_SHUTDOWN_OK");
                    doDelete();
                    return;

                default:
                    reply.setContent("UNKNOWN_OPERATION");
            }
        } catch (Exception e) {
            reply.setContent("ERROR");
            e.printStackTrace();
        }

        send(reply);
    }

    /* ---------------- OPERATIONS ---------------- */

    private String handleCreate(String[] p) {
        String user = p[1];
        String pin = p[2];
        int balance = Integer.parseInt(p[3]);

        if (accounts.containsKey(user)) {
            return "ACCOUNT_EXISTS";
        }

        String pinHash = SecurityUtil.hashPIN(pin);
        accounts.put(user, new Account(user, pinHash, balance));
        saveData();

        return "ACCOUNT_CREATED;" + balance;
    }

    private String handleAuth(String[] p) {
        String user = p[1];
        String pin = p[2];

        Account acc = accounts.get(user);
        if (acc == null) {
            return "AUTH_FAIL";
        }

        String pinHash = SecurityUtil.hashPIN(pin);
        if (!acc.getPinHash().equals(pinHash)) {
            return "AUTH_FAIL";
        }

        return "AUTH_OK;" + acc.getBalance();
    }

    private String handleDeposit(String[] p) {
        String user = p[1];
        String pin = p[2];
        int amount = Integer.parseInt(p[3]);

        Account acc = accounts.get(user);
        if (acc == null || !acc.getPinHash().equals(SecurityUtil.hashPIN(pin))) {
            return "AUTH_FAIL";
        }

        acc.deposit(amount);
        saveData();

        return "DEPOSIT_OK;" + acc.getBalance();
    }

    private String handleWithdraw(String[] p) {
        String user = p[1];
        String pin = p[2];
        int amount = Integer.parseInt(p[3]);

        Account acc = accounts.get(user);
        if (acc == null || !acc.getPinHash().equals(SecurityUtil.hashPIN(pin))) {
            return "AUTH_FAIL";
        }

        if (!acc.withdraw(amount)) {
            return "INSUFFICIENT_FUNDS;" + acc.getBalance();
        }

        saveData();
        return "WITHDRAW_OK;" + acc.getBalance();
    }

    /* ---------------- PERSISTENCE ---------------- */

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(DATA_FILE))) {

            accounts = (Map<String, Account>) ois.readObject();
            System.out.println("Bank: loaded " + accounts.size() + " accounts");

        } catch (Exception e) {
            System.out.println("Bank: no existing data, starting fresh");
            accounts = new HashMap<>();
        }
    }

    private void saveData() {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {

            oos.writeObject(accounts);
            System.out.println("Bank: data saved");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        saveData();
        System.out.println("BankAgent shutting down");
    }
}
