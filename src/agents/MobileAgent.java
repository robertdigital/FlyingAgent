package agents;

import information.AllInformation;
import information.RawInformation;
import jade.core.AID;
import jade.core.Agent;
import jade.core.Location;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

public class MobileAgent extends Agent {

    private AID stableAgent = new AID(MainAgent.NAME, AID.ISLOCALNAME);

    @Override
    protected void setup() {

        addBehaviour(new ServeMovingMessages());
    }

    @Override
    protected void takeDown() {
        System.out.println("Agent done.");
    }

    @Override
    protected void afterMove() {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        System.out.println(AllInformation.getInstance().toJson());
        message.setContent(AllInformation.getInstance().toJson());
        message.addReceiver(stableAgent);
        send(message);
    }


    public class ServeMovingMessages extends CyclicBehaviour {

        private MessageTemplate template = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        @Override
        public void action() {
            ACLMessage message = receive(template);
            if (message != null) {
                switch (message.getPerformative()) {
                    case ACLMessage.QUERY_IF:
                        try {
                            Location location = (Location) message.getContentObject();
                            System.out.println(location.toString());
                            doMove(location);
                        } catch (UnreadableException e) {
                            e.printStackTrace();
                        }
                        break;
                    case ACLMessage.REQUEST:
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        String moreInfo = new RawInformation().toString();
                        System.out.println(moreInfo);
                        msg.setContent(moreInfo);
                        msg.addReceiver(stableAgent);
                        send(msg);
                        break;
                }

            } else block();
        }
    }
}
