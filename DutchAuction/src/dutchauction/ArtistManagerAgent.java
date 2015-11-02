package dutchauction;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.content.onto.basic.Result;
import jade.core.Agent;
import jade.core.AID;
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
import jade.domain.mobility.MobilityOntology;
import jade.wrapper.StaleProxyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class ArtistManagerAgent extends Agent {

    private String targetBookTitle;// The title of the book to buy
    private int itemPrice = 700;  // The price of the item in auction  
    private AID[] sellerAgents; // The list of known seller agents
    private Location destination;
    private Vector agents = new Vector();

    private jade.wrapper.AgentContainer home;
    jade.core.Runtime runtime = jade.core.Runtime.instance();
    private jade.wrapper.AgentContainer[] container = null;
    private Map locations = new HashMap();

    protected void setup() {// Put agent initializations here
        destination = here();
        System.out.println("Hallo! Buyer-agent " + getAID().getName() + " is ready.");
        Object[] args = getArguments();// Get the title of the book to buy as a start-up argument

        if (args != null && args.length > 0) {
            targetBookTitle = (String) args[0];
            System.out.println("Trying to sell " + targetBookTitle);

            // Add a TickerBehaviour that schedules a request to seller agents every minute
            addBehaviour(new WakerBehaviour(this, 10000) {

                protected void handleElapsedTimeout() {

                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("artefact-auction");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following auction participants:");
                        sellerAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println(sellerAgents[i].getName());
                        }
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
            });
        } else {
            // Make the agent terminate
            System.out.println("No target book title specified");
            doDelete();
        }
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Buyer-agent " + getAID().getName() + " terminating.");
    }

    /**
     * Inner class RequestPerformer. This is the behaviour used by Book-buyer
     * agents to request seller agents the target book.
     */
    private class RequestPerformer extends Behaviour {

        private AID bestSeller; // The agent who bids the auction 

        private MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP); // The template to receive replies
        private int step = 0;

        private long startTime;//the time when the auctioner starts listening to bidders
        private int decrementValue = 100;//the predefined value by which the price of the item is to be decremeted if no bidders
        private int minimumItemPrice = 200;//the minimum acceptable price of the item
        private int numberOfBidders = 0;//the number of bidders willing to pay the current price of the item
        private ACLMessage reply;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < sellerAgents.length; i++) {
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(targetBookTitle + " " + itemPrice);
                    System.out.println(cfp.getContent());
                    cfp.setConversationId("item-auction");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("item-auction"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    startTime = System.currentTimeMillis();
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            if (numberOfBidders == 0)//First come first serve
                            {
                                bestSeller = reply.getSender();
                            }
                            numberOfBidders++;
                        }
                    } else {
                        if (startTime - System.currentTimeMillis() < 15000) {
                            block(15000 /*- (startTime - System.currentTimeMillis())*/);
                        } else {
                            doWake();
                        }
                    }
                    //if no proposal found;
                    if (numberOfBidders == 0) {
                        itemPrice -= decrementValue;
                        if (itemPrice >= minimumItemPrice) {//decrement item price and call for auction again
                            step = 0;
                        } else {
                            step = 4;//auction failure
                        }
                    } else {
                        step = 2;
                    }
                    //JOptionPane.showMessageDialog(null, "current item price is " + itemPrice);
                    break;
                case 2:
                    //send message to the winner
                    ACLMessage order = new ACLMessage(ACLMessage.INFORM);
                    order.addReceiver(bestSeller);
                    order.setContent("Winner");
                    //  order.setConversationId("Winner");
                    //  order.setReplyWith("ok" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order 
                    mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    step = 3;
                    break;
                case 3://get the purchase order message
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                        step = 5;
                        System.out.println(targetBookTitle + " sold to agent " + msg.getSender().getName() + " for " + itemPrice);

                    } else {
                        block();
                    }
                    break;
            }

        }

        public boolean done() {
            if (step == 4) {
                System.out.println("Auction failed: " + targetBookTitle + " cannot be sold below " + minimumItemPrice);
                return step == 4;
            }
            if (step == 5) {
                System.out.println("Auction finished successfully ");
            }
            return step == 5;
        }
    }  // End of inner class RequestPerformer

    void sendRequest(Action action) {
// ---------------------------------

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
            for (int i = 0; i < 0; i++) {
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

    class mobility extends OneShotBehaviour {

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

            //myAgent.doMove((Location) locations.get(containers[0]));

           myAgent.doClone((Location) locations.get(containers[0]), agentName);
            System.out.println("this agent is cloned in container " + destination + " and it is" + agentName);

           // myAgent.doMove((Location) locations.get(containers[1]));

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

            try {
                a.move((Location) locations.get(containers[0]));
                a.start();
            } catch (StaleProxyException ex) {
                System.out.println("Problem moving new agent");
            }
        }

    }

}
