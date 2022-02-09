package pack;

import net.tomp2p.peers.PeerAddress;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name, description;
    private Date endTime;
    private double riserva;     //prezzo minimo di vendita
    private Bid offertaAtt;     //prezzo dell'ultima offerta
    private Bid offertaPrec;    //prezzo dell'offerta precedente, pagato dal vincitore
    private Status status;      //enumerazione dello stato dell'asta
    private PeerAddress owner;  //indirizzo di contatto del creatore dell'asta.
    private PeerAddress winner; //indirizzo del vincitore
    private boolean oneTimeClose;

    public Auction(String name, String description, Date endTime, double minPrice, PeerAddress owner) {
        this.name = name;
        this.description = description;
        this.endTime = endTime;
        this.riserva = minPrice;
        this.status = Status.aperta;
        this.owner = owner;
        oneTimeClose = false;
        winner=null;
        offertaAtt = null;
        offertaPrec = null;
    }

    //chiude l'asta se è scaduto il tempo
    public boolean timeClose(){
        //data di oggi
        long milliseconds = System.currentTimeMillis();
        Date data = new Date(milliseconds);
        if(data.after(this.endTime) && !this.oneTimeClose){
            this.status = Status.chiusa;
            this.oneTimeClose=true;
            return true;
        }
        return false;
    }

    public boolean close(PeerAddress address){
        if(address == owner){
            this.status = Status.chiusa;
            return true;
        }
        return false;
    }

    public boolean isOpen(){
        return this.status == Status.aperta;
    }

    public boolean isClosed(){
        return this.status == Status.chiusa;
    }

    //metodi get e set dei campi
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public double getRiserva() {
        return riserva;
    }

    public void setRiserva(double riserva) {
        this.riserva = riserva;
    }

    public Bid getOffertaAtt() {
        return offertaAtt;
    }

    public void setOffertaAtt(Bid offertaAtt) {
        this.offertaAtt = offertaAtt;
    }

    public Bid getOffertaPrec() {
        return offertaPrec;
    }

    public void setOffertaPrec(Bid offertaPrec) {
        this.offertaPrec = offertaPrec;
    }

    public Status getStatus() {
        return status;
    }

    public PeerAddress getOwner() {
        return owner;
    }

    public PeerAddress getWinner() {
        return winner;
    }

    public void setWinner(PeerAddress winner) {
        this.winner = winner;
    }

    @Override
    public String toString() {
        String offerta1 =(offertaAtt==null) ? "nessuna": String.valueOf(offertaAtt.getAmount());
        String offerta2 = (offertaPrec==null) ? "nessuna" : String.valueOf(offertaPrec.getAmount());
        return "Auction{" +
                "nome= '" + name + '\'' +
                ", descrizione= '" + description + '\'' +
                ", Termine= " + endTime +
                ", Prezzo di riserva= " + riserva +
                ", Offerta maggiore= " + offerta1 +
                ", Seconda offerta= " + offerta2 +
                ", stato= " + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Auction asta = (Auction) o;
        return Double.compare(asta.riserva, riserva) == 0 && name.equals(asta.name) &&
                Objects.equals(description, asta.description) && endTime.equals(asta.endTime) &&
                Objects.equals(offertaAtt, asta.offertaAtt) && Objects.equals(offertaPrec, asta.offertaPrec) &&
                status == asta.status && owner.equals(asta.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, endTime, riserva, offertaAtt, offertaPrec, status, owner);
    }
}
