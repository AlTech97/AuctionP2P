package pack;

import net.tomp2p.peers.PeerAddress;
import java.io.Serializable;

public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;
    private final PeerAddress owner;          //indirizzo di contatto di chi ha effettuato la puntata
    private String auctionName;         //nome dell'asta su cui si punta
    private double amount;              //valore della puntata

    public Bid(PeerAddress owner, String auctionName, double amount) {
        this.owner = owner;
        this.auctionName = auctionName;
        this.amount = amount;
    }

    public PeerAddress getOwner() {
        return owner;
    }

    public String getAuctionName() {
        return auctionName;
    }

    public void setAuctionName(String auctionName) {
        this.auctionName = auctionName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }
}
