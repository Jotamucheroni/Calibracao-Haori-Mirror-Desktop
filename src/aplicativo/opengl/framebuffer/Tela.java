package aplicativo.opengl.framebuffer;

public class Tela extends FrameBuffer {
    private static Tela instancia;
    
    private Tela() {
        setId( 0 );
    }
    
    public static Tela getInstance() {
        if ( instancia == null )
            instancia = new Tela();
        
        return instancia;
    }
    
    @Override
    public void setLargura( int largura ) {
        super.setLargura( largura );
    }
    
    @Override
    public void setAltura( int altura ) {
        super.setAltura( altura );
    }
}