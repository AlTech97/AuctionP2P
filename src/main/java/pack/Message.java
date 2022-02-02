package pack;

import net.tomp2p.peers.PeerAddress;

import java.io.Serializable;


public class Message implements Serializable {
    public enum MessageType{
        feed,           //se il messaggio è un feed, conterrà un oggetto Auction con le info aggiornate da inviare ai followers
        bid,            //se il messaggio è un offerta conterrà un oggetto Bid con le info sulla puntata
        victory,        //se il messaggio è di vittoria significa che chi lo riceve otterrà un testo con le congratulazioni
        dhtUpdate       //se il messaggio è un dhtUpdate, conterrà un Auction con le informazioni che l'owner dell'asta dovrà aggiornare
    }

    private Auction asta;
    private Bid bid;
    private String text;
    private MessageType msgtype;
    private PeerAddress sender;

    /**
     * Constructor for feed messages, intended to the followers of an auction
     * @param asta an Auction, contains updated information
     * @param sender a PeerAddress, the address of the sender
     * @param type a MessageType, accept "feed" (default) or "dhtUpdate"
     */
    public Message(Auction asta, PeerAddress sender, MessageType type) {
        this.asta = asta;
        if(type.equals(MessageType.dhtUpdate))
            msgtype = type;
        else //default, per evitare l'uso di altri tipi in questo caso
            msgtype = MessageType.feed;
        this.sender = sender;
        this.bid = null;
        this.text = null;
    }

    /**
     * Constructor for bid messages, intended to the owner of the auction
     * @param bid a Bid, contains information about the bidder and the bid amount
     * @param sender a PeerAddress, the address of the sender
     */
    public Message(Bid bid, PeerAddress sender) {
        this.bid = bid;
        this.msgtype = MessageType.bid;
        this.sender = sender;
        this.asta = null;
        this.text = null;
    }

    /**
     * Constructor for victory message, intended to the highest bidder
     * @param text a String, the text of a congratulation message from the owner of auction
     * @param sender a PeerAddress, the address of the sender
     */
    public Message(String text, PeerAddress sender) {
        this.text = text;
        this.msgtype = MessageType.victory;
        this.sender = sender;
        this.bid = null;
        this.asta = null;
    }


    public Auction getAsta() {
        return asta;
    }

    public void setAsta(Auction asta) {
        this.asta = asta;
    }

    public Bid getBid() {
        return bid;
    }

    public void setBid(Bid bid) {
        this.bid = bid;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public MessageType getType() {
        return msgtype;
    }

    public void setType(MessageType type) {
        this.msgtype = type;
    }

    public PeerAddress getSender() {
        return sender;
    }

}
