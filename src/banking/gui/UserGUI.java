package banking.gui;

import jade.lang.acl.ACLMessage;

public interface UserGUI {
    void processBankResponse(ACLMessage msg);
    void dispose();
}
