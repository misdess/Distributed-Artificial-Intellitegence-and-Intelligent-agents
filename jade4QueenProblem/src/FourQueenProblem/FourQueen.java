package FourQueenProblem;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class FourQueen extends Agent {

    private int queenNo;
    private AID[] QueenAgents;
    private AID nextQueen;
    private int step;

    private final int N = 5;//number of queens
    private int col = 0;//position of e

    @Override
    protected void setup() {

        // Get the title of the book to buy as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            queenNo = Integer.parseInt((String) args[0]);//Object cast to String and then to Integer

            addBehaviour(new WakerBehaviour(this, 100) {

                protected void handleElapsedTimeout() {
                    //  System.out.println("This is Queen: " + queenNo);
                    //register in the DF
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("queen problem");
                    sd.setName("Queen");
                    template.addServices(sd);
                    try {
                        DFService.register(myAgent, template);
                    } catch (FIPAException ex) {
                        ex.printStackTrace();
                    }

                    //Find all queens registered in the DF
                    DFAgentDescription template1 = new DFAgentDescription();
                    ServiceDescription sd1 = new ServiceDescription();
                    sd1.setType("queen problem");
                    sd1.setName("Queen");
                    template1.addServices(sd1);
                    doWait(500);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template1);
                        // System.out.println("Queen " + queenNo + ": Found the following Queen agents:");
                        QueenAgents = new AID[result.length];
                        if (queenNo == 0 || queenNo == N - 1)//save only predecessor and successor queens
                        {
                            QueenAgents = new AID[1];
                        } else if (queenNo > 0 && queenNo < N - 1)//save only predecessor and successor queens
                        {
                            QueenAgents = new AID[2];
                        }

                        for (int i = 0; i < result.length; ++i) {
                            nextQueen = result[i].getName();
                            int a = Integer.parseInt((result[i].getName().toString().substring(26, 27)));

                            if (queenNo == 0) {//save only predecessor and successor queens
                                if (a == 1) {
                                    QueenAgents[0] = nextQueen;
                                }
                            }
                            if (queenNo == N - 1) {//save only predecessor and successor queens
                                if (a == N - 2) {
                                    QueenAgents[0] = nextQueen;
                                }
                            }

                            if (queenNo > 0 && queenNo < N - 1) {//save only predecessor and successor queens
                                if (a == queenNo - 1) {
                                    QueenAgents[0] = nextQueen;
                                }
                                if (a == queenNo + 1) {
                                    QueenAgents[1] = nextQueen;
                                }
                            }

                        }

                        /* System.out.println("Q"+queenNo+" found the following queen agents");
                         for (int i = 0; i < QueenAgents.length; i++) {
                         System.out.println(QueenAgents[i]);
                         }*/
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    }
                    addBehaviour(new queenImplementation());
                }
            });
        } else {
            // Make the agent terminate
            System.out.println("");
            doDelete();
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Queen " + getAID().getName() + " terminating.");
    }

    class queenImplementation extends Behaviour {

        int[] q = new int[N];//column positions of the queens.
        String str1 = "";
        String receivedStr = "";
        boolean isConsistent = false;

        private MessageTemplate mt;
        private ACLMessage msg;
        private ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);

        @Override
        public void action() {
            if (queenNo == 0) {
                step = 0;
            } else {
                step = 1;
            }

            while (true) {
                switch (step) {
                    case 0:
                        if (col != N) {
                            isConsistent = enumerate(queenNo);
                            msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(QueenAgents[0]);
                           // q[0]=4;
                            msg.setContent("" + q[0]);
                            msg.setSender(myAgent.getAID());
                            myAgent.send(msg);
                            step = 2;
                         //   System.out.println("The queen positions is " + q[0]);
                        } else {
                            block();
                        }
                        break;

                    case 1://message from previous queen...implies 
                        mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                        msg = myAgent.receive(mt);

                        if (msg != null) {
                            receivedStr = msg.getContent();//receive the column postions of the previous queens
                            for (int i = 0; i < receivedStr.length(); i++) {
                                q[i] = Integer.parseInt(receivedStr.substring(i, i + 1));//put the queens in their positions
                            }
                            isConsistent = enumerate(queenNo);

                            if (isConsistent) {//queen is placed in a valid position
                                msg1.setPerformative(ACLMessage.INFORM);

                                msg1.addReceiver(QueenAgents[1]); //add next queen as the receiver
                                for (int i = 0; i < receivedStr.length() + 1; i++)//concatinate previous queen positions to send to the next queen
                                {
                                    str1 = str1.concat("" + q[i]);
                                }
                                msg1.setSender(myAgent.getAID());
                                msg1.setContent(str1);
                                myAgent.send(msg1);//send message to the next agent
                                step = 2;
                            } else {//create a refuse message
                                // if(queenNo==N-1)System.out.println(" last queen cant be here");
                                ACLMessage reply = msg.createReply();
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setSender(myAgent.getAID());
                                myAgent.send(reply);
                                col = 0;
                            }
                            str1 = "";
                        } else {
                            block();
                        }
                        break;
                    case 2:
                        mt = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
                        msg = myAgent.receive(mt);
                        if (msg != null) {
                            if (queenNo == 0) {
                               // System.out.println("The queen positions was " + q[0]);
                                step = 0;
                                break;
                            }
                            if (enumerate(queenNo)) {//queen is placed in a valid position
                                msg1.setPerformative(ACLMessage.INFORM);
                                msg1.addReceiver(QueenAgents[1]); //add next queen as the receiver
                                for (int i = 0; i < receivedStr.length() + 1; i++)//concatinate previous queen positions to send to the next queen
                                {
                                    str1 = str1.concat("" + q[i]);
                                }
                                msg1.setContent(str1);
                                msg1.setSender(myAgent.getAID());
                                myAgent.send(msg1);//send message to the next agent
                                str1 = "";
                            } else {//create a refuse message
                               
                                ACLMessage reply = new ACLMessage(ACLMessage.REFUSE);
                                reply.addReceiver(QueenAgents[0]);//here is my suspecion
                                col = 0;
                                step = 1;
                                reply.setContent(receivedStr);
                                reply.setSender(myAgent.getAID());
                                myAgent.send(reply);
                            }
                        } else {
                            block();
                        }
                        break;
                }
            }

        }

        @Override
        public boolean done() {
            return (queenNo == 0 && col == N + 1);
        }

        public boolean isConsistent(int n) {
            for (int i = 0; i < n; i++) {
                if (q[i] == q[n]) {
                    return false;   // same column
                }
                if ((q[i] - q[n]) == (n - i)) {
                    return false;   // same major diagonal
                }
                if ((q[n] - q[i]) == (n - i)) {
                    return false;   // same minor diagonal
                }
            }
            return true;
        }

        public boolean enumerate(int n) {
           //  System.out.print("The queen positions is: "+n);

            for (int i = col; i < N; i++) {
                q[n] = i;
                if (isConsistent(n)) {
                    col = q[n] + 1;
                    if (n == N - 1) {
                         System.out.print("The queen positions are ");
                        for (int j = 0; j < q.length; j++) {
                              System.out.print(q[j] + " ");
                        }
                        System.out.println();
                        return false;//one success and try another
                    }
                    return true;
                }
            }
            return false;
        }

    }
}
