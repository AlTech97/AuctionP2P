package pack;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import net.tomp2p.dht.*;
import net.tomp2p.futures.*;
import net.tomp2p.p2p.Peer;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

public class AuctionMechanism implements AuctionMechanismInterface {

    ArrayList<Auction> asteCreate;                  // aste che creo
    final private ArrayList<String> asteSeguite;    //aste a cui partecipo
    final private Peer peer;
    final private PeerDHT dht;
    final static private int DEFAULT_MASTER_PORT = 4000;
    private Thread thread;
    private LinkedBlockingQueue<Message> messageQueue;

    class MessageListener implements MessageListenerInterface{

        public MessageListener(int id)
        {
            messageQueue = new LinkedBlockingQueue<>();
            thread = new Thread(new Worker(id));
            thread.start();
        }

        @Override
        public Object parseMessage(Object obj) {
            Message msg = (Message) obj;
            try {
                messageQueue.put(msg);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "success";
        }
    }

    class Worker implements Runnable{
        int myid;
        public Worker(int id){
            myid= id;
        }

        /**
         * When an object implementing interface {@code Runnable} is used
         * to create a thread, starting the thread causes the object's
         * {@code run} method to be called in that separately executing
         * thread.
         * <p>
         * The general contract of the method {@code run} is that it may
         * take any action whatsoever.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            while(!Thread.interrupted()){
                try {
                    //leggi un messaggio dalla coda condivisa
                    Message msg = messageQueue.take();
                    //un'asta che seguo ha subito una modifica o ha ricevuto un'offerta, stampa l'asta aggiornata
                    if (msg.getType().equals(Message.MessageType.feed))
                        System.out.println("peer"+ myid + ": (Aggiornamento) " + msg.getAsta().toString() +"\n");
                    //ho vinto un'asta, stampa il messaggio di vittoria ricevuto
                    else if (msg.getType().equals(Message.MessageType.victory))
                        System.out.println("peer"+myid + ": (Vittoria) " + msg.getText()+"\n");
                    //ho ricevuto un messaggio di bid
                    else if (msg.getType().equals(Message.MessageType.bid)) {
                        Bid offerta = msg.getBid();
                        Auction asta = localSearch(offerta.getAuctionName());
                        if (asta != null) {
                            if (offerta.getAmount() >= asta.getRiserva()) {
                                //prima offerta in assoluto ricevuta
                                if (asta.getOffertaAtt() == null && asta.getOffertaPrec() == null) {
                                    asta.setOffertaAtt(offerta);
                                    updateAuction(asta);
                                } else { //per tutte le offerte dopo la prima:
                                    //Se l'offerta ricevuta supera quella con valore maggiore
                                    if (offerta.getAmount() > asta.getOffertaAtt().getAmount()) {
                                        asta.setOffertaPrec(asta.getOffertaAtt());
                                        asta.setOffertaAtt(offerta);
                                        updateAuction(asta);
                                    }
                                    //Seconda offerta in assoluto ricevuta (offertaPrec ?? null) che non supera la prima in valore...
                                    //...oppure qualsiasi offerta che abbia un valore minore del primo e maggiore del secondo
                                    else if (asta.getOffertaPrec() == null ||
                                            offerta.getAmount() > asta.getOffertaPrec().getAmount()) {
                                        asta.setOffertaPrec(offerta);
                                        updateAuction(asta);
                                    }
                                }
                            }
                        }
                    }
                    //sono l'owner di un asta e quest'ultima si ?? chiusa, aggiorno la dht con l'asta aggiornata
                    else if (msg.getType().equals(Message.MessageType.dhtUpdate)) {
                        try {
                            Auction update = msg.getAsta();
                            if (update == null)
                                throw new Exception("Errore nella ricezione del messaggio di update\n");
                            else
                                //aggiorna la dht e invia un feed a tutti i followers
                                updateAuction(update);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public AuctionMechanism(int id, String master) throws Exception {
        this.asteCreate = new ArrayList<>();
        this.asteSeguite = new ArrayList<>();

        peer = new PeerBuilder(Number160.createHash(id)).ports(DEFAULT_MASTER_PORT+id).start();
        dht = new PeerBuilderDHT(peer).start();
        FutureBootstrap futureBoot = peer.bootstrap().inetAddress(InetAddress.getByName(master))
                .ports(DEFAULT_MASTER_PORT).start();
        futureBoot.awaitUninterruptibly();
        if(futureBoot.isSuccess()) {
            peer.discover().peerAddress(futureBoot.bootstrapTo().iterator().next())
                    .start().awaitUninterruptibly();
            System.out.println("\nAvvio del peer con id: "+id+ " e master node: " +master+ "\n");
        } else {
            throw new Exception("Error in master peer bootstrap.");
        }

        MessageListener listener = new MessageListener(id);
        peer.objectDataReply((sender, request) -> listener.parseMessage(request));
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
        //salvo la data di oggi
        long milliseconds = System.currentTimeMillis();
        Date data = new Date(milliseconds);
        try {
            //eseguo prima dei controlli sulla correttezza dei parametri
            if(_end_time == null || _end_time.before(data))
                throw new Exception("La data inserita non ?? corretta\n");
            else{
                if(_reserved_price < 0)
                    throw new Exception("Il prezzo di riserva deve essere maggiore o uguale a zero\n");
                else{
                    ArrayList<String> nomiAste = getEveryAuctionNames();
                    if(nomiAste.contains(_auction_name))
                        throw new Exception("Il nome dell'asta indicato ?? gi?? esistente\n");
                    else{
                        //inserisco prima l'asta in dht fisicamente e successivamente la aggiungo in lista
                        Auction nuova = new Auction(_auction_name, _description, _end_time, _reserved_price, peer.peerAddress());
                        FuturePut future = dht.put(Number160.createHash(_auction_name))
                                .data(new Data(nuova)).start().awaitUninterruptibly();
                        if(!future.isSuccess())
                            throw new Exception("Errore durante l'aggiunta dell'asta in lista\n");
                        else{
                            //aggiungi il nome dell'asta nella lista globale
                            nomiAste.add(_auction_name);
                            FuturePut future2 =  dht.put(Number160.createHash("auctionList"))
                                    .data(new Data(nomiAste)).start().awaitUninterruptibly();
                            if(!future2.isSuccess()) {
                                //se l'aggiunta in lista non ha successo elimina l'oggetto Auction appena aggiunto alla DHT
                                dht.remove(Number160.createHash(_auction_name)).all()
                                        .start().awaitUninterruptibly();
                                throw new Exception("Errore durante l'aggiornamento della lista dei nomi\n");
                            }
                            else {
                                FutureGet futureGet = dht.get(Number160.createHash(_auction_name + "Followers"))
                                        .start().awaitUninterruptibly();
                                if (!futureGet.isSuccess() || !futureGet.isEmpty())
                                    throw new Exception("Eesiste gi?? una lista dei followers collegata all'asta creata\n");
                                else {
                                    //crea la lista di followers dell'asta appena creata
                                    FuturePut future3 = dht.put(Number160.createHash(_auction_name + "Followers"))
                                            .data(new Data(new HashSet<PeerAddress>())).start().awaitUninterruptibly();
                                    if (!future3.isSuccess()) {
                                        //se la creazione della lista dei followers non va a buon fine elimina l'oggetto dell'asta
                                        dht.remove(Number160.createHash(_auction_name)).all()
                                                .start().awaitUninterruptibly();
                                        //elimina anche il nome dell'asta dalla lista globale
                                        nomiAste.remove(_auction_name);
                                        dht.put(Number160.createHash("auctionList"))
                                                .data(new Data(nomiAste)).start().awaitUninterruptibly();
                                        throw new Exception("Errore durante la creazione della lista dei followers dell'asta\n");
                                    } else {
                                        //aggiungi la nuova asta alla lista locale delle aste che ho creato
                                        asteCreate.add(nuova);
                                        return true;
                                    }
                                }
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

    /**
     * Remove an auction. Only the owner will have success
     * @param _auction_name a String, the name of the auction.
     * @return true if the auction is correctly removed, false otherwise.
     */
    @Override
    public boolean removeAuction(String _auction_name){
            try{
                Auction a = localSearch(_auction_name);
                if(a == null)
                    throw new Exception("Solo il proprietario pu?? rimuovere l'asta\n");
                else {
                    //se l'asta da rimuovere ?? tra quelle che ho creato
                    //aggiorna la lista di nomi delle aste
                    ArrayList<String> nomiAste = getEveryAuctionNames();
                    if (!nomiAste.remove(_auction_name))
                        throw new Exception("L'asta indicata non ?? in lista\n");
                    else{
                        FuturePut future = dht.put(Number160.createHash("auctionList"))
                                .data(new Data(nomiAste)).start().awaitUninterruptibly();
                        if (!future.isSuccess())
                            throw new Exception("Errore durante la rimozione del nome dell'asta\n");
                        else {
                            //rimuovi l'oggetto dell'asta dalla DHT
                            FutureRemove future2 = dht.remove(Number160.createHash(_auction_name)).all()
                                    .start().awaitUninterruptibly();
                            if (!future2.isSuccess()) {
                                //riporto la lista allo stato precedente dato che la rimozione dell'oggetto dell'asta ?? fallita
                                nomiAste.add(_auction_name);
                                dht.put(Number160.createHash("auctionList"))
                                        .data(new Data(nomiAste)).start().awaitUninterruptibly();
                                throw new Exception("Errore durante la rimozione dell'asta\n");
                            } else {
                                FutureRemove future3 = dht.remove(Number160.createHash(_auction_name + "Followers")).all()
                                        .start().awaitUninterruptibly();
                                if (!future3.isSuccess()) {
                                    //se fallisce la rimozione della lista dei followers ripristina la lista dei nomi
                                    //e aggiungi di nuovo l'oggetto dell'asta rimosso prima
                                    nomiAste.add(_auction_name);
                                    dht.put(Number160.createHash("auctionList"))
                                            .data(new Data(nomiAste)).start().awaitUninterruptibly();
                                    dht.put(Number160.createHash(_auction_name))
                                            .data(new Data(localSearch(_auction_name))).start().awaitUninterruptibly();
                                    throw new Exception("Errore durante la rimozione della lista di followers dell'asta\n");
                                }
                                //se tutte e tre le rimozioni hanno avuto successo rimuovi l'asta dalla lista locale
                                asteCreate.remove(a);
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

    /**
     * Update an auction by replacing the old one with the new one.
     * Note that only the owner can update the auction
     * @param _auction an Auction that replacing the current one
     * @return true if the update is successful, false otherwise
     */
    @Override
    public boolean updateAuction(Auction _auction){
        String _auction_name = _auction.getName();
        try{
            Auction myAuction = localSearch(_auction_name);
            if( myAuction == null)
                throw new Exception("Solo il proprietario pu?? aggiornare l'asta\n");
            else {
                ArrayList<String> lista = getEveryAuctionNames();
                if (!lista.contains(_auction_name))
                    throw new Exception("Auction non presente in lista\n");
                else {
                    Auction a = globalSearch(_auction_name);
                    if (a == null)
                        throw new Exception("L'oggetto dell'asta da aggiornare non ?? stato trovato\n");
                    else {
                        FuturePut future = dht.put(Number160.createHash(_auction_name))
                                .data(new Data(_auction)).start().awaitUninterruptibly();
                        if (future.isSuccess()) {
                            sendFeedMessage(_auction);
                            asteCreate.remove(myAuction);
                            asteCreate.add(_auction);
                            return true;
                        } else
                            throw new Exception("Errore nell'aggiornamento dell'asta\n");

                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void sendFeedMessage(Auction toSend){
        String _auction_name = toSend.getName();
        try {
            FutureGet futureGet = dht.get(Number160.createHash(_auction_name + "Followers"))
                    .start().awaitUninterruptibly();
            if (!futureGet.isSuccess())
                throw new Exception("Errore nel prelievo della lista dei followers dell'asta\n");
            else {
                HashSet<PeerAddress> peers_following = (HashSet<PeerAddress>)
                        futureGet.dataMap().values().iterator().next().object();
                Message msg = new Message(toSend, peer.peerAddress(), Message.MessageType.feed);
                for (PeerAddress follower : peers_following)
                    dht.peer().sendDirect(follower).object(msg).start().awaitUninterruptibly();
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Follow an auction to receive all its updates
     * @param _auction_name a String, the name of the auction
     * @return true if the the operation is successful, false otherwise
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean followAuction(String _auction_name){
        try {
            if (asteSeguite.contains(_auction_name))
                throw new Exception("Segui gi?? quest'asta\n");
            else {
                String stato = checkAuction(_auction_name);
                if(stato != null) {
                    if (stato.equals(Status.chiusa.toString()))
                        throw new Exception("Non ?? possibile seguire un'asta chiusa\n");
                    else {
                        FutureGet futureGet = dht.get(Number160.createHash(_auction_name + "Followers"))
                                .start().awaitUninterruptibly();
                        if (!futureGet.isSuccess())
                            throw new Exception("Errore nel prelievo della lista dei followers dell'asta\n");
                        else {
                            HashSet<PeerAddress> peers_following = (HashSet<PeerAddress>)
                                    futureGet.dataMap().values().iterator().next().object();
                            peers_following.add(dht.peer().peerAddress());
                            FuturePut fp = dht.put(Number160.createHash(_auction_name + "Followers"))
                                    .data(new Data(peers_following)).start().awaitUninterruptibly();
                            if (!fp.isSuccess())
                                throw new Exception("Errore nell'aggiornamento della lista dei followers dell'asta\n");
                            else {
                                //aggiungo il nome dell'asta alla lista di quelle seguite
                                asteSeguite.add(_auction_name);
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

    /**
     * stop following an auction to no longer receive updates
     * @param _auction_name a String, the name of the auction
     * @return true if the the operation is successful, false otherwise
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean unfollowAuction(String _auction_name){
        try {
            if (!asteSeguite.contains(_auction_name))
                throw new Exception("Non segui quest'asta\n");
            else {
                FutureGet futureGet = dht.get(Number160.createHash(_auction_name + "Followers"))
                        .start().awaitUninterruptibly();
                if (!futureGet.isSuccess() || futureGet.isEmpty())
                    throw new Exception("Errore nel prelievo della lista dei followers dell'asta\n");
                else {
                    HashSet<PeerAddress> peers_following =
                            (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                    peers_following.remove(dht.peer().peerAddress());
                    FuturePut fp = dht.put(Number160.createHash(_auction_name + "Followers"))
                            .data(new Data(peers_following)).start().awaitUninterruptibly();
                    if (!fp.isSuccess())
                        throw new Exception("Errore nell'aggiornamento della lista dei followers dell'asta\n");
                    else {
                        asteSeguite.remove(_auction_name);
                        return true;
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Places a bid for an auction if it is not already ended.
     *
     * @param _auction_name name of the auction
     * @param _bid_amount   a double value, the bid for an auction.
     * @return a String value that is the status of the auction.
     */
    @Override
    public String placeAbid(String _auction_name, double _bid_amount) {
        try {
            if(localSearch(_auction_name) != null)
                throw new Exception("Non ?? possibile puntare su una propria asta\n");
            else{
                //controlla lo stato dell'asta e la chiude se ?? scaduto il tempo
                String stato = checkAuction(_auction_name);
                if(stato!= null){
                    if (stato.equals(Status.chiusa.toString())) {
                        throw new Exception("L'asta ?? chiusa, non ?? possibile effettuare la puntata\n");
                    }
                    else {
                        //ottieni l'oggetto dell'asta, non lancia eccezioni siccome globalSearch gi?? le contiene
                        Auction asta = globalSearch(_auction_name);
                        if (asta != null) {
                            //controlla la validit?? dell'offerta
                            if (_bid_amount < asta.getRiserva())
                                throw new Exception("Prezzo di riserva non raggiunto dalla tua offerta\n");
                            else {
                                //invia l'offerta all'owner dell'asta
                                Bid puntata = new Bid(peer.peerAddress(), _auction_name, _bid_amount);
                                Message msg = new Message(puntata, peer.peerAddress());
                                FutureDirect fd = dht.peer().sendDirect(asta.getOwner()).object(msg)
                                        .start().awaitUninterruptibly();
                                if (fd.isFailed()) {
                                    throw new Exception("Errore durante l'invio del messaggio della puntata, riprova\n");
                                }
                            }
                        }
                    }
                    return stato;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
       return null;
    }


    /**
     * Checks the status of the auction and close it if the time is expired
     *
     * @param _auction_name a String, the name of the auction.
     * @return a String value that is the status of the auction.
     */
    @Override
    public String checkAuction(String _auction_name) {
        try{
            Auction asta = globalSearch(_auction_name);
            if(asta!= null){
                //se il tempo ?? scaduto avvia la procedura di vincita
                if(asta.timeClose()) {
                    declareTheWinner(asta);
                }
                return asta.getStatus().toString();
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private void declareTheWinner(Auction _auction) throws Exception {
        String prezzoFinale ;
        if(_auction.getOffertaPrec()==null)
            if(_auction.getOffertaAtt() != null)
                prezzoFinale = String.valueOf(_auction.getOffertaAtt().getAmount());

            else {   //l'asta ?? scaduta senza nessuna puntata, non c'?? un vincitore
                //invia il messaggio per aggiornare la dht con l'asta in stato chiuso
                sendUpdateAuctionMessage(_auction);
                return;
            }
        else
            prezzoFinale = String.valueOf(_auction.getOffertaPrec().getAmount());

        Message msg = new Message("Complimenti, hai vinto l'asta \"" + _auction.getName()
                + "\". Il prezzo con cui ti sei aggiudicato il prodotto ??: "+ prezzoFinale, peer.peerAddress());
        PeerAddress winnerAddress = _auction.getOffertaAtt().getOwner();
        FutureDirect fd = dht.peer().sendDirect(winnerAddress).object(msg).start();
        fd.awaitUninterruptibly();
        if(fd.isFailed())
            throw new Exception("Errore durante l'invio del messaggio di vittoria\n");

        //aggiorno l'oggetto dell'asta includendo l'indirizzo del vincitore
        _auction.setWinner(winnerAddress);
        //invio un messaggio all'owner per fargli aggiornare l'oggetto contenente il vincitore
        sendUpdateAuctionMessage(_auction);
    }

    private void sendUpdateAuctionMessage(Auction _auction) throws Exception{
        //se l'asta ?? scaduta e non sono l'owner invia un messaggio a quest'ultimo per avvisarlo
        if(_auction.getOwner() != peer.peerAddress()){
            Message msg2 = new Message(_auction, peer.peerAddress(), Message.MessageType.dhtUpdate);
            FutureDirect fd2 = dht.peer().sendDirect(_auction.getOwner()).object(msg2)
                    .start().awaitUninterruptibly();
            if(fd2.isFailed())
                throw new Exception("Errore durante l'invio del messaggio di terminazione dell'asta all'owner\n");
        }
        else{   //se sono l'owner aggiorna direttamente la dht
            updateAuction(_auction);
        }
    }

    /**
     * Search for an auction among those created by the peer
     * @param _auction_name a String, the name of the auction
     * @return the Auction object found, null otherwise
     */
    public Auction localSearch (String _auction_name){
        for(Auction a: asteCreate){
            if(a.getName().equals(_auction_name))
                return a;
        }
        return null;
    }

    /**
     * Search for an auction among all existing ones
     * @param _auction_name a String, the name of the auction
     * @return the Auction object found, null otherwise
     */
    @Override
    public Auction globalSearch(String _auction_name){
        try {
        FutureGet fg = this.dht.get(Number160.createHash(_auction_name)).getLatest().start().awaitUninterruptibly();
        if(!fg.isSuccess() || fg.isEmpty())
            throw new Exception("L'oggetto dell'asta richiesto non ?? stato trovato\n");
        else
            return (Auction) fg.data().object();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get the unique name of all existing auctions
     * @return an ArrayList that contains the list of the names
     */
    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> getEveryAuctionNames() {
        try{
            FutureGet fg = this.dht.get(Number160.createHash("auctionList")).getLatest().start().awaitUninterruptibly();
            if (fg.isSuccess() && !fg.isEmpty())
                return (ArrayList<String>) fg.data().object();

        } catch (Exception e){
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    /**
     *  Get the list of all open auctions
      * @return an ArrayList that contains all open auctions
     */
    @Override
    public ArrayList<Auction> getOpenAuctions(){
        ArrayList<Auction> result = new ArrayList<>();
        try {
            ArrayList<String> names = getEveryAuctionNames();
            if (names.isEmpty())
                throw new Exception("La lista dei nomi delle aste ?? vuota\n");
            else {
                for(String name : names){
                    Auction a = globalSearch(name);
                    String stato =checkAuction(name);
                    if(a!=null && stato!= null && stato.equals(Status.aperta.toString()))
                        result.add(a);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     * leave the P2P network and unsubscribe from the auctions
     * @return true if unsubscribe from all the auctions followed is done correctly, false otherwise
     */
    @Override
    public boolean leaveNetwork(){
        boolean result = true, b;
        for(String nome: new ArrayList<>(asteSeguite)){
            b = unfollowAuction(nome);
            if(!b)
                result=false;
        }
        dht.peer().announceShutdown().start().awaitUninterruptibly();
        thread.interrupt();
        return result;
    }

    public ArrayList<String> getAsteSeguite(){
        return asteSeguite;
    }

    public ArrayList<Auction> getAsteCreate(){
        return asteCreate;
    }

    public PeerAddress getAddress(){
        return peer.peerAddress();
    }

}
