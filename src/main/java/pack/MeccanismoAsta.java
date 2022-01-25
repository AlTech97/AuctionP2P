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
            //se la lettura della dht ha successo e non c'è un occorrenza con il nome dell'asta
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
    }

    //rimuovi una mia asta. Solo il proprietario avrà successo
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

    //abbandona un asta non mia
    public boolean leaveAuction(String _auction_name){
        try {
            FutureGet futureGet = dht.get(Number160.createHash(_auction_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess() && nomi.contains(_auction_name) && !futureGet.isEmpty()) {
                HashSet<PeerAddress> peers_on_topic;
                peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                peers_on_topic.remove(dht.peer().peerAddress());
                dht.put(Number160.createHash(_auction_name)).data(new Data(peers_on_topic)).start().awaitUninterruptibly();
                nomi.remove(_auction_name);
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
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

    //abbandona la rete P2P disiscrivendosi prima da tutte le aste
    public void leaveNetwork() {
        for(String nome: new ArrayList<String>(nomi)) leaveAuction(nome);
        dht.peer().announceShutdown().start().awaitUninterruptibly();
    }

    //cerca un asta tra quelle che ho creato e restituiscila se esiste
    private Asta search ( String _auction_name){
        for(Asta a: aste ){
            if(a.getName().equals(_auction_name))
                return a;
        }
        return null;
    }
}
