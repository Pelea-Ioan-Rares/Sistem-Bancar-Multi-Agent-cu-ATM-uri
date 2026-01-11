package banking.agents;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;

public class SystemManagerAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println("SystemManagerAgent started");
    }

    public void shutdownSystem() {
        System.out.println("SystemManager: initiating shutdown");

        sendShutdownToService("bank-service");
        sendShutdownToService("atm-service");
        sendShutdownToService("user-service");

        doDelete();
    }

    private void sendShutdownToService(String serviceType) {
        try {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(serviceType);
            template.addServices(sd);

            DFAgentDescription[] result = DFService.search(this, template);

            for (DFAgentDescription dfd : result) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("SHUTDOWN");
                msg.addReceiver(dfd.getName());
                send(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
