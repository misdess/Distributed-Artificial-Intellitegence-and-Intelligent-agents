package smartmuseum;

import jade.core.Agent;
import jade.core.behaviours.*;

public class TourGuideAgent extends Agent {

    private String[] tour = new String[20];

    protected void setup() {

        addBehaviour(new CreateTour(this, 1000));

    }

    class CreateTour extends WakerBehaviour {

        public CreateTour(Agent a, long time) {
            super(a, time);
        }

        public void handleElapsedTimeout() {
            //retrieve information about artifacts in the museum/gallery
            //analyze user interest
            //both the above operations can be done in parallel- thus parallelBehaviour
            tour[0] = "Monaliza";
            tour[1] = "Arch of the Covenant";
            tour[2] = "The Creation of Adam";
            tour[3] = "Michelangelo";
        }
    }

    class RetrieveArtifacetInfo extends SimpleBehaviour {

        @Override
        public void action() {
            CuratorAgent a = new CuratorAgent();
            CuratorAgent.artifact[] objecst = a.getInfo();
        }

        @Override
        public boolean done() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
    
    class RetrieveProfilerInfo extends SimpleBehaviour {

        @Override
        public void action() {
            CuratorAgent a = new CuratorAgent();
            CuratorAgent.artifact[] objecst = a.getInfo();
        }

        @Override
        public boolean done() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    }
    
    

}
