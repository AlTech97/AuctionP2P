package pack;

public class MessageListenerImpl implements MessageListener {
    final int peerid;
    public MessageListenerImpl(int peerid)
    {
        this.peerid=peerid;
    }

    @Override
    public Object parseMessage(Object obj) {
        Message m = (Message) obj;
        if(m.getType().equals(Message.MessageType.update)){
            System.out.println("ID: " +peerid+") (Aggiornamento) "+m.getAsta().toString());
        }
        else if (m.getType().equals(Message.MessageType.victory)){
            System.out.println("ID: " +peerid+") (Vittoria) "+ m.getText());
        }
        else { //messaggio di bid ricevuto da un peer che vuole fare un'offerta sulla mia asta.


        }

        return "success";
    }
}
