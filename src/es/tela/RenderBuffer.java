package es.tela;

import com.jogamp.opengl.GL4;

public class RenderBuffer implements AutoCloseable {
    public static GL4 gl4;
    
    public static final int numCompCor = 3;
    
    private int largura, altura;
    
    private final int id;
    
    public RenderBuffer( int largura, int altura ) {
        setLargura( largura );
        setAltura( altura );
        
        int[] bufferId = new int[1];
        gl4.glGenRenderbuffers( 1, bufferId, 0 );
        id = bufferId[0];
        
        alocar();
    }
    
    public RenderBuffer() {
        this( 1, 1 );
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
        return getNumPix() * RenderBuffer.numCompCor;
    }
    
    private void alocar() {
        gl4.glBindRenderbuffer( GL4.GL_RENDERBUFFER, id );
        gl4.glRenderbufferStorage( GL4.GL_RENDERBUFFER, GL4.GL_RGB8, largura, altura );
    }
    
    @Override
    public void close() {
        gl4.glDeleteRenderbuffers( 1, new int[]{ id }, 0 );
    }
}