import java.nio.ByteBuffer;

import com.jogamp.opengl.GL4;

public class TexturaOpenGL implements AutoCloseable {
    public static GL4 gl4;
    
    private final int id;
    private int largura, altura;
    private boolean monocromatica;
    private int formato;
    
    public TexturaOpenGL( int largura, int altura, boolean monocromatica ) {
        setLargura( largura );
        setAltura( altura );
        setMonocromatica( monocromatica );
        
        int[] bufferId = new int[1];
        gl4.glGenTextures( 1, bufferId, 0 );
        id = bufferId[0];
        
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, id );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR );
        
        // alocar();
    }
    
    public TexturaOpenGL( int largura, int altura ) {
        this( largura, altura, false );
    }
    
    public TexturaOpenGL( boolean monocromatica ) {
        this( 1, 1, monocromatica );
    }
    
    public TexturaOpenGL() {
        this( 1, 1, false );
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
    
    public void setMonocromatica( boolean monocromatica ) {
        this.monocromatica = monocromatica;
        
        formato = monocromatica ? GL4.GL_RED : GL4.GL_BGR; 
    }
    
    public int getLargura() {
        return largura;
    }
    
    public int getAltura() {
        return altura;
    }
    
    public boolean getMonocromatica() {
        return monocromatica;
    }
    
    public int getId() {
        return id;
    }
    
    private boolean alocado = false;
    
    public void alocar() {
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, id );       
        gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
            largura, altura, 0,
            formato, GL4.GL_UNSIGNED_BYTE, null
        );
        
        alocado = true;
    }
    
    public void carregarImagem( ByteBuffer imagem ) {
        if ( imagem == null || !alocado )
            return;
        
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, id );
        gl4.glTexSubImage2D(
            GL4.GL_TEXTURE_2D, 0,
            0, 0, largura, altura,
            formato, GL4.GL_UNSIGNED_BYTE, imagem
        );
    }
    
    public void bind() {
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, id );
    }
    
    @Override
    public void close() {
        gl4.glDeleteTextures( 1, new int[]{ id }, 0 );
    }
}
