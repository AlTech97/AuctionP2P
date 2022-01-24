package pack;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Date;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;


public class MeccanismoAsta implements AuctionMechanism {
    ArrayList<Asta> aste;   // aste che creo
    final private ArrayList<String> nomi;   //aste a cui partecipo
    final private Peer peer;
    final private PeerDHT dht;
    final private int DEFAULT_MASTER_PORT = 4000;

    public MeccanismoAsta(int id, String master, final MessageListener listener) throws Exception{
        this.aste = new ArrayList<Asta>();
        this.nomi = new ArrayList<String>();
        peer= new PeerBuilder(Number160.createHash(id)).ports(DEFAULT_MASTER_PORT+ id).start();
        dht = new PeerBuilderDHT(peer).start();
        FutureBootstrap futureBoot = peer.bootstrap().inetAddress(InetAddress.getByName(master))
                .ports(DEFAULT_MASTER_PORT).start();
        futureBoot.awaitUninterruptibly();
        if(futureBoot.isSuccess()){
            peer.discover().peerAddress(futureBoot.bootstrapTo().iterator().next())
                    .start().awaitUninterruptibly();
        } else{
            throw new Exception("Error in master peer bootstrap.");
        }
        peer.objectDataReply(new ObjectDataReply() {

            public Object reply(PeerAddress sender, Object request) throws Exception {
                return listener.parseMessage(request);
            }
        });

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
        try {
            //dht.get(location_key) restituisce una coppia [key,value]
            FutureGet futureGet = dht.get(Number160.createHash(_auction_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess() && futureGet.isEmpty()) {
                //metodo dht.put(key, value) aggiunge una linea nella DHT con chiave e valore associato
                dht.put(Number160.createHash(_auction_name)).data(new Data(new HashSet<PeerAddress>()))
                        .start().awaitUninterruptibly();
                Asta nuova = new Asta(_auction_name, _description, _end_time, _reserved_price, peer.peerID());
                aste.add(nuova);
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
        /*
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
        */
    }

    //rimuovi un asta, solo il proprietario dell'asta avrà successo
    public boolean removeAuction(String _auction_name){
        //rimozione dalla lista e cambiamento di stato in chiusa.
        Asta a = search(_auction_name);
        if(a != null) {
            if(a.close(peer.peerID())){
                aste.remove(a);
                return true;
            }

        }
        return false;
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
