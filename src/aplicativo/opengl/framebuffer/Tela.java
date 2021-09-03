package aplicativo.opengl.framebuffer;

public class Tela extends FrameBuffer {
    private static Tela instancia;
    
    private Tela() {
        setId( 0 );
        setAlocado( true );
    }
    
    public static Tela getInstance() {
        if ( instancia == null )
            instancia = new Tela();
        
        return instancia;
    }
}