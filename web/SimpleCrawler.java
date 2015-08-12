package wsa.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class SimpleCrawler implements Crawler{
    private volatile Set<URI> succDownload;
    private volatile Set<URI> toDownload;
    private volatile Set<URI> failDownload;
    private final Predicate<URI> rule;
    private volatile AsyncLoader loader;
    private volatile boolean running;
    private volatile ConcurrentLinkedQueue<Future<LoadResult>> tasks;
    private volatile ConcurrentLinkedQueue<CrawlerResult> results;
    private volatile Thread downloadThread;

    public SimpleCrawler(Collection<URI> succDownload, Collection<URI> toDownload, Collection<URI> failDownload, Predicate<URI> rule) {
        this.succDownload = new HashSet<>(succDownload);
        this.toDownload = new HashSet<>(toDownload);
        this.failDownload = new HashSet<>(failDownload);
        if(rule!=null)
            this.rule=rule;
        else
            this.rule=(s)->true;
        loader=WebFactory.getAsyncLoader();
        running=false;
        tasks=new ConcurrentLinkedQueue<>();
        results=new ConcurrentLinkedQueue<>();
    }

    /**
     * Aggiunge un URI all'insieme degli URI da scaricare. Se però è presente
     * tra quelli già scaricati, quelli ancora da scaricare o quelli che sono
     * andati in errore, l'aggiunta non ha nessun effetto. Se invece è un nuovo
     * URI, è aggiunto all'insieme di quelli da scaricare.
     *
     * @param uri un URI che si vuole scaricare
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public void add(URI uri) throws IllegalStateException{
        if(!loader.isShutdown()){
            Set<URI> allURIs = new HashSet<>(succDownload);
            allURIs.addAll(toDownload);
            allURIs.addAll(failDownload);
            if(!allURIs.contains(uri))
                toDownload.add(uri);
        }
        else
            throw new IllegalStateException();
    }

    /**
     * Inizia l'esecuzione del Crawler se non è già in esecuzione e ci sono URI
     * da scaricare, altrimenti l'invocazione è ignorata. Quando è in esecuzione
     * il metodo isRunning ritorna true.
     *
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public void start() throws IllegalStateException{
        if (!loader.isShutdown()) {
            if(!running) {
                running = true;
                downloadThread = new Thread(() -> {
                    while (running && toDownload.size() > 0) {
                        Iterator<URI> uris = toDownload.iterator();
                        while (uris.hasNext()) {
                            URI u = uris.next();
                            boolean tested = rule.test(u);
                            Exception ex = null;
                            try {
                                URL url = u.toURL();
                                tasks.add(loader.submit(url));
                            } catch (Exception e) {
                                ex = e;
                                results.add(new CrawlerResult(u, tested, null, null, ex));
                                uris.remove();
                                toDownload.remove(u);
                                failDownload.add(u);
                            }
                        }

                        while (!tasks.isEmpty()) {
                            Future<LoadResult> t = tasks.poll();
                            LoadResult res = null;
                            URI u = null;
                            boolean tested = false;
                            Exception ex = null;
                            try {
                                res = t.get(2000, TimeUnit.MILLISECONDS);
                                if (res.exc == null) {
                                    u = res.url.toURI();
                                    tested = rule.test(u);
                                    ex = res.exc;

                                    List<String> resLinks = res.parsed.getLinks();
                                    List<String> failLinks = new ArrayList<>(resLinks);
                                    List<URI> absLinks = new ArrayList<>();

                                    Iterator<String> links = resLinks.iterator();
                                    if (tested) {
                                        while (links.hasNext()) {
                                            try {
                                                String s = links.next();
                                                URI var = URI.create(s);
                                                URI newURI = u.resolve(var);
                                                URL url = newURI.toURL();
                                                absLinks.add(newURI);
                                                links.remove();
                                                failLinks.remove(s);
                                                add(newURI);
                                            } catch (Exception e) {
                                                System.out.println(u);
                                                ex = e;
                                                links.remove();
                                                toDownload.remove(u);
                                                failDownload.add(u);
                                            }
                                        }
                                    }

                                    if (toDownload.remove(u))
                                        succDownload.add(u);

                                    results.add(new CrawlerResult(u, tested, absLinks, failLinks, ex));
                                }
                            } catch (URISyntaxException e) {
                                ex = e;
                                results.add(new CrawlerResult(u, tested, null, null, ex));
                            } catch (TimeoutException | ExecutionException | InterruptedException e) {
                                tasks.add(t);
                            }
                        }
                    }
                });
                downloadThread.setDaemon(true);
                downloadThread.start();
            }
        } else
            throw new IllegalStateException();
    }

    /**
     * Sospende l'esecuzione del Crawler. Se non è in esecuzione, ignora
     * l'invocazione. L'esecuzione può essere ripresa invocando start. Durante
     * la sospensione l'attività del Crawler dovrebbe essere ridotta al minimo
     * possibile (eventuali thread dovrebbero essere terminati).
     *
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public void suspend() throws IllegalStateException{
        if(!loader.isShutdown())
            if(running) {
                running = false;
                downloadThread.interrupt();
            }
        else
            throw new IllegalStateException();
    }

    /**
     * Cancella il Crawler per sempre. Dopo questa invocazione il Crawler non
     * può più essere usato. Tutte le risorse devono essere rilasciate.
     */
    @Override
    public void cancel() {
        suspend();
        loader.shutdown();
    }

    /**
     * Ritorna il risultato relativo al prossimo URI. Se il Crawler non è in
     * esecuzione, ritorna un Optional vuoto. Non è bloccante, ritorna
     * immediatamente anche se il prossimo risultato non è ancora pronto.
     *
     * @return il risultato relativo al prossimo URI scaricato
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public Optional<CrawlerResult> get() throws IllegalStateException{
        if(!loader.isShutdown()){
            if(running) {
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
     * Ritorna l'insieme di tutti gli URI scaricati, possibilmente vuoto.
     *
     * @return l'insieme di tutti gli URI scaricati (mai null)
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public Set<URI> getLoaded() throws IllegalStateException{
        if(!loader.isShutdown())
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
     * @throws IllegalStateException se il Crawler è cancellato
     */
    @Override
    public Set<URI> getToLoad() throws IllegalStateException{
        if(!loader.isShutdown())
            return toDownload;
        else
            throw new IllegalStateException();
    }

    /**
     * Ritorna l'insieme, possibilmente vuoto, degli URI che non è stato
     * possibile scaricare a causa di errori.
     *
     * @return l'insieme degli URI che hanno prodotto errori (mai null)
     * @throws IllegalStateException se il crawler è cancellato
     */
    @Override
    public Set<URI> getErrors() throws IllegalStateException{
        if(!loader.isShutdown())
            return failDownload;
        else
            throw new IllegalStateException();
    }

    /**
     * Ritorna true se il Crawler è in esecuzione.
     *
     * @return true se il Crawler è in esecuzione
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Ritorna true se il Crawler è stato cancellato. In tal caso non può più
     * essere usato.
     *
     * @return true se il Crawler è stato cancellato
     */
    @Override
    public boolean isCancelled() {
        return loader.isShutdown();
    }
}
