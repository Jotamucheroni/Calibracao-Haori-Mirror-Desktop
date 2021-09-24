package aplicativo.opengl;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL4;

public class Textura extends OpenGL implements AutoCloseable {
    private static final int[] formatoInterno = new int[]{
        GL4.GL_R8, GL4.GL_RG8, GL4.GL_RGB8, GL4.GL_RGBA8
    };
    private static final int[] formatoImagem = new int[]{
        GL4.GL_RED, GL4.GL_RG, GL4.GL_RGB, GL4.GL_RGBA
    };
    
    private final int id;
    private final int largura, altura;
    private final int numeroComponentesCor;
    
    public Textura( int largura, int altura, int numeroComponentesCor ) {
        int[] bufferId = new int[1];
        gl4.glGenTextures( 1, bufferId, 0 );
        id = bufferId[0];
        
        bind();
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR );
        
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
        
        gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D, 0, formatoInterno[this.numeroComponentesCor - 1],
            this.largura, this.altura, 0,
            formatoImagem[this.numeroComponentesCor - 1], GL4.GL_UNSIGNED_BYTE, null
        );
        unbind();
    }
    
    public Textura( int largura, int altura ) {
        this( largura, altura, 4 );
    }
    
    public void bind() {
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, id );
    }
    
    public void unbind() {
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, 0 );
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
    
    public void carregarImagem( ByteBuffer imagem ) {
        if ( imagem == null )
            return;
        
        bind();
        gl4.glTexSubImage2D(
            GL4.GL_TEXTURE_2D, 0,
            0, 0, largura, altura,
            formatoImagem[numeroComponentesCor - 1], GL4.GL_UNSIGNED_BYTE, imagem
        );
        unbind();
    }
    
    @Override
    public void close() {
        gl4.glDeleteTextures( 1, new int[]{ id }, 0 );
    }
}