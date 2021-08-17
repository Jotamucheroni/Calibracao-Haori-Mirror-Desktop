package es.tela;

import com.jogamp.opengl.GL4;

public class RenderBuffer implements AutoCloseable {
    public static GL4 gl4;
    
    public static final int numCompCor = 3;
    
    private int largura, altura;
    
    private final int[] id;
    
    public RenderBuffer( int largura, int altura ) {
        setLargura( largura );
        setAltura( altura );
        id = new int[1];
        gl4.glGenRenderbuffers( 1, id, 0 );
        
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
        return id[0];
    }
    
    public int getNumPix() {
        return largura * altura;
    }
    
    public int getNumBytes() {
        return getNumPix() * RenderBuffer.numCompCor;
    }
    
    public void alocar() {
        gl4.glBindRenderbuffer( GL4.GL_RENDERBUFFER, id[0] );
        gl4.glRenderbufferStorage( GL4.GL_RENDERBUFFER, GL4.GL_RGB8, getLargura(), getAltura() );
    }
    
    @Override
    public void close() {
        gl4.glDeleteRenderbuffers( 1, id, 0 );
    }
}