package dutchauction;

import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.ProfileImpl;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.JADEAgentManagement.QueryPlatformLocationsAction;
import jade.domain.mobility.CloneAction;
import jade.domain.mobility.MobileAgentDescription;
import jade.domain.mobility.MobilityOntology;
import jade.domain.mobility.MoveAction;
import jade.gui.GuiAgent;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;

public class CuratorAgent extends Agent {

    private Location destination;
    private jade.wrapper.AgentContainer home;
    jade.core.Runtime runtime = jade.core.Runtime.instance();
    private jade.wrapper.AgentContainer[] container = null;
    private Map locations = new HashMap();

    private Hashtable catalogue;
    private BidderGui myGui;

    private Vector agents = new Vector();

    // Put agent initializations here
    protected void setup() {
addBehaviour(new mobility());
        catalogue = new Hashtable();
        catalogue.put("monaliza", 80);
        // Create and show the GUI 
        myGui = new BidderGui(this);
        myGui.showGui();
        // Register the artifact-buying service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("artefact-auction");
        sd.setName("monaliza");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new OfferRequestsServer());
        // Add the behaviour serving purchase orders from saler agents
        addBehaviour(new PurchaseOrdersServer());
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        myGui.dispose();
    }

    public void updateCatalogue(final String artefact, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(artefact, price);
                // JOptionPane.showMessageDialog(null, artefact + " inserted into catalogue. With Price = " + price);
            }
        });
    }

    private class OfferRequestsServer extends CyclicBehaviour {

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                StringTokenizer sToken = new StringTokenizer(msg.getContent());
                String name = sToken.nextToken();
                int price = Integer.parseInt(sToken.nextToken());

                if ((msg.getPerformative() == ACLMessage.CFP)) {
                    if (price < (Integer) catalogue.get(name)) {

                        ACLMessage reply = msg.createReply();

                        reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                        //reply.setContent(String.valueOf(price.intValue()));

                        myAgent.send(reply);
                    }
                } else {
                    block();
                }
            }
        }
    }  // End of inner class OfferRequestsServer

    /**
     * Inner class PurchaseOrdersServer. This is the behaviour used by
     * CuratorAgent agents to serve incoming offer acceptances (i.e. purchase
     * orders) from seller agents. The CuratorAgent agent removes the purchased
     * artifact from its catalogue and replies with an INFORM message to notify
     * the seller that the purchase has been successfully completed.
     */
    private class PurchaseOrdersServer extends CyclicBehaviour {

        public void action() {

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                if (msg.getContent().equals("Winner") == true) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Purchase Order");
                    myAgent.send(reply);
                }
            } else {
                block();
            }
        }
    }  // End of inner class OfferRequestsServer

    void sendRequest(Action action) {
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.setLanguage(new SLCodec().getName());
        request.setOntology(MobilityOntology.getInstance().getName());
        try {
            getContentManager().fillContent(request, action);
            request.addReceiver(action.getActor());
            send(request);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void containerStuffs() {
        // Register language and ontology
        getContentManager().registerLanguage(new SLCodec());
        getContentManager().registerOntology(MobilityOntology.getInstance());
        try {
            // Create the container objects
            home = runtime.createAgentContainer(new ProfileImpl());
            container = new jade.wrapper.AgentContainer[1];
            for (int i = 0; i < 2; i++) {
                container[0] = runtime.createAgentContainer(new ProfileImpl());
            }
            doWait(1000);

            // Get available locations with AMS
            sendRequest(new Action(getAMS(), new QueryPlatformLocationsAction()));
            //Receive response from AMS
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchSender(getAMS()),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage resp = blockingReceive(mt);
            ContentElement ce = getContentManager().extractContent(resp);
            Result result = (Result) ce;
            //get the containers
            jade.util.leap.Iterator it = result.getItems().iterator();
            while (it.hasNext()) {
                Location loc = (Location) it.next();
                locations.put(loc.getName(), loc);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

class mobility extends OneShotBehaviour{

        @Override
        public void action() {
        destination = here();
        containerStuffs();//creating and getting created containers
        Object[] containers = locations.keySet().toArray(new String[1]);
        String agentName = "Cloned" + myAgent.getName();

        for (int i = 0; i < containers.length; i++) {
            if (!containers[i].equals("Main-Container")) {
                System.out.println(containers[i]);
            }
        }
/*
        myAgent.doMove((Location) locations.get(containers[0]));

        myAgent.doClone(destination, agentName);
        System.out.println("this agent is cloned in container " + destination + " and it is" + agentName);

        myAgent.doMove((Location) locations.get(containers[1]));

*/
        jade.wrapper.AgentController a = null;
        try {
            Object[] args = new Object[2];
            args[0] = getAID();
            String name = "Agent0";
            a = home.createNewAgent(name, MobileAgent.class.getName(), args);
            a.start();
            agents.add(name);
            //   myGui.updateList(agents);
        } catch (Exception ex) {
            System.out.println("Problem creating new agent");
        }

         AID aid = new AID(agentName, AID.ISLOCALNAME);

         Location dest = (Location)locations.get((Location) locations.get(containers[1]));
         MobileAgentDescription mad = new MobileAgentDescription();
         mad.setName(aid);
         mad.setDestination(dest);
         MoveAction ma = new MoveAction();
         ma.setMobileAgentDescription(mad);
         sendRequest(new Action(aid, ma));
	  
}

}

}
