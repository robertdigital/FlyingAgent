package agents;

import behaviours.GetLocationsBehaviour;
import controllers.HomeController;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.domain.mobility.MobilityOntology;
import jade.gui.GuiAgent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.util.leap.List;
import javafx.application.Platform;
import models.Message;

import java.io.IOException;
import java.io.Serializable;


public class MainAgent extends Agent {

    public static final String NAME = "WaiterAgent";
    public static GuiAgent agentInstance;
    private static HomeController homeControllerA;
    private List locationList;
    private OneReceiveBehavior oneReceiveBehavior = new OneReceiveBehavior();
    private AID mobileAgent;
    private CyclicBehaviour myBehaviour = new CyclicBehaviour(this) {

        public void action() {
            Object object = myAgent.getO2AObject();
            if (object instanceof Message) {
                handleO2Object((Message) object);
            } else {
                block();
            }
        }
    };

    public static void setController(HomeController homeController) {
        homeControllerA = homeController;
    }

    private void handleO2Object(Message message) {
        switch (message.getRequestType()) {
            case Message.REFRESH_REQUEST:
                refreshLocation();
                break;
            case Message.MOVE_REQUEST:
                askForMoving((Location) message.getParameters().get(Message.KEY_LOCATION));
                break;
            case Message.ASK_REQUEST:
                askMoreInfo();
                break;
        }
    }

    @Override
    protected void setup() {

        setEnabledO2ACommunication(true, 0);
        getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL0);
        getContentManager().registerOntology(MobilityOntology.getInstance());


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        addBehaviour(new GetLocationsBehaviour(this));
    }

    private void refreshLocation() {
        removeBehaviour(oneReceiveBehavior);
        removeBehaviour(myBehaviour);
        addBehaviour(new GetLocationsBehaviour(this));
    }


    private void askForMoving(Location location) {
        ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
        message.addReceiver(new AID("Service-Agent", AID.ISLOCALNAME));
        addBehaviour(myBehaviour);
        addBehaviour(oneReceiveBehavior);
        try {
            message.setContentObject(location);
        } catch (IOException e) {
            e.printStackTrace();
        }
        send(message);
    }

    public void askMoreInfo() {
        addBehaviour(new AskMoreBehavior());
    }

    public void updateLocations(List items) {
        if (homeControllerA != null) {
            Platform.runLater(() -> homeControllerA.updateLocation(items));
        }
        System.out.println("from receiver " + (items));
        this.locationList = items;
        addBehaviour(myBehaviour);
    }

    public List getLocationList() {
        return locationList;
    }

    public void askForMoving(int n) {
        ACLMessage message = new ACLMessage(ACLMessage.QUERY_IF);
        message.addReceiver(new AID("Service-Agent", AID.ISLOCALNAME));
        addBehaviour(oneReceiveBehavior);
        try {
            message.setContentObject((Serializable) locationList.get(n));
        } catch (IOException e) {
            e.printStackTrace();
        }
        send(message);
    }


    class OneReceiveBehavior extends CyclicBehaviour {


        MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.INFORM);


        @Override
        public void action() {
            ACLMessage message = receive(template);
            if (message != null) {
                System.out.println("From Receiver" + message.getContent());
                mobileAgent = message.getSender();
            } else {
                block();
            }
        }

        private void handleMessage(String jsonResponse) {
            System.out.println(jsonResponse);
        }
    }

    private class AskMoreBehavior extends Behaviour {
        private static final int ASK = 0;
        private static final int RESPONSE = 1;
        private static final int DONE = 2;
        private int status = ASK;
        private MessageTemplate template;

        @Override
        public void action() {
            switch (status) {
                case ASK:
                    ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
                    message.setConversationId(String.valueOf(System.currentTimeMillis()));
                    message.addReceiver(mobileAgent);
                    template = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                            MessageTemplate.MatchConversationId(message.getConversationId()));
                    send(message);
                    status = RESPONSE;
                    break;
                case RESPONSE:
                    ACLMessage receivedMessage = receive(template);
                    if (receivedMessage != null) {
                        handleRawData(receivedMessage.getContent());
                    } else {
                        block();
                    }
                    break;
            }
        }

        private void handleRawData(String rawJson) {
            System.out.println(rawJson);
            status = DONE;
        }

        @Override
        public boolean done() {
            return status == DONE;
        }

    }
}
