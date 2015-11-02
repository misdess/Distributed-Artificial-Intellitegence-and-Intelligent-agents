package smartmuseum;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import java.util.Scanner;

public class CuratorAgent extends Agent {

    private int numberOfArtifacts = 0;
    private String id, name, creator, dateOfCreation, placeOfCreation, genre;
    private Scanner input = new Scanner(System.in);
    private artifact[] objects = new artifact[20];

    private String[] artifact = {id, name, creator, dateOfCreation, placeOfCreation, genre};

    protected void setup() {
        addBehaviour(new artifactRegistration());
    }

    class artifactRegistration extends OneShotBehaviour {

        @Override
        public void action() {
            artifact obj1 = new artifact();
            objects[numberOfArtifacts] = obj1;
            numberOfArtifacts++;

        }
    }

    class artifact {

        public artifact() {
            System.out.print("Register artifact here:\n Id: ");
            id = input.nextLine();
            System.out.print("\nName: ");
            name = input.nextLine();
            System.out.print("\nCreator: ");
            creator = input.nextLine();
            System.out.print("\nDate of Creation: ");
            dateOfCreation = input.nextLine();
            System.out.print("\nPlace of Creation: ");
            placeOfCreation = input.nextLine();
            System.out.print("\nGenre: ");
            genre = input.nextLine();
        }

    }
    public artifact [] getInfo(){
    return objects;
    
    }
}
