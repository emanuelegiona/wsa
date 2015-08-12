package wsa;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/** Classe di utilità per eseguire codice nel JavaFX Application Thread. Il metodo
 * {@link wsa.JFX#exec(Runnable)} permette di usare classi e metodi della libreria
 * JavaFX anche in un programma che non è un'applicazione JavaFX. I metodi di questa
 * classe possono essere usati anche in un'applicazione JavaFX. */
public class JFX {
    /** Esegue l'azione act nel JavaFX Application Thread. Se tale thread non è già
     * attivo, lancia nel background una JavaFX application. Il thread rimane attivo
     * finché non si invoca il metodo {@link JFX#exit()}. Quindi se si invoca questo
     * metodo, per far terminare il programma sarà necessario invocare il metodo
     * {@link JFX#exit()}.
     * @param act  l'azione da eseguire nel JavaFX Application Thread */
    public static synchronized void exec(Runnable act) {
        if (terminated)
            throw new IllegalStateException("JavaFX Application ia terminated");
        if (lastThread == null) {
            Thread t = new Thread(() -> {
                try {
                    Application.launch(App.class);
                } catch (Exception ex) {}});
            t.setDaemon(true);
            t.start();
        }
        lastThread = Thread.currentThread();
        action = act;
        while (action != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { break; }
        }
    }

    /** Fa terminare il JavaFX Application Thread. Se si è invocato il metodo
     * {@link wsa.JFX#exec(Runnable)}, il programma non potrà terminare fino a che
     * non si invoca questo metodo. */
    public static void exit() {
        exitAppOnThreadDeath = true;
    }


    private static volatile boolean exitAppOnThreadDeath = false, terminated = false;
    private static volatile Thread lastThread = null;
    private static volatile Runnable action = null;
    private static final Runnable executor = new Runnable() {
        @Override
        public void run() {
            if (action != null) action.run();
            action = null;
            if (exitAppOnThreadDeath && lastThread != null && !lastThread.isAlive()) {
                terminated = true;
                Platform.exit();
            } else
                Platform.runLater(this);
        }
    };

    public static class App extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            Platform.setImplicitExit(true);
            Platform.runLater(executor);
        }
    }
}
