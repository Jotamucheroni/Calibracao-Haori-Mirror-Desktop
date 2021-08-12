public class Aplicativo {
    private static Thread thread;
    
    public static void main( String[] args ) throws Exception {
        thread = new Thread( new Janela() );
        thread.start();
    }
    
    public static void close() {
        thread.interrupt();
    }
}