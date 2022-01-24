package pack;

import net.tomp2p.peers.Number160;

import java.util.Date;

public class Asta {
    String name, description;
    Date endTime;
    double riserva;     //prezzo minimo di vendita
    double offertaAtt;  //prezzo dell'ultima offerta
    double offertaPrec; //prezzo dell'offerta precedente, pagato dal vincitore
    Status status;      //enumerazione dello stato dell'asta
    Number160 owner;    //id del creatore dell'asta.


    public Asta(String name, String description, Date endTime, double minPrice, Number160 owner) {
        this.name = name;
        this.description = description;
        this.endTime = endTime;
        this.riserva = minPrice;
        this.offertaAtt = 0.0;
        this.offertaPrec = 0.0;
        this.status = Status.aperta;
        this.owner = owner;
    }

    public boolean close(Number160 id){
        if(id == owner){
            status = Status.chiusa;
            return true;
        }
        return false;
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

    public double getOffertaAtt() {
        return offertaAtt;
    }

    public void setOffertaAtt(double offertaAtt) {
        this.offertaAtt = offertaAtt;
    }

    public double getOffertaPrec() {
        return offertaPrec;
    }

    public void setOffertaPrec(double offertaPrec) {
        this.offertaPrec = offertaPrec;
    }

    public Status getStatus() {
        return status;
    }




}
