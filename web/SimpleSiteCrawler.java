package wsa.web;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class SimpleSiteCrawler implements SiteCrawler{
    private volatile URI dom;
    private final Path dir;
    private volatile Set<URI> succDownload;
    private volatile Set<URI> toDownload;
    private volatile Set<URI> failDownload;
    private final Crawler crawler;
    private final Predicate<URI> pageLink;
    private volatile Thread crawlingThread;
    private volatile ConcurrentLinkedQueue<CrawlerResult> results;

    public SimpleSiteCrawler(URI dom, Path dir) throws IllegalArgumentException,IOException{
        if(dom==null && dir==null)
            throw new IllegalArgumentException();
        if(dom!=null) {
            if (checkDomain(dom))
                this.dom = dom;
            else
                throw new IllegalArgumentException();
        }
        else {
            if(dir!=null){
                if(Files.isDirectory(dir)) {
                    Path file=dir.resolve("downloadData.dat");
                    try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
                        Object[] state = (Object[]) input.readObject();
                        this.dom = (URI) state[0];
                        succDownload = (Set<URI>) state[1];
                        toDownload = (Set<URI>) state[2];
                        failDownload = (Set<URI>) state[3];
                        results = (ConcurrentLinkedQueue<CrawlerResult>) state[4];
                    } catch (ClassNotFoundException e) {
                        throw new IllegalArgumentException();
                    }
                }
                else
                    throw new IllegalArgumentException();
            }
        }
        this.dir=dir;
        succDownload=new HashSet<>();
        toDownload=new HashSet<>();
        failDownload=new HashSet<>();
        pageLink=(URI u)->checkSeed(this.dom,u);
        crawler=WebFactory.getCrawler(succDownload,toDownload,failDownload,pageLink);
        results=new ConcurrentLinkedQueue<>();
    }

    /** Controlla se l'URI specificato è un dominio. È un dominio se è un URI
     * assoluto gerarchico in cui la parte authority consiste solamente
     * nell'host (che può essere vuoto), ci può essere il path ma non ci
     * possono essere query e fragment.
     * @param dom  un URI
     * @return true se l'URI specificato è un dominio */
    static boolean checkDomain(URI dom) {
        return dom.getAuthority().equals(dom.getHost()) && dom.getFragment()==null && dom.getQuery()==null;
    }

    /** Controlla se l'URI seed appartiene al dominio dom. Si assume che dom
     * sia un dominio valido. Quindi ritorna true se dom.equals(seed) or not
     * dom.relativize(seed).equals(seed).
     * @param dom  un dominio
     * @param seed  un URI
     * @return true se seed appartiene al dominio dom */
    static boolean checkSeed(URI dom, URI seed) {
        return dom.equals(seed) || !(dom.relativize(seed).equals(seed));
    }

    /**
     * Aggiunge un seed URI. Se però è presente tra quelli già scaricati,
     * quelli ancora da scaricare o quelli che sono andati in errore,
     * l'aggiunta non ha nessun effetto. Se invece è un nuovo URI, è aggiunto
     * all'insieme di quelli da scaricare.
     *
     * @param uri un URI
     * @throws IllegalArgumentException se uri non appartiene al dominio di
     *                                  questo SuteCrawlerrawler
     * @throws IllegalStateException    se il SiteCrawler è cancellato
     */
    @Override
    public void addSeed(URI uri) throws IllegalArgumentException,IllegalStateException{
        if(!isCancelled()){
            if(pageLink.test(uri)){
                succDownload=crawler.getLoaded();
                toDownload=crawler.getToLoad();
                failDownload=crawler.getErrors();

                Set<URI> allURIs=new HashSet<>(succDownload);
                allURIs.addAll(toDownload);
                allURIs.addAll(failDownload);
                if(!allURIs.contains(uri)) {
                    toDownload.add(uri);
                    crawler.getToLoad().add(uri);
                }
            }
            else
                throw new IllegalArgumentException();
        }
        else
            throw new IllegalStateException();
    }

    /**
     * Inizia l'esecuzione del SiteCrawler se non è già in esecuzione e ci sono
     * URI da scaricare, altrimenti l'invocazione è ignorata. Quando è in
     * esecuzione il metodo isRunning ritorna true.
     *
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public void start() throws IllegalStateException{
        if(!isCancelled()){
            if(!crawler.isRunning()) {
                crawlingThread = new Thread(() -> {
                    addSeed(dom);
                    crawler.start();
                    while (!crawler.isCancelled()) {
                        Optional<CrawlerResult> var = crawler.get();
                        while (var == null || !var.isPresent())
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {}
                        results.add(var.get());

                        succDownload = crawler.getLoaded();
                        toDownload = crawler.getToLoad();
                        failDownload = crawler.getErrors();

                        if(crawlingThread.isInterrupted())
                            crawler.suspend();
                    }
                });
                crawlingThread.setDaemon(true);
                crawlingThread.start();
            }
        }
        else
            throw new IllegalStateException();
    }

    /**
     * Sospende l'esecuzione del SiteCrawler. Se non è in esecuzione, ignora
     * l'invocazione. L'esecuzione può essere ripresa invocando start. Durante
     * la sospensione l'attività dovrebbe essere ridotta al minimo possibile
     * (eventuali thread dovrebbero essere terminati). Se è stata specificata
     * una directory per l'archiviazione, lo stato del crawling è archiviato.
     *
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public void suspend() throws IllegalStateException{
        if(!isCancelled()){
            if(crawler.isRunning()){
                crawlingThread.interrupt();
                if(dir!=null){
                    Path file=dir.resolve("downloadData.dat");
                    try(ObjectOutputStream output=new ObjectOutputStream(Files.newOutputStream(file))){
                        Object[] state={dom,succDownload,toDownload,failDownload,results};
                        output.writeObject(state);
                    }catch(IOException e){
                        System.out.println("Errore I/O");
                    }
                }
            }
        }
        else
            throw new IllegalStateException();
    }

    /**
     * Cancella il SiteCrawler per sempre. Dopo questa invocazione il
     * SiteCrawler non può più essere usato. Tutte le risorse sono
     * rilasciate.
     */
    @Override
    public void cancel() {
        suspend();
        crawler.cancel();
    }

    /**
     * Ritorna il risultato relativo al prossimo URI. Se il SiteCrawler non è
     * in esecuzione, ritorna un Optional vuoto. Non è bloccante, ritorna
     * immediatamente anche se il prossimo risultato non è ancora pronto.
     *
     * @return il risultato relativo al prossimo URI scaricato
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public Optional<CrawlerResult> get() throws IllegalStateException{
        if(!crawler.isCancelled()){
            if(crawler.isRunning()) {
                while (results.isEmpty())
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {}
                return Optional.of(results.poll());
            }
            else
                return Optional.empty();
        }
        else
            throw new IllegalStateException();
    }

    /**
     * Ritorna il risultato del tentativo di scaricare la pagina che
     * corrisponde all'URI dato.
     *
     * @param uri un URI
     * @return il risultato del tentativo di scaricare la pagina
     * @throws IllegalArgumentException se uri non è nell'insieme degli URI
     *                                  scaricati né nell'insieme degli URI che hanno prodotto errori.
     * @throws IllegalStateException    se il SiteCrawler è cancellato
     */
    @Override
    public CrawlerResult get(URI uri) throws IllegalArgumentException,IllegalStateException{
        if(!crawler.isCancelled()){
            succDownload = crawler.getLoaded();
            toDownload = crawler.getToLoad();
            failDownload = crawler.getErrors();

            Set<URI> partialURIs=new HashSet<>(succDownload);
            partialURIs.addAll(failDownload);
            if(!partialURIs.contains(uri)){
                Object[] state={crawler.getLoaded(),crawler.getToLoad(),crawler.getErrors()};
                crawler.suspend();
                crawler.getLoaded().clear();
                crawler.getToLoad().clear();
                crawler.getErrors().clear();

                crawler.add(uri);
                crawler.start();
                while(crawler.get().get().uri==null)
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {}
                Optional<CrawlerResult> res=crawler.get();
                crawler.suspend();

                crawler.getLoaded().addAll((Set<URI>)state[0]);
                crawler.getToLoad().addAll((Set<URI>)state[1]);
                crawler.getErrors().addAll((Set<URI>)state[2]);

                succDownload = crawler.getLoaded();
                toDownload = crawler.getToLoad();
                failDownload = crawler.getErrors();
                crawler.start();

                return res.get();
            }
            else
                throw new IllegalArgumentException();
        }
        else
            throw new IllegalStateException();
    }

    /**
     * Ritorna l'insieme di tutti gli URI scaricati, possibilmente vuoto.
     *
     * @return l'insieme di tutti gli URI scaricati (mai null)
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public Set<URI> getLoaded() throws IllegalStateException {
        if(!isCancelled())
            return succDownload;
        else
            throw new IllegalStateException();
    }

    /**
     * Ritorna l'insieme, possibilmente vuoto, degli URI che devono essere
     * ancora scaricati. Quando l'esecuzione del crawler termina normalmente
     * l'insieme è vuoto.
     *
     * @return l'insieme degli URI ancora da scaricare (mai null)
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public Set<URI> getToLoad() throws IllegalStateException{
        if(!isCancelled())
            return toDownload;
        else
            throw new IllegalStateException();
    }

    /**
     * Ritorna l'insieme, possibilmente vuoto, degli URI che non è stato
     * possibile scaricare a causa di errori.
     *
     * @return l'insieme degli URI che hanno prodotto errori (mai null)
     * @throws IllegalStateException se il SiteCrawler è cancellato
     */
    @Override
    public Set<URI> getErrors() throws IllegalStateException{
        if(!isCancelled())
            return failDownload;
        else
            throw new IllegalStateException();
    }

    /**
     * Ritorna true se il SiteCrawler è in esecuzione.
     *
     * @return true se il SiteCrawler è in esecuzione
     */
    @Override
    public boolean isRunning() {
        return crawler.isRunning();
    }

    /**
     * Ritorna true se il SiteCrawler è stato cancellato. In tal caso non può
     * più essere usato.
     *
     * @return true se il SiteCrawler è stato cancellato
     */
    @Override
    public boolean isCancelled() {
        return crawler.isCancelled();
    }
}
