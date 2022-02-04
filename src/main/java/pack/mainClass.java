package pack;

import org.kohsuke.args4j.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class mainClass {
    @Option(name="-m", aliases="--masterip", usage="the master peer ip address", required=true)
    private static String master;
    @Option(name="-id", aliases="--identifierpeer", usage="the unique identifier for this peer", required=true)
    private static int id;

    public static void main(String[] args){
        try{

            AuctionMechanism peer = new AuctionMechanism(id, master);
            System.out.println("\nAvvio del peer con id: "+id+ " e master node: " +master+ "\n");

            while(true){
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("MENU: Digita un numero per effettuare l'operazione\n");
                System.out.println("(0) - MOSTRA TUTTE LE ASTE APERTE\n");
                System.out.println("(1) - CREA UN'ASTA\n");
                System.out.println("(2) - ELIMINA ASTA\n");
                System.out.println("(3) - SEGUI UN'ASTA\n");
                System.out.println("(4) - SMETTI DI SEGUIRE UN'ASTA\n");
                System.out.println("(5) - FAI UNA PUNTATA\n");
                System.out.println("(6) - VERIFICA LO STATO DI UN'ASTA\n");
                System.out.println("(7) - ESCI\n");
                int menu=  Integer.parseInt(br.readLine());
                String nome;        //nome dell'asta su cui operare
                switch(menu){
                    case 0: //mostra tutte le aste aperte
                        ArrayList<Auction> aste = peer.getOpenAuctions();
                        System.out.println("Ecco tutte le aste attualmente attive:\n");
                        for(Auction a : aste){
                            System.out.println(a+"\n");
                        }
                        break;
                    case 1: //crea un'asta
                        System.out.println("\nInserisci il nome dell'asta che vuoi creare: \n");
                        nome = br.readLine();
                        System.out.println("Inserisci la descrizione dell'asta: \n");
                        String descrizione = br.readLine();
                        System.out.println("Inserisci il prezzo di riserva Es:10.90 \n");
                        double prezzo = Double.parseDouble(br.readLine());
                        System.out.println("Inserisci la data di termine dell'asta [gg/mm/yyyy]: \n");
                        try{
                            DateFormat formatoData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY);
                            formatoData.setLenient(false);
                            Date data = formatoData.parse(br.readLine());

                            if(peer.createAuction(nome, data, prezzo, descrizione))
                                System.out.println("Auction creata con successo\n");
                            else
                                System.out.println("Errore durante la creazione dell'asta\n");

                        }catch (ParseException e) {
                            System.out.println("Formato data non valido.\n");
                        }
                        break;
                case 2:     //elimina un'asta
                        System.out.println("Inserisci il nome dell'asta da eliminare: \n");
                        nome = br.readLine();
                        if(peer.removeAuction(nome))
                            System.out.println("Asta eliminata con successo\n");
                        else
                            System.out.println("Errore durante l'eliminazione dell'asta\n");
                        break;
                    case 3: //follow dell'asta
                        System.out.println("Inserisci il nome dell'asta su cui vuoi restare aggiornato: \n");
                        nome = br.readLine();
                        if(peer.followAuction(nome))
                            System.out.println("Ora segui l'asta indicata\n");
                        else
                            System.out.println("Operazione non riuscita\n");
                        break;
                    case 4: //unfollow dell'asta
                        System.out.println("Inserisci il nome dell'asta di cui non vuoi più ricevere notifiche: \n");
                        nome = br.readLine();
                        if(peer.unfollowAuction(nome))
                            System.out.println("Hai abbandonato l'asta\n");
                        else
                            System.out.println("Operazione non riuscita\n");
                        break;
                    case 5: //fai una puntata
                        System.out.println("Inserisci il nome dell'asta su cui vuoi puntare: \n");
                        nome = br.readLine();
                        System.out.println("Inserisci il valore del'offerta Es:10.90 \n");
                        double puntata = Double.parseDouble(br.readLine());

                        //se la puntata è stata fatta su un asta aperta
                        if(Status.aperta.toString().compareTo(peer.placeAbid(nome, puntata)) == 0)
                            System.out.println("Puntata effettuata\n");
                        else
                            System.out.println("Puntata non effettuata\n");
                        break;
                    case 6:
                        System.out.println("Inserisci il nome dell'asta di cui vuoi controllare lo stato: \n");
                        nome = br.readLine();
                        String stato = peer.checkAuction(nome);
                        if(stato == null)
                            System.out.println("Asta inesistente\n");
                        else
                            System.out.println("l'asta '"+nome+"' è " +stato);
                        break;
                    case 7: //exit
                        peer.leaveNetwork();
                        System.out.println("Rete abbandonata, termino il programma.\n");
                        System.exit(0);
                    default:
                        System.out.println("Valore errato, inserire un numero corretto\n");
                        break;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
