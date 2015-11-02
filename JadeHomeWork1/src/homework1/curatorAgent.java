package homework1;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Scanner;
import java.util.*;

public class curatorAgent extends Agent {

    private Scanner input = new Scanner(System.in);

    smartmuseumGui myGui;
    private Hashtable catalogue;

    // Put agent initializations here
    protected void setup() {
        // Create the catalogue
        catalogue = new Hashtable();
        myGui = new smartmuseumGui(this);
        myGui.showGui();

        catalogue.put(200, "Oblisk");
        catalogue.put(300, "TajMahal");
        catalogue.put(400, "Lalibela");

        // Register the artefact registration service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("item_detail");
        sd.setName("item_description");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

      //  addBehaviour(new artifactRegisteration(this, 500));
        addBehaviour(new curatorAgentComm(this, 5000));

    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
            myGui.dispose();
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    class artifactRegisteration extends WakerBehaviour {

        public artifactRegisteration(Agent a, long wakeuptime) {
            super(a, wakeuptime);
        }

        public void handleElapsedTimeout() {
            int price;
            String artifact;
            System.out.println("Enter artefact info \nArtefact name:");
            artifact = input.nextLine();
            System.out.println("Artefact price:");
            price = input.nextInt();
            catalogue.put(artifact, price);
        }
    }

    public void updateCatalogue(final String title, final int price) {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                catalogue.put(title, new Integer(price));
                System.out.println(title + " inserted into catalogue. Price = " + price);
            }
        });
    }

    public class curatorAgentComm extends WakerBehaviour {

        public curatorAgentComm(Agent a, long wakeupDate) {
            super(a, wakeupDate);
        }

        public void handleElapsedTimeout() {
            int step = 0;
            MessageTemplate mt = null;
            ACLMessage msg = null;
            while (step < 2) {
                switch (step) {
                    case 0://item detail request from the profiler
                        mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
                        msg = myAgent.receive(mt);
                        if (msg != null && msg.getPerformative() == ACLMessage.QUERY_IF) {
                            System.out.println("processing the item detail request");
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.CONFIRM);
                            myAgent.send(reply);//replying to the profiler
                            step++;
                            mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                        } else {
                            block();
                        }
                        break;
                    case 1:
                        msg = myAgent.receive(mt);
                        if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.CONFIRM);
                            reply.setContent(msg.getContent());
                            myAgent.send(reply);//replying to the tourGuideAgent
                            step++;
                        } else {
                            block();
                        }

                }
            }
        }
    }
}
