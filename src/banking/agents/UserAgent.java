package banking.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;

import banking.gui.*;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

public class UserAgent extends Agent {

    private AID bankAID;
    private UserGUI currentGUI;

    @Override
    protected void setup() {
        System.out.println("UserAgent started: " + getLocalName());

        /* ---------------- DF REGISTER ---------------- */

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("user-service");
        sd.setName(getLocalName());

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (Exception e) {
            e.printStackTrace();
        }

        /* ---------------- FIND BANK ---------------- */

        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    Thread.sleep(500); // allow bank to register
                    findBank();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        /* ---------------- MESSAGE HANDLER ---------------- */

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg == null) {
                    block();
                    return;
                }

                if ("SHUTDOWN".equals(msg.getContent())) {
                    System.out.println("UserAgent shutting down");

                    if (currentGUI != null) {
                        SwingUtilities.invokeLater(() -> currentGUI.dispose());
                    }

                    doDelete();
                    return;
                }

                if (currentGUI != null) {
                    SwingUtilities.invokeLater(() ->
                        currentGUI.processBankResponse(msg)
                    );
                }
            }
        });

        /* ---------------- START UI ---------------- */

        SwingUtilities.invokeLater(() -> openMainMenu());

    }

    /* ==================================================
                         UI ROUTING
       ================================================== */

    public void setGUI(UserGUI gui) {
        this.currentGUI = gui;
    }

    public void openMainMenu() {
        SwingUtilities.invokeLater(() -> {
            currentGUI = null;
            new MainUI(this);
        });
    }

    public void openBankUI() {
        SwingUtilities.invokeLater(() -> {
            BankUI ui = new BankUI(this);
            setGUI(ui);
        });
    }
    
    public void openATM(String atmName) {
        SwingUtilities.invokeLater(() -> {
            ATMUI ui = new ATMUI(atmName, this);
            setGUI(ui);
        });
    }


    public void openATMSelector() {
        SwingUtilities.invokeLater(() -> {
            List<String> atms = getAvailableATMs();
            if (atms.isEmpty()) {
                openMainMenu();
                return;
            }
            // pentru simplitate: primul ATM
            ATMUI ui = new ATMUI(atms.get(0), this);
            setGUI(ui);
        });
    }

    /* ==================================================
                         BANK
       ================================================== */

    private void findBank() {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("bank-service");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                bankAID = result[0].getName();
                System.out.println("UserAgent found Bank: " + bankAID.getLocalName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendToBank(String content) {
        if (bankAID == null) return;

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(bankAID);
        msg.setContent(content);
        send(msg);
    }

    /* ==================================================
                          ATM
       ================================================== */

    public void sendToATM(String atmName, String content) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID(atmName, AID.ISLOCALNAME));
        msg.setContent(content);
        send(msg);
    }

    public List<String> getAvailableATMs() {
        List<String> atms = new ArrayList<>();
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("atm-service");
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription dfd : result) {
                atms.add(dfd.getName().getLocalName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return atms;
    }

    /* ==================================================
                         SHUTDOWN
       ================================================== */

    public void requestShutdown() {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID("system", AID.ISLOCALNAME));
        msg.setContent("SHUTDOWN");
        send(msg);
    }
    
 // În UserAgent.java
    public String fetchAdText(String atmName) {
        // căutăm AID-ul ATM-ului
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("atm-service");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            for (DFAgentDescription dfd : result) {
                if (dfd.getName().getLocalName().equals(atmName)) {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(dfd.getName());
                    msg.setContent("GET_AD");
                    send(msg);

                    ACLMessage reply = blockingReceive();
                    if (reply != null) {
                        return reply.getContent();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fallback
        return "💰 ATM Promotion!\r\n"
        		+ "Make purchases of at least 500 lei and you could win a vacation in Dubai!\r\n"
        		+ "";
    }

}
