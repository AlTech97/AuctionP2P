package pack;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainClass {
    @Option(name="-m", aliases="--masterip", usage="the master peer ip address", required=true)
    private static String master;
    @Option(name="-id", aliases="--identifierpeer", usage="the unique identifier for this peer", required=true)
    private static int id;

    public static void main(String[] args) throws Exception {
        //ottengo i parametri dalla linea di comando
        MainClass classe = new MainClass();
        final CmdLineParser parser = new CmdLineParser(classe);
        parser.parseArgument(args);
        //inizializzo il meccanismo d'asta con i parametri letti
        AuctionMechanism peer = new AuctionMechanism(id, master);
        //dichiaro una serie di variabili e oggetti che mi serviranno nel menù
        String  nome, in;
        String regex = "[0-9]+";
        Pattern p = Pattern.compile(regex);
        Matcher m;
        int menu;
        DateFormat formatoData = DateFormat.getDateInstance(DateFormat.SHORT, Locale.ITALY);
        formatoData.setLenient(false);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            try {

                System.out.println("\nMENU: Digita un numero per effettuare l'operazione\n");
                System.out.println("(0) - ELENCA TUTTE LE ASTE APERTE\n");
                System.out.println("(1) - CREA UN'ASTA\n");
                System.out.println("(2) - ELENCA LE ASTE CHE HAI CREATO\n");
                System.out.println("(3) - MODIFICA UN'ASTA\n");
                System.out.println("(4) - ELIMINA ASTA\n");
                System.out.println("(5) - SEGUI UN'ASTA\n");
                System.out.println("(6) - ELENCA TUTTE LE ASTE CHE SEGUI\n");
                System.out.println("(7) - SMETTI DI SEGUIRE UN'ASTA\n");
                System.out.println("(8) - FAI UNA PUNTATA\n");
                System.out.println("(9) - VERIFICA LO STATO DI UN'ASTA\n");
                System.out.println("(10) - ESCI\n");

                in = br.readLine();
                m = p.matcher(in);
                if (in.isEmpty() || !m.matches())
                    menu = -1;
                else
                    menu = Integer.parseInt(in);

                switch (menu) {
                    case 0: //mostra tutte le aste aperte
                        ArrayList<Auction> aste = peer.getOpenAuctions();
                        if (!aste.isEmpty()){
                            System.out.println("Ecco tutte le aste attualmente attive:\n");
                            for (Auction a : aste) {
                                System.out.println(a + "\n");
                            }
                        }else
                            System.out.println("\nNon ci sono aste attive\n");
                        break;

                    case 1: //crea un'asta
                        System.out.println("\nInserisci il nome dell'asta che vuoi creare: \n");
                        nome = br.readLine();
                        if (nome.isEmpty()) {
                            System.out.println("Il nome dell'asta non può essere vuoto\n");
                            break;
                        }

                        System.out.println("Inserisci la descrizione dell'asta: \n");
                        String descrizione = br.readLine();

                        System.out.println("Inserisci il prezzo di riserva Es:10.90 o premi invio per impostarlo a 0\n");
                        in = br.readLine();
                        double prezzo;
                        if (in.isEmpty())
                            prezzo = 0;
                        else
                            prezzo = Double.parseDouble(in);

                        System.out.println("Inserisci la data di termine dell'asta a partire da quella di domani [gg/mm/yyyy]: \n");
                        in = br.readLine();
                        if (in.isEmpty()) {
                            System.out.println("\nLa data di termine di un'asta è obbligatoria\n");
                            break;
                        }
                        Date data = formatoData.parse(in);
                        if (peer.createAuction(nome, data, prezzo, descrizione))
                            System.out.println("\nAsta creata con successo\n");
                        else
                            System.out.println("\nErrore durante la creazione dell'asta\n");
                        break;

                    case 2: //elenca le aste che hai creato
                        ArrayList<Auction> lista = peer.getAsteCreate();
                        if (lista.isEmpty())
                            System.out.println("\nNon hai creato alcun'asta finora\n");
                        else {
                            System.out.println("Ecco la lista di aste che hai creato:\n");
                            for (Auction a : lista)
                                System.out.println(a + "\n");
                        }
                        break;

                    case 3: //modifica un'asta
                        System.out.println("Inserisci il nome dell'asta che vuoi modificare: \n");
                        nome = br.readLine();
                        if (nome.isEmpty()) {
                            System.out.println("\nNessun nome inserito\n");
                            break;
                        }
                        if (peer.localSearch(nome) != null) { //sono il proprietario dell'asta quindi posso modificarla
                            Auction myauction = peer.localSearch(nome);
                            if (myauction != null) { //sono il proprietario dell'asta quindi posso modificarla
                                Auction asta = peer.globalSearch(nome);
                                if (asta != null) {
                                    System.out.println("Hai chiesto di modificare quest'asta:\n\n" + asta +
                                            "\n\nTi ricordo che non puoi modificare il nome dell'asta ma solo gli altri valori.\n\n" +
                                            "Inserisci la nuova descrizione o premi invio per lasciarla invariata:\n");
                                    in = br.readLine();
                                    if (!in.isEmpty())
                                        asta.setDescription(in);

                                    System.out.println("Inserisci il nuovo prezzo di riserva o premi invio per lasciarlo invariato:\n");
                                    in = br.readLine();
                                    if (!in.isEmpty())
                                        asta.setRiserva(Double.parseDouble(in));

                                    System.out.println("Inserisci la data aggiornata di termine dell'asta [gg/mm/yyyy] o premi invio per lasciarlo invariato:\n");
                                    in = br.readLine();
                                    if (!in.isEmpty())
                                        asta.setEndTime(formatoData.parse(in));
                                    //aggiorna l'asta con i nuovi campi
                                    if (peer.updateAuction(asta))
                                        System.out.println("\nAsta aggiornata con successo\n");
                                    else
                                        System.out.println("\nErrore nell'aggiornamento dell'asta\n");
                                } else {
                                    System.out.println("\nAsta non trovata\n");
                                }
                            } else {
                                System.out.println("\nErrore: Puoi modificare solo le aste che hai creato\n");
                            }
                        }
                        break;

                    case 4: //elimina un'asta
                        System.out.println("Inserisci il nome dell'asta da eliminare: \n");
                        nome = br.readLine();
                        if (nome.isEmpty()) {
                            System.out.println("\nNessun nome inserito\n");
                            break;
                        }
                        if (peer.removeAuction(nome))
                            System.out.println("\nAsta eliminata con successo\n");
                        else
                            System.out.println("\nErrore durante l'eliminazione dell'asta\n");
                        break;

                    case 5: //follow dell'asta
                        System.out.println("Inserisci il nome dell'asta su cui vuoi restare aggiornato: \n");
                        nome = br.readLine();
                        if (nome.isEmpty()) {
                            System.out.println("Nessun nome inserito\n");
                            break;
                        }
                        if (peer.followAuction(nome))
                            System.out.println("\nOra segui l'asta indicata\n");
                        else
                            System.out.println("\nOperazione non riuscita\n");
                        break;

                    case 6: //mostra le aste seguite
                        ArrayList<String> following = peer.getAsteSeguite();
                        if (following.isEmpty())
                            System.out.println("\nNon segui alcun'asta\n");
                        else {
                            System.out.println("Ecco la lista di tutte le aste che segui:\n");
                            for (String nomeAsta : following)
                                System.out.println(nomeAsta + "\n");
                        }
                        break;

                    case 7: //unfollow dell'asta
                        System.out.println("Inserisci il nome dell'asta di cui non vuoi più ricevere notifiche: \n");
                        nome = br.readLine();
                        if (nome.isEmpty()) {
                            System.out.println("\nNessun nome inserito\n");
                            break;
                        }
                        if (peer.unfollowAuction(nome))
                            System.out.println("\nHai abbandonato l'asta\n");
                        else
                            System.out.println("\nOperazione non riuscita\n");
                        break;

                    case 8: //fai una puntata
                        System.out.println("Inserisci il nome dell'asta su cui vuoi puntare: \n");
                        nome = br.readLine();
                        if (nome.isEmpty()) {
                            System.out.println("\nNessun nome inserito\n");
                            break;
                        }
                        System.out.println("Inserisci il valore del'offerta Es:10.90 \n");
                        in = br.readLine();
                        if (in.isEmpty()) {
                            System.out.println("\nNessuna puntata inserita\n");
                            break;
                        }
                        double puntata = Double.parseDouble(in);

                        //se la puntata è stata fatta su un asta aperta
                        String stato = peer.placeAbid(nome, puntata);
                        if (stato != null) {
                            if (Status.aperta.toString().compareTo(stato) == 0)
                                System.out.println("\nPuntata effettuata\n");
                            else
                                System.out.println("\nPuntata non effettuata\n");
                        }
                        break;

                    case 9: //stato di un'asta
                        System.out.println("Inserisci il nome dell'asta di cui vuoi controllare lo stato: \n");
                        nome = br.readLine();
                        if (nome.isEmpty()) {
                            System.out.println("\nNessun nome inserito\n");
                            break;
                        }

                        String status = peer.checkAuction(nome);
                        if (status == null)
                            System.out.println("\nAsta inesistente\n");
                        else
                            System.out.println("\nL'asta '" + nome + "' è " + status);
                        break;

                    case 10: //exit
                        peer.leaveNetwork();
                        System.out.println("\nRete abbandonata, termino il programma.\n");
                        System.exit(0);

                    default:
                        System.out.println("\nValore errato, inserire un numero corretto\n");
                        break;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
