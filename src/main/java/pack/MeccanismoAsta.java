package pack;

import java.net.InetAddress;
import java.text.DateFormat;
import java.util.*;

import net.tomp2p.dht.*;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.Peer;
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
        //data di oggi
        long milliseconds = System.currentTimeMillis();
        Date data = new Date(milliseconds);
        try {
            //eseguo prima dei controlli sulla correttezza dei parametri
            if(_end_time == null || _end_time.before(data))
                throw new Exception("La data inserita non è corretta\n");
            else{
                if(_reserved_price < 0)
                    throw new Exception("Il prezzo di riserva deve essere maggiore o uguale a zero\n");
                else{
                    ArrayList<String> nomiAste = getEveryAuctionNames();
                    if(nomiAste.contains(_auction_name))
                        throw new Exception("Il nome dell'asta indicato è già esistente\n");
                    else{
                        //inserisco prima l'asta fisicamente e successivamente la aggiungo in lista
                        Asta nuova = new Asta(_auction_name, _description, _end_time, _reserved_price, peer.peerID());
                        FuturePut future = dht.put(Number160.createHash(_auction_name))
                                .data(new Data(nuova)).start().awaitUninterruptibly();
                        if(!future.isSuccess())
                            throw new Exception("Errore durante l'aggiunta dell'asta in lista\n");
                        else{
                            //aggiungi il nome dell'asta nella lista globale
                            nomiAste.add(_auction_name);
                            FuturePut future2 =  dht.put(Number160.createHash("auctionIndex"))
                                    .data(new Data(nomiAste)).start().awaitUninterruptibly();
                            if(!future2.isSuccess()) {
                                //se l'aggiunta in lista non ha successo elimina l'oggetto Asta appena aggiunto alla DHT
                                FutureRemove delete = dht.remove(Number160.createHash(_auction_name)).all()
                                        .start().awaitUninterruptibly();
                                throw new Exception("Errore durante l'aggiornamento della lista dei nomi\n");
                            }
                            else{
                                //tutte le operazioni sono state eseguite con successo, aggiungi la nuova asta alla lista locale delle mie aste
                                aste.add(nuova);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    //rimuovi una mia asta. Solo il proprietario avrà successo
    public boolean removeAuction(String _auction_name){
            try{
                Asta a = localSearch(_auction_name);
                if(a == null || a.isClosed())
                    throw new Exception("Solo il proprietario può rimuovere l'asta\n");
                else {
                    //se l'asta da rimuovere è tra quelle che ho creato ed è ancora aperta
                    //aggiorna la lista di nomi delle aste
                    ArrayList<String> nomiAste = getEveryAuctionNames();
                    if (nomiAste.remove(_auction_name)) {
                        FuturePut future = dht.put(Number160.createHash("auctionIndex"))
                                .data(new Data(nomiAste)).start().awaitUninterruptibly();
                        if (!future.isSuccess())
                            throw new Exception("Errore durante la rimozione del nome dell'asta\n");
                        else {
                            FutureRemove future2 = dht.remove(Number160.createHash(_auction_name)).all()
                                    .start().awaitUninterruptibly();
                            if (!future2.isSuccess()) {
                                //riporto la lista allo stato precedente dato che la rimozione dell'asta è fallita
                                if (nomiAste.add(_auction_name)) {
                                    FuturePut restore = dht.put(Number160.createHash("auctionIndex"))
                                            .data(new Data(nomiAste)).start().awaitUninterruptibly();
                                }
                                throw new Exception("Errore durante la rimozione dell'asta\n");
                            } else {
                                aste.remove(a);
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return false;
    }

    public boolean updateAuction(Asta _auction){
        String _auction_name = _auction.getName();
        try{
            if(localSearch(_auction_name) == null)
                throw new Exception("Solo il proprietario può aggiornare l'asta\n");
            else {
                ArrayList<String> lista = getEveryAuctionNames();
                if (!lista.contains(_auction_name))
                    throw new Exception("Asta non presente in lista\n");
                else {
                    Asta a = globalSearch(_auction_name);
                    if (a == null)
                        throw new Exception("Non è stata trovata alcun'asta con questo nome\n");
                    else {
                        FuturePut future = dht.put(Number160.createHash(_auction_name))
                                .data(new Data(_auction)).start().awaitUninterruptibly();
                        if (!future.isSuccess())
                            throw new Exception("Errore nell'aggiornamento dell'asta\n");
                        return true;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /*
        Per implementare il meccanismo di following per ogni asta creare un ulteriore riga nella DHT
        con, ad esempio, chiave = nomeAsta+"followers" e per dati un HashSet<PeerAddress> contenente tutti
        gli indirizzi dei peer che intendono ricevere aggiornamenti in tempo reale con messaggi contenenti
        lo stato dell'asta e il valore dell'ultima offerta ad esempio.
     */
    public boolean followAuction(String _auction_name){
        /*
        if (!nomi.contains(_auction_name)) {
            try {
                FutureGet futureGet = dht.get(Number160.createHash(_auction_name)).start().awaitUninterruptibly();
                if (futureGet.isSuccess() && !futureGet.isEmpty()) {
                    HashSet<PeerAddress> peers_following = (HashSet<PeerAddress>)
                            futureGet.dataMap().values().iterator().next().object();
                    peers_following.add(dht.peer().peerAddress());
                    dht.put(Number160.createHash(_auction_name)).data(new Data(peers_following))
                            .start().awaitUninterruptibly();
                    nomi.add(_auction_name);
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

            */
        return false;
    }

    //abbandona un asta non mia
    public boolean unfollowAuction(String _auction_name){
            /*
        }
        try {
            FutureGet futureGet = dht.get(Number160.createHash(_auction_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess() && nomi.contains(_auction_name) && !futureGet.isEmpty()) {
                HashSet<PeerAddress> peers_on_auction;
                peers_on_auction = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                peers_on_auction.remove(dht.peer().peerAddress());
                dht.put(Number160.createHash(_auction_name)).data(new Data(peers_on_auction))
                        .start().awaitUninterruptibly();
                nomi.remove(_auction_name);
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        */

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
        try{
            Asta a = globalSearch(_auction_name);
            if(a!= null){
                return a.getStatus().toString();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
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
        /*
        }
        if(!nomi.contains(_auction_name)) {      //se non sono iscritto all'asta su cui voglio puntare prima iscriviti
            followAuction(_auction_name);
        }
        try {
            FutureGet futureGet = dht.get(Number160.createHash(_auction_name)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess() && !futureGet.isEmpty() ) {
                HashSet<PeerAddress> peers_on_topic = (HashSet<PeerAddress>)
                        futureGet.dataMap().values().iterator().next().object();
                //fai la puntata
                for(PeerAddress peer: peers_on_topic) {

                }
                return Status.aperta.toString();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
      */
        return Status.chiusa.toString();
    }

    //abbandona la rete P2P disiscrivendosi prima da tutte le aste
    public void leaveNetwork() {
        for(String nome: new ArrayList<String>(nomi)) 
            unfollowAuction(nome);
        dht.peer().announceShutdown().start().awaitUninterruptibly();
    }

    //cerca un asta tra quelle che ho creato e restituiscila se esiste
    private Asta localSearch ( String _auction_name){
        for(Asta a: aste ){
            if(a.getName().equals(_auction_name))
                return a;
        }
        return null;
    }
    //cerca un asta nella DHT e restituiscila se esiste
    public Asta globalSearch(String _auction_name) throws Exception{
        FutureGet fg = this.dht.get(Number160.createHash(_auction_name)).getLatest().start().awaitUninterruptibly();
        if (fg.isSuccess() && !fg.isEmpty()) {
            return (Asta) fg.data().object();
        }
        else
            return null;
    }
    //ottieni la lista dei nomi di tutte le aste attive
    public ArrayList<String> getEveryAuctionNames() throws Exception{
        FutureGet fg = this.dht.get(Number160.createHash("auctionIndex")).getLatest().start().awaitUninterruptibly();
        if (fg.isSuccess() && !fg.isEmpty()) {
                return (ArrayList<String>) fg.data().object();
        } else{
            return new ArrayList<String>();
        }
    }




}
