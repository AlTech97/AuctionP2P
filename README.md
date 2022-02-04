# AuctionP2P
Un meccanismo d'asta second-price su rete P2P realizzato con la libreria TomP2P, Docker e Maven

## Indice dei contenuti
0. [Selezione dell'homework](#0-selezione-dellhomework)
1. [Introduzione](#1-introduzione)
2. [Implementazione](#2-implementazione)
3. [Test di unità](#3-test-di-unit)
4. [Esecuzione con Docker](#4-esecuzione-con-docker)

## 0. Selezione dell'homework
***
**Autore:** *Alfonso Ingenito*

**Stringa MD5:** md5(alfonsoingenito-21) = c456cb20f4797a52b86ae87e9e3ce873

**Homework corrispondente:** Homework 3: Auction Mechanism

## 1. Introduzione
***
Questo progetto ha avuto come scopo quello di realizzare un’asta second-price. Un’asta second-price è un modello per cui ogni offerente fa un’offerta su un prodotto messo in vendita ma, 
soltanto chi effettuerà l’offerta più alta potrà aggiudicarsi tale prodotto pagando però il prezzo offerto dal secondo miglior offerente, con valore inferiore.


## 2. Implementazione
***
La caratteristica principale di questa applicazione è che si basa su una rete peer-to-peer e sfrutta le DHT (Distributed Hash Table) oltre allo scambio di messaggi tra peer ed entrambe le funzionalità sono messe a disposizione dalla libreria Java “TomP2P”. 
L’applicazione inoltre adopera Apache Maven per gestire le dipendenze e Docker per l’esecuzione dell’applicazione su diversi container per simulare la rete P2P.


### Funzionalità realizzate
In dettaglio, l’applicazione permette ai client di connettersi e disconnettersi dalla rete, effettuare operazioni CRUD sulle aste presenti in DHT, 
effettuare puntate su un’asta aperta, seguire un’asta per ricevere tutti gli aggiornamenti riguardo: 
le modifiche delle informazioni dell’asta (descrizione, data di termine, prezzo di riserva), puntate effettuate e terminazione della stessa.


### Struttura della soluzione
Nella cartella più esterna sono situati il file pom.xml che contiene le dipendenze del progetto utili a Maven all’avvio dell’applicazione, 
il Dockerfile che consente la creazione di un’immagine personalizzata a partire da alpine che include le dipendenze scaricate da maven e il progetto clonato da Git. 
Mostreremo la procedura di creazione dell'immagine (build) e avvio dell'applicazione (run) successivamente, in un capitolo a parte.

L’implementazione è suddivisa in due package: “pack” e “testPack”. Il primo, “pack”, contiene tutta la logica di funzionamento del sistema. 
Partendo dalla base troviamo le classi Auction, Bid e Message che rispettivamente identificano l’asta, la puntata ad un’asta e il messaggio che si invia ad un altro peer. 
Queste tre classi vengono adoperate per contenere e restituire informazioni. 
Legata alla classe Asta abbiamo l’enumerazione Status che definisce due stati consentiti per un’asta: “aperta” o “chiusa”. 
Anche la classe Message detiene un’enumerazione al suo interno che permette di differenziare la tipologia di messaggio tra: 
“feed”, il messaggio di aggiornamento inviato a tutti i peer che seguono un’asta, “bid” se il messaggio contiene una puntata ad un’asta, 
“victory” se il messaggio contiene una frase riguardo la vittoria ad un’asta, “dhtUpdate” se contiene l’asta appena chiusa che, inviata al proprietario, provvederà ad aggiornare la DHT. 
Quest’ultima è fondamentale in quanto si concede unicamente al proprietario di un’asta di modificare i campi della dht relativi alle proprie aste.
Proseguendo la descrizione, ritroviamo la classe AuctionMechanism che implementa i metodi definiti nell’interfaccia AuctionMechanismInterface per effettuare tutte le operazioni CRUD sulla DHT, 
lo scambio di messaggi attraverso i metodi di invio e il MessageListener come classe interna per la ricezione, ed infine altre funzioni come il follow e l’unfollow di un’asta.
Nella classe main, invece, viene instanziato un solo oggetto AuctionMechanism così da permettere l’esecuzione di un solo peer per macchina, 
dato che ad ogni istanza di AuctionMechanism è associata l'istanza di un peer al suo interno. 
La classe main stampa un menù a riga di comando e permette di effettuare tutte le operazioni elencate precedentemente.
Il secondo, “testPack”, contiene unicamente la classe JunitTestAuction per i test unitari che approfondiremo successivamente


### Dettagli implementativi
Tutte le operazioni che descriveremo in questa sezione sono effettuate dai metodi nella classe AuctionMechanism.

Il costruttore della classe permette di inizializzare il peer con id univoco, definire la DHT e di creare un'istanza di MessageListener, 
utilizzato per chiamare il metodo parseMessage ogni qualvolta il peer riceve un messaggio.

Per descrivere al meglio le informazioni contenute nella DHT porrò l’esempio della creazione di una nuova asta.
All’interno della DHT è presente una lista identificata dalla chiave “auctionList” unica per tutte le aste esistenti, che siano esse aperte o chiuse, contenente i loro nomi univoci. 
Questa lista permette una rapida ricerca utile a diverse operazioni.
Nel momento in cui viene creata una nuova asta, se questa non è già in lista allora si carica in DHT l’oggetto utilizzando come chiave il suo nome univoco. 
Tale oggetto conterrà tutte le informazioni immesse dall’utente. Infine, per ogni oggetto Auction, sempre in DHT, 
viene detenuta una lista dei followers dell’asta (un HashSet\<PeerAddress>) alla quale si aggiungeranno i peer che decideranno di rimanere aggiornati su tutti gli avvenimenti legati all’asta in questione. 
Questa lista di followers all'interno della DHT avrà come chiave la stringa ottenuta dalla concatenazione del nome dell’asta e la stringa “Followers”.

Per quanto riguarda l’invio di messaggi tra peer, questa funzionalità viene adoperata per le seguenti operazioni: 
* L’invio di un feed, contenente le informazioni aggiornate di un asta a tutti i peer iscritti (metodo sendFeedMessage) in caso di modifica o chiusura 
* L’esecuzione di una puntata ad un'asta da parte di un peer che invia al proprietario la sua offerta unita al suo indirizzo di contatto in caso di vittoria (metodo placeAbid) 
* L’invio di un messaggio di congratulazioni alla chiusura di un’asta al peer che ha effettuato l’offerta maggiore 
* Infine, in concomitanza con quello precedente, l’invio di un messaggio di chiusura dell’asta al proprietario nel momento in cui un peer (diverso dal proprietario stesso) si accorge che è scaduto il tempo limite (metodo declareTheWinner). 
Quest’ultimo messaggio si rende necessario siccome il proprietario di un’asta non controlla di continuo se le proprie aste sono terminate, 
allora qualsiasi peer che controlla lo stato di un’asta (con il metodo checkAuction), prima di svolgere qualsiasi operazione su di essa, 
esegue implicitamente anche un controllo sul tempo limite dell’asta e nel caso questo sia scaduto avverte il proprietario dell’asta di quest’evento per chiudere l’asta e decretare il vincitore.

All’invio dei messaggi all’interno dei metodi appena citati corrisponde la ricezione da parte di un altro peer gestita con il metodo parseMessage della classe interna MessageListener il quale, 
in base alla tipologia di messaggio ricevuto effettua l’operazione prestabilita:
* Nel caso di un messaggio di feed o di vittoria (congratulazioni) si stampa semplicemente il contenuto di questo
* Se si riceve un messaggio di Bid si accede ai due valori relativi alle offerte più alte ricevute finora e se l'offerta dovesse superare uno di questi due a partire dal primo si sostituisce il campo di tipo Bid (contenente nome dell'asta, indirizzo del bidder e valore della puntata) avente importo minore con quello di valore più alto
* Se il proprietario di un'asta riceve un messaggio di tipo "dhtUpdate" significa che qualche altro peer ha appena notato che l'asta è scaduta,
quindi ha modificato il campo status localmente e ha inviato l'intero oggetto all'owner dell'asta che provvederà ad aggiornarlo in DHT

## 3. Test di unità
***
Per l’esecuzione dei test è stata realizzata la classe “JunitTestAuction” nel package “testPack” la quale utilizza quattro peer per simulare situazioni reali di utilizzo del sistema in ogni sua funzione, 
controllando sia che vengano generati errori in caso di operazioni non consentite (correlate al lancio di un'eccezzione) e sia che tutto vada per il verso giusto nelle normali operazioni. 
A tal proposito elenchiamo i differenti metodi di test realizzati:

* testCaseGeneratePeers() è il metodo che viene eseguito per una sola volta e prima di tutti gli altri test avendo il tag @BeforeAll ed inizializza i quattro peer utilizzati negli altri metodi
* leaveNetwork(): eseguito dopo tutti i test tramite il tag @AfterAll, permette l’abbandono della rete da parte di tutti i peer
* testCaseCreateAuction(): testa il metodo di creazione dell’asta mostrando il lancio delle eccezioni qualora un peer tenti di creare un’asta con un nome già utilizzato, 
di crearne una con il prezzo di riserva minore di zero e nel caso in cui la data di termine dell’asta sia errata (precedente alla data odierna)
* testCaseRemoveAuction(): testa il metodo di rimozione di un’asta tentando prima l’eliminazione con un peer diverso dal proprietario, che lancia l’eccezione, e poi da parte del proprietario stesso con successo.
* testCaseUpdateAuction(): in maniera simile al precedente si testa l’aggiornamento di un’asta, successivo ad una modifica del campo del prezzo di riserva, 
prima da parte di un peer qualsiasi con lancio d’eccezione e successivamente dal proprietario con successo.
* testCaseFollowUnfollowAuction(): testa due metodi relativi al follow e l’unfollow di un’asta mostrando la ricezione dei feed da parte di un peer che segue l’asta quando questa viene aggiornata e riceve un’offerta. 
Successivamente, quando il peer smette di seguire l’asta, si osserva la mancata ricezione del feed riguardo un altro aggiornamento.
* testCasePlaceAbid(): testa il metodo utilizzato dai peer per fare offerte su un’asta. Viene testato il fallimento della puntata su una propria asta e di una puntata con valore inferiore alla riserva mentre si ha successo quando un peer diverso dal proprietario fa un’offerta con valore maggiore alla soglia di riserva. 
Infine, si testa il valore restituito da un’offerta andata a buon fine su un’asta aperta.
* testCaseDeclareTheWinner(): questo test case simula la chiusura anticipata di un’asta attraverso la modifica del tempo di scadenza (dato che non avrebbe senso creare un’asta già scaduta, per cui ho inserito il lancio dell’eccezione). 
Dopo la modifica del tempo qualsiasi operazione si faccia sull’asta implica l’innesco della procedura di terminazione che invia il messaggio di congratulazioni al vincitore ed un altro al proprietario che chiude l’asta definitivamente.

## 4. Esecuzione con Docker
***