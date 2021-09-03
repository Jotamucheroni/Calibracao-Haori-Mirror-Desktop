package aplicativo.opengl;

import com.jogamp.opengl.GL4;

public class RenderBuffer extends OpenGL implements AutoCloseable {
    public static final int numeroComponentesCor = 4;
    
    private int largura, altura;
    
    private final int id;
    
    public RenderBuffer( int largura, int altura ) {
        setLargura( largura );
        setAltura( altura );
        
        int[] bufferId = new int[1];
        gl4.glGenRenderbuffers( 1, bufferId, 0 );
        id = bufferId[0];
    }
    
    public void setLargura( int largura ) {
        if ( largura < 1 )
            largura = 1;
        
        this.largura = largura;
    }
    
    public void setAltura( int altura ) {
        if ( altura < 1 )
            altura = 1;
        
        this.altura = altura;
    }
    
    public int getLargura() {
        return largura;
    }
    
    public int getAltura() {
        return altura;
    }
    
    public int getId() {
        return id;
    }
    
    public int getNumPix() {
        return largura * altura;
    }
    
    public int getNumBytes() {
        return getNumPix() * RenderBuffer.numeroComponentesCor;
    }
    
    private boolean alocado = false;
    
    public void alocar() {
        gl4.glBindRenderbuffer( GL4.GL_RENDERBUFFER, id );
        gl4.glRenderbufferStorage( GL4.GL_RENDERBUFFER, GL4.GL_RGBA8, largura, altura );
        
        alocado = true;
    }
    
    public boolean getAlocado() {
        return alocado;
    }
    
    @Override
    public void close() {
        gl4.glDeleteRenderbuffers( 1, new int[]{ id }, 0 );
    }
}