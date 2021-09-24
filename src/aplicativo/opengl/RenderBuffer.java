package aplicativo.opengl;

import com.jogamp.opengl.GL4;

public class RenderBuffer extends OpenGL implements AutoCloseable {
    private static final int[] formatoInterno = new int[]{
        GL4.GL_R8, GL4.GL_RG8, GL4.GL_RGB8, GL4.GL_RGBA8
    };
    
    private final int id;
    private final int largura, altura;
    private final int numeroComponentesCor;
    
    public RenderBuffer( int largura, int altura, int numeroComponentesCor ) {
        int[] bufferId = new int[1];
        gl4.glGenRenderbuffers( 1, bufferId, 0 );
        id = bufferId[0];
        
        if ( largura < 1 )
            largura = 1;
        this.largura = largura;
        
        if ( altura < 1 )
            altura = 1;
        this.altura = altura;
        
        if ( numeroComponentesCor < 1 )
            numeroComponentesCor = 1;
        else if ( numeroComponentesCor > 4 )
            numeroComponentesCor = 4;
        this.numeroComponentesCor = numeroComponentesCor;
        
        gl4.glBindRenderbuffer( GL4.GL_RENDERBUFFER, id );
        gl4.glRenderbufferStorage(
            GL4.GL_RENDERBUFFER, formatoInterno[this.numeroComponentesCor - 1], largura, altura
        );
    }
    
    public RenderBuffer( int largura, int altura ) {
        this( largura, altura, 4 );
    }
    
    public int getId() {
        return id;
    }
    
    public int getLargura() {
        return largura;
    }
    
    public int getAltura() {
        return altura;
    }
    
    public int getNumeroComponentesCor() {
        return numeroComponentesCor;
    }
    
    public int getNumeroPixeis() {
        return largura * altura;
    }
    
    public int getNumeroBytes() {
        return getNumeroPixeis() * numeroComponentesCor;
    }
    
    @Override
    public void close() {
        gl4.glDeleteRenderbuffers( 1, new int[]{ id }, 0 );
    }
}