public class App {
    private static Thread thread;

    public static void main( String[] args ) throws Exception {
        thread = new Thread( new JanelaOpenGL() );
        thread.start();
    }

    public static void close() {
        thread.interrupt();
    }
}
