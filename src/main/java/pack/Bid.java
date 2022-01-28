package pack;

import net.tomp2p.peers.Number160;

import java.io.Serializable;

public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;
    private Number160 owner;
    private String auctionName;
    private double amount;

    public Bid(Number160 owner, String auctionName, double amount) {
        this.owner = owner;
        this.auctionName = auctionName;
        this.amount = amount;
    }

    public Number160 getOwner() {
        return owner;
    }

    public void setOwner(Number160 owner) {
        this.owner = owner;
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
