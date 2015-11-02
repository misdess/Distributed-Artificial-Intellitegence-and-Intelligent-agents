package homework1;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.states.MsgReceiver;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class tourGuideAgent extends Agent {

    private AID[] curatorAgent;

    // Put agent initializations here
    protected void setup() {

        // Update the list of curator agents
        addBehaviour(new WakerBehaviour(this, 2000) {
            public void handleElapsedTimeout() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("profiling");
                sd.setName("proriler");
                template.addServices(sd);
                try {
                    DFService.register(myAgent, template);
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                DFAgentDescription template1 = new DFAgentDescription();
                ServiceDescription sd1 = new ServiceDescription();
                sd1.setType("item_detail");
                sd1.setName("item_description");
                template1.addServices(sd1);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template1);
                    curatorAgent = new AID[1];
                    curatorAgent[0] = result[0].getName();

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });

        addBehaviour(new tourGuideAgentComm(this, 5000));

    }

    // Put agent clean-up operations here
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }

    }

    public class tourGuideAgentComm extends WakerBehaviour {

        public tourGuideAgentComm(Agent a, long wakeupDate) {
            super(a, wakeupDate);
        }

        public void handleElapsedTimeout() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg;
            String text = null;
            int step = 0;
            while (step < 3) {
                switch (step) {
                    case 0://wait the profiler for a request
                        msg = myAgent.receive(mt);
                        if (msg != null) {
                            text = msg.getContent();
                            step++;
                        } else {
                            block();
                        }
                        break;

                    case 1://send a request to the curator agent
                        msg = new ACLMessage(ACLMessage.REQUEST);
                        msg.setContent(text);
                        msg.addReceiver(curatorAgent[0]);
                        msg.setConversationId("virtual_tour");

                        myAgent.send(msg);
                        mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);

                        step++;
                        break;
                    case 2://display the virtual tour
                        msg = myAgent.receive(mt);
                        if (msg != null) {
                            virtualTour(msg.getContent());

                            step++;
                        } else {
                            block();
                        }
                        break;
                }
            }
        }

    }

    static public void virtualTour(String str) {
        JFrame frame = new JFrame("Virtual Tour");
        frame.setSize(450, 300);
        JPanel p = new JPanel();
        p.setSize(300, 300);
        JLabel label = new JLabel();
        label.setText(str);
        p.add(label);
        frame.add(p);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        p.setLayout(new GridLayout(2, 2));

        frame.setResizable(false);
        frame.setVisible(true);

    }

}
