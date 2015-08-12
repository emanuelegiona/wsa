/** Le definizioni (package, interfacce, classi, ecc.) sono le stesse di quelle del
 * progetto. Anche i javadoc sono gli stessi eccetto i javadoc di
 * {@link wsa.web.WebFactory} che specificano in maggiore dettaglio alcuni aspetti.
 * Seguire scrupolosamente le specifiche contenute nei javadoc. Tutte le implementazioni
 * dei metodi delle classi o interfacce non devono essere bloccanti (a casua di
 * connessioni remote) eccetto quelle dei due metodi di {@link wsa.web.Loader}.
 * <br>
 * Rispetto alla versione del progetto è stato anche aggiunto questo javadoc, una
 * precisazione nel javadoc di {@link wsa.web.SiteCrawler} e la classe {@link wsa.JFX}
 * come necessario ausilio per l'implementazione di default dei {@link wsa.web.Loader}.
 * <br>
 * <br>
 * Il testing per valutare l'homework sarà articolato nelle seguenti quattro fasi.
 * <ol>
 *     <li>
 *         Test dei {@link wsa.web.Loader} creati da {@link wsa.web.WebFactory#getLoader()}
 *         tramite l'implementazione di default. Questo comporta anche il test del
 *         risultati dei metodi di {@link wsa.web.Loader}, in particolare di
 *         {@link wsa.web.html.Parsed} e dei suoi metodi.
 *     </li>
 *     <li>
 *         Test dei {@link wsa.web.AsyncLoader} prodotti da
 *         {@link wsa.web.WebFactory#getAsyncLoader()} sia con l'implementazione di
 *         default sia con una factory per i Loader impostata con
 *         {@link wsa.web.WebFactory#setLoaderFactory(wsa.web.LoaderFactory)}. I test
 *         metterrano alla prova non solo il rispetto delle specifiche ma anche
 *         l'efficienza dell'implementazione che deve sfruttare la programmazione
 *         concorrente per velocizzare il più possibile il dowloading delle pagine.
 *         Attenzione però a non usare troppi thread o troppa memoria. Se il numero
 *         di thread (simultaneamente attivi) usati da un singolo loader asincrono
 *         supera 200 o se la memoria richiesta supera 1GB sarà considerato un errore.
 *     </li>
 *     <li>
 *         Test dei {@link wsa.web.Crawler} prodotti da
 *         {@link wsa.web.WebFactory#getCrawler(java.util.Collection, java.util.Collection, java.util.Collection, java.util.function.Predicate)}
 *         sia con l'implementazione di default sia con una factory per i Loader
 *         impostata con
 *         {@link wsa.web.WebFactory#setLoaderFactory(wsa.web.LoaderFactory)}. I test
 *         metterrano alla prova non solo il rispetto delle specifiche ma anche
 *         l'efficienza.
 *     </li>
 *     <li>
 *         Test dei {@link wsa.web.SiteCrawler} prodotti da
 *         {@link wsa.web.WebFactory#getSiteCrawler(java.net.URI, java.nio.file.Path)}
 *         sia con l'implementazione di default sia con una factory per i Loader
 *         impostata con
 *         {@link wsa.web.WebFactory#setLoaderFactory(wsa.web.LoaderFactory)}. I test
 *         metterrano alla prova non solo il rispetto delle specifiche ma anche
 *         l'efficienza. In particolare si metteranno alla prova le funzionalità relative
 *         all'archiviazione.
 *     </li>
 * </ol>
 * Si tenga inoltre presente che il testing riguarderà URI con una varietà di schemi
 * diversi, http://, https://, file://, ecc. Ognuna delle quattro fasi produrrà un
 * punteggio proprio. Però dalla seconda in poi ogni fase dipende ovviamente da quelle
 * precedenti. Questo significa che se si vuole si può consegnare un'implementazione
 * parziale relativa solamente alle prime k fasi (con k = 1, 2 o 3).
 * <br>
 * <br>
 * Si possono introdurre nuovi package, interfacce e classi ma devono essere
 * all'interno del package {@link wsa}. Non modificare in alcun modo le interfacce e
 * le intestazioni dei metodi forniti. Si può usare solamente la libreria Java,
 * nessuna libreria esterna è ammessa.
 * <br>
 * <br>
 * Si consiglia vivamente di scrivere dei test per ogni parte implementata.
 *
 * @version 18 Mag 2015 */
package wsa;