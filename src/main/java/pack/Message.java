package pack;

import net.tomp2p.peers.PeerAddress;

import java.io.Serializable;


public class Message implements Serializable {
    public enum MessageType{
        update,         //se il messaggio è di update conterrà un oggetto Auction con le info aggiornate
        bid,            //se il messaggio è un offerta conterrà un oggetto Bid con le info sulla puntata
        victory         //se il messaggio è di vittoria significa che chi lo riceve otterrà un testo con le congratulazioni
    }

    private Auction asta;
    private Bid bid;
    private String text;
    private MessageType type;
    private PeerAddress sender;

    public Message(Auction asta, PeerAddress sender) {
        this.asta = asta;
        this.type = MessageType.update;
        this.sender = sender;
        this.bid = null;
        this.text = null;
    }

    public Message(Bid bid, PeerAddress sender) {
        this.bid = bid;
        this.type = MessageType.bid;
        this.sender = sender;
        this.asta = null;
        this.text = null;
    }

    public Message(String text, PeerAddress sender) {
        this.text = text;
        this.type = MessageType.victory;
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
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public PeerAddress getSender() {
        return sender;
    }

}
