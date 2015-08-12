package wsa.web;

import javafx.scene.web.WebEngine;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Predicate;

/** Una factory per oggetti che servono a scaricare pagine web. L'implementazione
 * dell'homework deve fornire l'implementazione di default per i {@link wsa.web.Loader}
 * che è specificata nel javadoc del metodo {@link WebFactory#getLoader()}. Inoltre
 * l'implementazione di {@link WebFactory#getAsyncLoader()} deve usare esclusivamente
 * {@link wsa.web.Loader} forniti da {@link WebFactory#getLoader()}, l'implementazione
 * di {@link wsa.web.WebFactory#getCrawler(Collection, Collection, Collection, Predicate)}
 * deve usare esclusivamente {@link wsa.web.AsyncLoader} fornito da
 * {@link WebFactory#getAsyncLoader()} e l'implementazione di
 * {@link wsa.web.WebFactory#getSiteCrawler(URI, Path)} deve usare esclusivamente
 * {@link wsa.web.Crawler} fornito da
 * {@link wsa.web.WebFactory#getCrawler(Collection, Collection, Collection, Predicate)}. */
public class WebFactory {
    private static LoaderFactory loadFactory=null;

    /** Imposta la factory per creare {@link wsa.web.Loader}. Dopo l'impostazione
     * della factory con questo metodo, il metodo {@link WebFactory#getLoader()} deve
     * creare nuovi {@link wsa.web.Loader} solamente tramite la factory impostata.
     * Inoltre, tutti gli altri metodi di questa classe devono funzionare correttamente
     * se i Loader forniti dalla factory impostata soddisfano le specifiche
     * dell'interfaccia {@link wsa.web.Loader}.
     * @param lf  factory per Loader */
    public static void setLoaderFactory(LoaderFactory lf) {
        loadFactory=lf;
    }

    /** Ritorna un nuovo {@link wsa.web.Loader}. Se non è stata impostata una factory
     * tramite il metodo {@link WebFactory#setLoaderFactory(LoaderFactory)}, il Loader
     * è creato tramite l'implementazione di default, altrimenti il Loader è creato
     * tramite la factory impostata con setLoaderFactory.
     * <br<
     * L'implementazione di default deve soddisfare i seguenti requisiti:
     * <ul>
     *     <li>
     *         Il metodo {@link wsa.web.Loader#load(URL)} deve scaricare la pagina
     *         tramite una {@link javafx.scene.web.WebEngine} (con il metodo
     *         {@link javafx.scene.web.WebEngine#load(String)}) e l'albero di parsing
     *         deve basarsi sul {@link org.w3c.dom.Document} ritornato da
     *         {@link WebEngine#getDocument()}. L'implementazione di default non deve
     *         assumere che sia all'interno di un'applicazione JavaFX e anzi deve
     *         funzionare in un qualsiasi programma. Pertanto per creare una
     *         {@link javafx.scene.web.WebEngine} e per invocare uno qualsiasi dei
     *         suoi metodi bisogna usare il metodo {@link wsa.JFX#exec(Runnable)}.
     *         In quanto una {@link javafx.scene.web.WebEngine} può essere manipolata
     *         solamente nel JavaFX Application Thread.
     *     <li>
     *         Il metodo {@link wsa.web.Loader#check(URL)} deve scaricare la pagina
     *         tramite {@link java.net.URLConnection}. Quindi non deve usare una
     *         {@link javafx.scene.web.WebEngine} per ragioni di efficienza, in quanto
     *         il parsing della pagina non è necessario.
     *     </li>
     * </ul>
     * @return un nuovo Loader */
    public static Loader getLoader() {
        return loadFactory==null?new SimpleLoader():loadFactory.newInstance();
    }

    /** Ritorna un nuovo loader asincrono che per scaricare le pagine usa
     * esclusivamente {@link wsa.web.Loader} forniti da {@link wsa.web.WebFactory#getLoader()}.
     * @return un nuovo loader asincrono. */
    public static AsyncLoader getAsyncLoader() {
        return new SimpleAsyncLoader();
    }

    /** Ritorna un {@link wsa.web.Crawler} che inizia con gli specificati insiemi di URI.
     * Per scaricare le pagine usa esclusivamente {@link wsa.web.AsyncLoader} fornito da
     * {@link WebFactory#getAsyncLoader()}.
     * @param loaded  insieme URI scaricati
     * @param toLoad  insieme URI da scaricare
     * @param errs  insieme URI con errori
     * @param pageLink  determina gli URI per i quali i link contenuti nelle
     *                  relative pagine sono usati per continuare il crawling
     * @return un Crawler con le proprietà specificate */
    public static Crawler getCrawler(Collection<URI> loaded, Collection<URI> toLoad, Collection<URI> errs, Predicate<URI> pageLink) {
        return new SimpleCrawler(loaded,toLoad,errs,pageLink);
    }

    /** Ritorna un {@link wsa.web.SiteCrawler}. Se dom e dir sono entrambi non null,
     * assume che sia un nuovo web site con dominio dom da archiviare nella directory
     * dir. Se dom non è null e dir è null, l'esplorazione del web site con dominio
     * dom sarà eseguita senza archiviazione. Se dom è null e dir non è null, assume
     * che l'esplorazione del web site sia già archiviata nella directory dir e la
     * apre. Per scaricare le pagine usa esclusivamente un {@link wsa.web.Crawler}
     * fornito da
     * {@link wsa.web.WebFactory#getCrawler(Collection, Collection, Collection, Predicate)}.
     * @param dom  un dominio o null
     * @param dir  un percorso di una directory o null
     * @throws IllegalArgumentException se dom e dir sono entrambi null o dom è
     * diverso da null e non è un dominio o dir è diverso da null non è una
     * directory o dom è null e dir non contiene l'archivio di un SiteCrawler.
     * @throws IOException se accade un errore durante l'accesso all'archivio
     * del SiteCrawler
     * @return un SiteCrawler */
    public static SiteCrawler getSiteCrawler(URI dom, Path dir) throws IOException {
        return new SimpleSiteCrawler(dom,dir);
    }
}
