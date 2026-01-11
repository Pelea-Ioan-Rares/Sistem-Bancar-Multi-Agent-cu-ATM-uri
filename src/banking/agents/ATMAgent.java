package banking.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class ATMAgent extends Agent {

    private AID bankAID;

    protected void setup() {
        System.out.println("ATM started: " + getLocalName());

        // Înregistrare DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("atm-service");
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println("ATM registered in DF: " + getLocalName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Delay pentru a permite Bank să se înregistreze
        addBehaviour(new jade.core.behaviours.OneShotBehaviour() {
            public void action() {
                try { Thread.sleep(500); } catch (InterruptedException e) {}
                findBank();

                if (bankAID != null) {
                    System.out.println("ATM ready to forward messages to Bank");

                    // Ascultă mesaje de la user
                    addBehaviour(new CyclicBehaviour() {
                        public void action() {
                            ACLMessage msg = receive();
                            if (msg != null && msg.getContent() != null) {
                                if ("SHUTDOWN".equals(msg.getContent())) {
                                    System.out.println(getLocalName() + " shutting down");
                                    doDelete();
                                    return;
                                }
                             // În comportamentul CyclicBehaviour al ATM-ului
                                if (msg.getContent() != null && msg.getContent().equals("GET_AD")) {
                                    String adText = fetchAdText(); // metoda de mai sus
                                    ACLMessage reply = msg.createReply();
                                    reply.setContent(adText);
                                    send(reply);
                                }
                                
                                if (!msg.getSender().equals(bankAID)) {
                                    // Mesaj de la User
                                    System.out.println("ATM received from user: " + msg.getContent());
                                    ACLMessage replyFromBank = forwardToBankAndWaitReply(msg);
                                    if (replyFromBank != null) {
                                        forwardToUser(replyFromBank, msg);
                                    }
                                }
                            } else {
                                block();
                            }
                        }
                    });
                }
            }
        });
    }

    private void findBank() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bank-service");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                bankAID = result[0].getName();
                System.out.println("ATM found bank: " + bankAID.getLocalName());
            } else {
                System.out.println("ATM did NOT find bank!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ACLMessage forwardToBankAndWaitReply(ACLMessage userMsg) {
        ACLMessage msgToBank = new ACLMessage(ACLMessage.REQUEST);
        msgToBank.addReceiver(bankAID);
        msgToBank.setContent(userMsg.getContent());
        msgToBank.setSender(getAID());
        send(msgToBank);

        ACLMessage reply = blockingReceive();
        System.out.println("ATM received reply from bank: " + (reply != null ? reply.getContent() : "null"));
        return reply;
    }

    private void forwardToUser(ACLMessage bankMsg, ACLMessage originalUserMsg) {
        if (bankMsg == null || originalUserMsg == null) return;

        ACLMessage reply = new ACLMessage(bankMsg.getPerformative());
        reply.addReceiver(originalUserMsg.getSender());
        reply.setContent(bankMsg.getContent());
        reply.setSender(getAID());
        send(reply);

        System.out.println("ATM forwarded reply to user: " + reply.getContent());
    }

    // Metodă pentru preluarea reclamei de la Python FastAPI
    public String fetchAdText() {
        String fallback = "💰 Promoție ATM!\nFă cumpărături de minim 500 lei și poți câștiga o vacanță în Dubai!";
        try {
            URL url = new URL("http://localhost:8001/ad");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            int status = conn.getResponseCode();
            if (status != 200) return fallback;

            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNext()) sb.append(scanner.nextLine());
            scanner.close();

            String json = sb.toString();
            // Extragem câmpul description
            int descStart = json.indexOf("\"description\":\"");
            if (descStart >= 0) {
                descStart += 15;
                int descEnd = json.indexOf("\"", descStart);
                if (descEnd > descStart) {
                    return json.substring(descStart, descEnd);
                }
            }
        } catch (IOException e) {
            System.out.println("Ad fetch failed, using fallback.");
        }
        return fallback;
    }
}
