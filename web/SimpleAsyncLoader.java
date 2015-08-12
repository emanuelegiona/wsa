package wsa.web;

import java.net.URL;
import java.util.concurrent.*;

public class SimpleAsyncLoader implements AsyncLoader{
    private ConcurrentLinkedQueue<Loader> loaderPool;
    private ConcurrentLinkedQueue<Future<LoadResult>> tasks;
    private ExecutorService pool;
    private CompletionService<LoadResult> exec;

    public SimpleAsyncLoader(){
        loaderPool=new ConcurrentLinkedQueue<>();
        tasks=new ConcurrentLinkedQueue<>();
        int cpu=Runtime.getRuntime().availableProcessors();
        for(int i=0;i<cpu;i++)
            loaderPool.add(WebFactory.getLoader());
        pool=Executors.newFixedThreadPool(cpu,tf->{
            Thread t = new Thread(tf);
            t.setDaemon(true);
            return t;
        });
        exec=new ExecutorCompletionService<>(pool);
    }

    /**
     * Sottomette il downloading della pagina dello specificato URL e ritorna
     * un Future per ottenere il risultato in modo asincrono.
     *
     * @param url un URL di una pagina web
     * @return Future per ottenere il risultato in modo asincrono
     * @throws IllegalStateException se il loader è chiuso
     */
    @Override
    public Future<LoadResult> submit(URL url) throws IllegalStateException{
        Future<LoadResult> res=null;
        if(!pool.isShutdown()) {
                res=exec.submit(()->{
                    while(loaderPool.isEmpty())
                        try{
                            Thread.sleep(10);
                        }catch(InterruptedException e){}
                    Loader loader=loaderPool.poll();
                    LoadResult r=loader.load(url);
                    loaderPool.add(loader);
                    return r;
                });
                tasks.add(res);
            return tasks.poll();
        }
        else
            throw new IllegalStateException();
    }

    /**
     * Chiude il loader e rilascia tutte le risorse. Dopo di ciò non può più
     * essere usato.
     */
    @Override
    public void shutdown() {
        pool.shutdown();
    }

    /**
     * Ritorna true se è chiuso.
     *
     * @return true se è chiuso
     */
    @Override
    public boolean isShutdown() {
        return pool.isShutdown();
    }
}
