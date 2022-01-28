package testPack;

import org.junit.jupiter.api.*;
import pack.Asta;
import pack.MeccanismoAsta;
import pack.MessageListener;
import pack.Status;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JunitTestAuction {
    protected MeccanismoAsta peer0, peer1, peer2, peer3;

    public JunitTestAuction() throws Exception{
        class MessageListenerImpl implements MessageListener {
            int peerid;
            public MessageListenerImpl(int peerid)
            {
                this.peerid=peerid;
            }
            public Object parseMessage(Object obj) {
                System.out.println(peerid+"] (Direct Message Received) "+obj);
                return "success";
            }

        }
        peer0 = new MeccanismoAsta(0, "127.0.0.1", new MessageListenerImpl(0));
        peer1 = new MeccanismoAsta(1, "127.0.0.1", new MessageListenerImpl(1));
        peer2 = new MeccanismoAsta(2, "127.0.0.1", new MessageListenerImpl(2));
        peer3 = new MeccanismoAsta(3, "127.0.0.1", new MessageListenerImpl(3));
    }
    @Test
    @Order(1)
    @Disabled
    void testCaseCreateAuction(TestInfo testInfo) throws ParseException {
        DateFormat formatoData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY);
        formatoData.setLenient(false);
        Date data = formatoData.parse("30/01/2022");
        Date dataErrata = formatoData.parse("26/01/2022");
        //inserisco un'asta nella maniera corretta
        assertTrue(peer1.createAuction("Portachiavi", data , 2.0, "come nuovo"));
        //inserisco un'asta con lo stesso nome della precedente
        assertFalse(peer1.createAuction("Portachiavi", data , 2.0, "come nuovo"));
        //inserisco un'asta con prezzo di riserva < 0
        assertFalse(peer1.createAuction("Quadro", data, -1, "originale"));
        //inserisco un'asta con data errata
        assertFalse(peer1.createAuction("Sedia", dataErrata , 20.0, "in ottimo stato"));
    }
    @Test
    @Order(2)
    void testCaseRemoveAuction(TestInfo testInfo) throws ParseException {
        DateFormat formatoData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY);
        formatoData.setLenient(false);
        Date data = formatoData.parse("30/01/2022");
        //il primo peer crea un'asta
        assertTrue(peer1.createAuction("Anello", data , 50.0, "mai indossato"));
        //un altro peer tenta di rimuovere l'asta appena creata
        assertFalse(peer3.removeAuction("Anello"));
        //il proprietario elimina l'asta che ha creato prima
        assertTrue(peer1.removeAuction("Anello"));
    }
    @Test
    @Order(3)
    @Disabled
    void testCaseUpdateAuction(TestInfo testInfo) throws Exception {
        DateFormat formatoData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY);
        formatoData.setLenient(false);
        Date data = formatoData.parse("30/01/2022");
        //il primo peer crea un'asta
        assertTrue(peer1.createAuction("Anello", data , 50.0, "mai indossato"));
        Asta a = peer1.globalSearch("Anello");
        System.out.println("Attualmente la riserva è di: " + a.getRiserva());
        a.setRiserva(20.0);
        //provo a far fare l'aggiornamento dell'asta ad un peer diverso dal proprietario e poi dal proprietario
        assertFalse(peer2.updateAuction(a));
        assertTrue(peer1.updateAuction(a));
        Asta b = peer1.globalSearch("Anello");
        System.out.println("Ora la riserva è di: " + b.getRiserva());
    }

/*
    @Test
    void testCasePlaceAbid(TestInfo testInfo){
        assertEquals( Status.aperta.toString(), peer3.placeAbid("Portachiavi", 10.0));
    }S
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
