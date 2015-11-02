package homework1;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Date;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProfilerAgent extends Agent {

    private Scanner input = new Scanner(System.in);
    private String name, gender, occupation;
    private String[] interest;
    private int age;
    String[] visitedItems = new String[20];
    private final String[] profiler = {name, "" + age, gender, occupation};
    private Profiler[] profilerUsers = new Profiler[30];

    // The list of known curator agents
    private AID[] curatorAgent = new AID[1];
    ;
    private AID[] tourGuideAgent;

    // Put agent initializations here
    protected void setup() {

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
          //  addBehaviour(new ProfilerRegistration());

            // Add a TickerBehaviour that schedules a request to curator agents every  2 minute
            addBehaviour(new WakerBehaviour(this, 3000) {
                protected void handleElapsedTimeout() {
                    // Update the list of curator agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("item_detail");
                    sd.setName("item_description");

                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result;
                        System.out.println("Found the following curator agent:");
                        if ((result = DFService.search(myAgent, template)) != null) {
                            curatorAgent[0] = result[0].getName();
                            System.out.println(result[0].getName());
                        }

                    } catch (FIPAException fe) {
                        fe.printStackTrace();

                    }
                }
            });

            // Update the list of curator agents
            addBehaviour(new WakerBehaviour(this, 3000) {
                public void handleElapsedTimeout() {
                    DFAgentDescription template1 = new DFAgentDescription();
                    ServiceDescription sd1 = new ServiceDescription();
                    sd1.setType("profiling");
                    sd1.setName("proriler");
                    template1.addServices(sd1);
                    try {
                        System.out.println("Found the following tourGuide agent:");
                        DFAgentDescription[] result1 = DFService.search(myAgent, template1);
                        tourGuideAgent = new AID[1];
                        tourGuideAgent[0] = result1[0].getName();
                        System.out.println(result1[0].getName());

                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                }
            });
            addBehaviour(new profilerAgentComm(this, 3000));
            
        } else {
            // Make the agent terminate
            doDelete();
        }
    }
// Put agent clean-up operations here
    @Override
    protected void takeDown() {
      /*  try { 
            DFService.deregister(this);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }*/
    }

    class ProfilerRegistration extends OneShotBehaviour {

        @Override
        public void action() {
            Profiler p1 = new Profiler();
            profilerUsers[0] = p1;
        }
    }

    class Profiler {

        public Profiler() {
            System.out.print("Register here:\n Name: ");
            name = input.nextLine();
            System.out.print("\nAge: ");
            age = Integer.parseInt(input.nextLine());
            System.out.print("\n Gender: ");
            gender = input.nextLine();
            System.out.print("\n Occupation: ");
            occupation = input.nextLine();
            /* interest[0] = "Laliba";
             interest[1] = "Tajmahal";
             interest[2] = "Axum";*/
        }
    }

    public class profilerAgentComm extends WakerBehaviour {

        public profilerAgentComm(Agent a, long wakeupDate) {
            super(a, wakeupDate);
        }

        @Override
        public void handleElapsedTimeout() {
            String text = null;
            int step = 0;
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);
            ACLMessage msg;
            while (step < 3) {
                switch (step) {
                    case 0:
                        //ask for details of items in the virtual tour from the curator
                        msg = new ACLMessage(ACLMessage.QUERY_IF);
                        msg.addReceiver(curatorAgent[0]);
                        text = "".concat("Lalibela Yeha Axum");
                        msg.setContent(text);
                        
                        msg.setConversationId("item_details");

                        msg.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
                        myAgent.send(msg);
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("item_details"),
                                MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
                        step++;
                        break;
                    case 1:
                        //get details of items from the curator
                      
                        msg = myAgent.receive(mt);
                        if (msg != null && msg.getPerformative() == ACLMessage.CONFIRM) {
                            step++; 
                        } else {
                            block();
                        }
                        break;
                    case 2:
                        //send list of interests to tour guide agent
                        msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.addReceiver(tourGuideAgent[0]);
                        msg.setContent(text);
                        myAgent.send(msg);
                        
                        step++;
                        break;
                }
            }
        }
    }
}
