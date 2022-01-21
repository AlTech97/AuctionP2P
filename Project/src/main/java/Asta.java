import java.util.Date;

public class Asta {
    String name, description;
    Date endTime;
    double riserva;
    double offerta;
    Status status;  //enumerazione dello stato dell'asta


    public Asta(String name, String description, Date endTime, double minPrice) {
        this.name = name;
        this.description = description;
        this.endTime = endTime;
        this.riserva = minPrice;
        this.offerta = 0.0;
        this.status = Status.aperta;
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
    public double getOfferta() {
        return offerta;
    }

    public void setOfferta(double offerta) {
        this.offerta = offerta;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


}
