package smartmuseum;

import jade.core.Agent;
import jade.core.behaviours.*;
import java.util.Scanner;

public class ProfilerAgent extends Agent {

    private Scanner input = new Scanner(System.in);
    private String name, gender, occupation;
    private String[] interest;
    private int age, countProfiler = 0;
    String[] visitedItems = new String[20];
    private String[] tour = new String[20];
    private final String[] profiler = {name, "" + age, gender, occupation, /*interest*/};
    private Profiler[] profilerUsers = new Profiler[30];

    @Override
    protected void setup() {

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            name = (String) args[0];
            addBehaviour(new ProfilerRegistration());
            System.out.println("User: " + name + " registered");
            doWait(2000);
            addBehaviour(new searchNetwork(this, 60000));
            addBehaviour(new CreateTour(this, 0));
            System.out.println("Created Tour:");
            doWait(2000);
            addBehaviour(new ShowTour(this, 0));
            doWait(2000);
            addBehaviour(new Visit());
        } else {
            System.out.println("Visitor information is incomplete");
            doDelete();
        }
    }

    protected void takeDown() {
        System.out.println("Profiler Agent" + getAID().getName() + "terminating");
    }

    class ProfilerRegistration extends OneShotBehaviour {

        @Override
        public void action() {
            Profiler p1 = new Profiler();
            profilerUsers[countProfiler] = p1;
            countProfiler++;
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
            //System.out.print("\n Interest: ");
           // interest = input.nextLine();
        }
    }

    class CreateTour extends WakerBehaviour {

        public CreateTour(Agent a, long time) {
            super(a, time);
        }

        public void handleElapsedTimeout() {
            tour[0] = "Monaliza";
            tour[1] = "Arch of the Covenant";
            tour[2] = "The Creation of Adam";
            tour[3] = "Michelangelo";
        }
    }

    class ShowTour extends WakerBehaviour {

        public ShowTour(Agent a, long time) {
            super(a, time);
        }

        public void handleElapsedTimeout() {
            int i = 0;
            while (tour[i] != null) {
                System.out.println("    " + tour[i]);
                i++;
            }
        }
    }

    class Visit extends Behaviour {

        int i = 0;

        public void action() {

            while (tour[i] != null) {
                System.out.println(" Visiting: " + tour[i]);
                doWait(4000);
                visitedItems[i] = tour[i];
                i++;
            }
        }

        public boolean done() {
            return tour[1] == null;
        }
    }

    public Profiler[] getInfo() {
        return profilerUsers;

    }
    class searchNetwork extends TickerBehaviour{

        public searchNetwork(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            /// perform operation
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    
    }
}
