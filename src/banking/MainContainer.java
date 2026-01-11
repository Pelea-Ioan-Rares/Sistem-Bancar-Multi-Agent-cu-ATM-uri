package banking;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.*;

public class MainContainer {

    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.GUI, "true");

        AgentContainer container = rt.createMainContainer(p);

        try {
            container.createNewAgent("bank", "banking.agents.BankAgent", null).start();
            container.createNewAgent("atm1", "banking.agents.ATMAgent", null).start();
            container.createNewAgent("atm2", "banking.agents.ATMAgent", null).start();
            container.createNewAgent("user", "banking.agents.UserAgent", null).start();
           // container.createNewAgent("shutdown", "banking.agents.ShutdownAgent", null).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
