package wsa.web;

import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import org.w3c.dom.Document;
import wsa.JFX;
import wsa.web.html.ParsedHTML;
import java.net.URL;
import java.net.URLConnection;

public class SimpleLoader implements Loader{
    private volatile WebEngine engine;
    private volatile Document doc;
    private volatile Exception ex;
    private volatile boolean done;

    public SimpleLoader(){
        JFX.exec(()->{
            try {
                if (engine == null)
                    engine = new WebEngine();
                engine.getLoadWorker().stateProperty().addListener((o, ov, nv) -> {
                    if (nv == Worker.State.SUCCEEDED) {
                        doc = engine.getDocument();
                        done=true;
                        ex=null;
                    }
                    else if (nv == Worker.State.FAILED) {
                        done=true;
                        ex = new Exception("Errore durante il download");
                    }
                    else if (nv == Worker.State.CANCELLED) {
                        done=true;
                        ex = new Exception("Download annullato");
                    }
                });
            }catch(Exception e){
                ex=e;
            }
        });
    }
    /**
     * Ritorna il risultato del tentativo di scaricare la pagina specificata. È
     * bloccante, finchè l'operazione non è conclusa non ritorna.
     *
     * @param url l'URL di una pagina web
     * @return il risultato del tentativo di scaricare la pagina
     */
    @Override
    public LoadResult load(URL url) {
        ParsedHTML parsed=null;
        //caricamento di una pagina vuota (reset della WebEngine)
        JFX.exec(() -> {
            try {
                done=false;
                doc=null;
                engine.load("");
            } catch (Exception e) {}
        });
        while (!done)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }

        JFX.exec(() -> {
            try {
                done=false;
                doc=null;
                engine.load(url.toString());
            } catch (Exception e) {
                ex=e;
            }
        });
        while (!done)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }

        if(doc!=null)
            parsed=new ParsedHTML(doc);
        else
            ex=new Exception("Errore durante il download");
        return new LoadResult(url,parsed,ex);
    }

    /**
     * Ritorna null se l'URL è scaricabile senza errori, altrimenti ritorna
     * un'eccezione che riporta l'errore.
     *
     * @param url un URL
     * @return null se l'URL è scaricabile senza errori, altrimenti
     * l'eccezione
     */
    @Override
    public Exception check(URL url) {
        Exception ex=null;
        try{
            URLConnection conn=url.openConnection();
            conn.setRequestProperty("User-Agent","Mozilla/5.0");
            conn.setRequestProperty("Accept", "text/html;q=1.0,*;q=0");
            conn.setRequestProperty("Accept-Encoding", "identity;q=1.0,*;q=0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.connect();
        }catch(Exception e){
            ex=e;
        }
        return ex;
    }
}
