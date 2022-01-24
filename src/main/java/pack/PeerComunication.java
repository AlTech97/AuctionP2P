package pack;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;

import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDirect;
import net.tomp2p.p2p.Peer;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;
import net.tomp2p.storage.Data;

public class PeerComunication {
    final private Peer peer;
    final private PeerDHT dht;
    final private int DEFAULT_MASTER_PORT = 4000;
    final private ArrayList<String> topics = new ArrayList<String>();

    public PeerComunication(int id, String master, final MessageListener listener) throws Exception{
        peer= new PeerBuilder(Number160.createHash(id)).ports(DEFAULT_MASTER_PORT+ id).start();
        dht = new PeerBuilderDHT(peer).start();
        //connessione iniziale del peer creato
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

    public boolean creaTopic(String nomeTopic){
        try {
            //dht.get(location_key) restituisce una mappa [key,value]
            FutureGet futureGet = dht.get(Number160.createHash(nomeTopic)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess() && futureGet.isEmpty())
                //metodo dht.put(key, value) aggiunge una linea nella DHT con chiave e valore associato
                dht.put(Number160.createHash(nomeTopic)).data(new Data(new HashSet<PeerAddress>()))
                        .start().awaitUninterruptibly();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean iscriviAlTopic(String nomeTopic) {
        try {
            //dht.get(location_key) restituisce una mappa [key,value]
            FutureGet futureGet = dht.get(Number160.createHash(nomeTopic)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess()) {
                if(futureGet.isEmpty() ) return false;
                HashSet<PeerAddress> peers_on_topic;
                peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                peers_on_topic.add(dht.peer().peerAddress());
                dht.put(Number160.createHash(nomeTopic)).data(new Data(peers_on_topic)).start().awaitUninterruptibly();
                topics.add(nomeTopic);
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean pubblicaSuTopic(String nomeTopic, Object obj) {
        try {
            FutureGet futureGet = dht.get(Number160.createHash(nomeTopic)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess()) {
                HashSet<PeerAddress> peers_on_topic;
                peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                for(PeerAddress peer:peers_on_topic)
                {
                    FutureDirect futureDir = dht.peer().sendDirect(peer).object(obj).start();
                    futureDir.awaitUninterruptibly();
                }

                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean disiscriviDaTopic(String nomeTopic) {
        try {
            FutureGet futureGet = dht.get(Number160.createHash(nomeTopic)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess()) {
                if(futureGet.isEmpty() ) return false;
                HashSet<PeerAddress> peers_on_topic;
                peers_on_topic = (HashSet<PeerAddress>) futureGet.dataMap().values().iterator().next().object();
                peers_on_topic.remove(dht.peer().peerAddress());
                dht.put(Number160.createHash(nomeTopic)).data(new Data(peers_on_topic)).start().awaitUninterruptibly();
                topics.remove(nomeTopic);
                return true;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean abbandonaRete() {

        for(String topic: new ArrayList<String>(topics)) disiscriviTopic(topic);
        dht.peer().announceShutdown().start().awaitUninterruptibly();
        return true;
    }

}
