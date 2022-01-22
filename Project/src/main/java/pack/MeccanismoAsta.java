package pack;

import java.util.ArrayList;
import java.util.Date;

public class MeccanismoAsta implements AuctionMechanism {
    ArrayList<Asta> aste;

    public MeccanismoAsta() {

        this.aste = new ArrayList<Asta>();
    }

    /**
     * Creates a new auction for a good.
     *
     * @param _auction_name   a String, the name identify the auction.
     * @param _end_time       a Date that is the end time of an auction.
     * @param _reserved_price a double value that is the reserve minimum pricing selling.
     * @param _description    a String describing the selling goods in the auction.
     * @return true if the auction is correctly created, false otherwise.
     */
    @Override
    public boolean createAuction(String _auction_name, Date _end_time, double _reserved_price, String _description) {
        //controllo se esiste già un asta creata da me con questo nome
        Asta a = search(_auction_name);
        if(a != null){
            if(a.getStatus().compareTo(Status.aperta)==0){
                return false;
            }
        }
        //aggiungere logica per ricercare l'asta anche negli altri nodi, se non esiste un asta con lo stesso nome negli
        // altri allora aggiungila tra le mie aste con le seguenti 2 linee e return true
        Asta nuova = new Asta(_auction_name,_description,_end_time,_reserved_price);
        aste.add(nuova);
        return true;
    }

    /**
     * Checks the status of the auction.
     *
     * @param _auction_name a String, the name of the auction.
     * @return a String value that is the status of the auction.
     */
    @Override
    public String checkAuction(String _auction_name) {
        //cerca nelle proprie aste per ottenere lo stato
        Asta a = search(_auction_name);
        if(a != null)
            return a.getStatus().toString();
        return null;
    }

    /**
     * Places a bid for an auction if it is not already ended.
     *
     * @param _auction_name
     * @param _bid_amount   a double value, the bid for an auction.
     * @return a String value that is the status of the auction.
     */
    @Override
    public String placeAbid(String _auction_name, double _bid_amount) {
        return null;
    }

    private Asta search ( String _auction_name){
        for(Asta a: aste ){
            if(a.getName().equals(_auction_name))
                return a;
        }
        return null;
    }
}
