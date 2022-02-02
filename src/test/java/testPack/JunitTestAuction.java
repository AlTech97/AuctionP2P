package testPack;

import org.junit.jupiter.api.*;
import pack.Auction;
import pack.AuctionMechanism;
import pack.Status;

import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class JunitTestAuction {
    private static AuctionMechanism peer0, peer1, peer2, peer3;

    @BeforeAll
    static void testCaseGeneratePeers(){
        assertDoesNotThrow(() ->peer0 = new AuctionMechanism(0, "127.0.0.1"));
        assertDoesNotThrow(() ->peer1 = new AuctionMechanism(1, "127.0.0.1"));
        assertDoesNotThrow(() ->peer2 = new AuctionMechanism(2, "127.0.0.1"));
        assertDoesNotThrow(() ->peer3 = new AuctionMechanism(3, "127.0.0.1"));

        assertThrows(Exception.class, () -> peer0 = new AuctionMechanism(0, "127.0.0.5"));
    }

    @Test
    @Order (1)
    //@Disabled
    void testCaseCreateAuction() {
        //data di oggi
        long milliseconds = System.currentTimeMillis();
        long unGiorno = 86400000;
        Date dataCorretta = new Date(milliseconds + unGiorno);
        Date dataErrata = new Date(milliseconds - unGiorno);
                //inserisco un'asta nella maniera corretta
        assertTrue(peer1.createAuction("Portachiavi", dataCorretta , 3.0, "come nuovo"));
        //lo stesso peer inserisce un'asta con lo stesso nome della precedente
        assertFalse(peer1.createAuction("Portachiavi", dataCorretta , 5.0, "realizzato a mano"));
        //un peer differente inserisce un'asta con lo stesso nome della precedente
        assertFalse(peer2.createAuction("Portachiavi", dataCorretta , 7.0, "nuovo"));
        //inserisco un'asta con prezzo di riserva < 0
        assertFalse(peer1.createAuction("Quadro", dataCorretta, -1, "originale"));
        //inserisco un'asta con data errata
        assertFalse(peer1.createAuction("Sedia", dataErrata , 20.0, "in ottimo stato"));
    }
    @Test
    @Order(2)
    //@Disabled
    void testCaseRemoveAuction() {
        long milliseconds = System.currentTimeMillis();
        long unGiorno = 86400000;
        Date dataCorretta = new Date(milliseconds + unGiorno);
        //il primo peer crea un'asta
        assertTrue(peer1.createAuction("Anello", dataCorretta , 50.0, "mai indossato"));
        //un altro peer tenta di rimuovere l'asta appena creata
        assertFalse(peer3.removeAuction("Anello"));
        //il proprietario elimina l'asta che ha creato prima
        assertTrue(peer1.removeAuction("Anello"));
    }
    @Test
    @Order(3)
    //@Disabled
    void testCaseUpdateAuction(){
        long milliseconds = System.currentTimeMillis();
        long unGiorno = 86400000;
        Date dataCorretta = new Date(milliseconds + unGiorno);
        //il primo peer crea un'asta
        assertTrue(peer1.createAuction("Bracciale", dataCorretta , 50.0, "mai indossato"));

        Auction a = peer1.globalSearch("Bracciale");
        System.out.println("Attualmente la riserva è di: " + a.getRiserva());
        a.setRiserva(20.0);

        //provo a far fare l'aggiornamento dell'asta ad un peer diverso dal proprietario e poi dal proprietario
        assertFalse(peer2.updateAuction(a));
        assertTrue(peer1.updateAuction(a));

        Auction b = peer1.globalSearch("Bracciale");
        System.out.println("Ora la riserva è di: " + b.getRiserva());
    }
    @Test
    @Order(4)
    //@Disabled
    void testCaseFollowUnfollowAuction(){
        long milliseconds = System.currentTimeMillis();
        long unGiorno = 86400000;
        Date dataCorretta = new Date(milliseconds + unGiorno);
        //il primo peer crea un'asta
        assertTrue(peer1.createAuction("Collana", dataCorretta , 50.0, "mai indossato"));
        //il secondo peer segue l'asta per ricevere le info sugli aggiornamenti
        assertTrue(peer2.followAuction("Collana"));

        //aggiorna l'asta con i nuovi valori e fai arrivare al secondo peer le info
        Auction a = peer1.globalSearch("Collana");
        a.setRiserva(20.0);
        a.setDescription("mai indossato, spedizione inclusa");

        assertTrue(peer1.updateAuction(a));

        //il secondo peer esegue l'unfollow per non ricevere i successivi aggiornamenti
        assertTrue(peer2.unfollowAuction("Collana"));
        //effettuaimo un altro aggiornamento e notiamo che al secondo peer non arriva il messaggio
        Auction b = peer1.globalSearch("Collana");
        b.setRiserva(15.0);
        b.setDescription("mai indossato, spedizione a carico del cliente");

        assertTrue(peer1.updateAuction(a));
    }

    @Test
    @Order(5)
    //@Disabled
    void testCasePlaceAbid(){
        long milliseconds = System.currentTimeMillis();
        long unGiorno = 86400000;
        Date dataCorretta = new Date(milliseconds + unGiorno);
        //creo un asta su cui puntare
        assertTrue(peer1.createAuction("Computer", dataCorretta , 250.0, "nuovo"));
        //l'owner fa un offerta sulla propria asta
        assertNull(peer1.placeAbid("Computer", 270.0));
        //un altro peer fa un offerta più bassa della riserva
        assertNull(peer2.placeAbid("Computer", 200.0));
        //lo stesso peer rifà l'offerta corretta
        assertNotNull(peer2.placeAbid("Computer", 270.0));

        //testo il meccanismo di follow per ricevere gli aggiornamenti
        assertTrue(peer2.followAuction("Computer"));
        //testo il valore restituito da un'operazioned di bid su un asta aperta
        assertEquals(peer3.placeAbid("Computer", 280.0), Status.aperta.toString());

    }

    @AfterAll
    static void leaveNetwork() {
        assertTrue(peer0.leaveNetwork());
        assertTrue(peer1.leaveNetwork());
        assertTrue(peer2.leaveNetwork());
        assertTrue(peer3.leaveNetwork());
    }
/*

    //TODO to remove it!
    @Test
    void testCaseGeneral(TestInfo testInfo){

        try {
            DateFormat formatoData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY);
            formatoData.setLenient(false);
            Date data = formatoData.parse("27/01/2022");
            peer1.createAuction("Moneta", data , 3.0, "storica");
            peer1.placeAbid("Moneta", 5.0);
            peer2.placeAbid("Moneta", 7.0);
            peer3.placeAbid("Moneta", 10.0);

            peer1.createAuction("Portafoglio", data , 20.0, "in vera pelle");
            peer1.placeAbid("Portafoglio", 22.0);
            peer2.placeAbid("Portafoglio", 24.0);
            peer3.placeAbid("Portafoglio", 28.0);


            peer1.unfollowAuction("Moneta");

            peer2.leaveNetwork();

            peer1.removeAuction("Moneta");
            System.exit(0);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
*/


}
