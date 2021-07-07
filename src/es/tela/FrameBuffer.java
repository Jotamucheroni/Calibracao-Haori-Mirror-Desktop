package es.tela;

import com.jogamp.opengl.GL4;

public class FrameBuffer implements AutoCloseable {
    public static GL4 gl4;

    public static final int numCompCor = 3;

    private int numRenderBuffer;
    private int largura, altura;

    private int[] id;

    private RenderBuffer[] rb;

    public FrameBuffer( int numRenderBuffer, int largura, int altura ) {
        setNumRenderBuffer( numRenderBuffer );
        setLargura( largura );
        setAltura( altura );
        id = new int[1];
        gl4.glGenFramebuffers( 1, id, 0 );

        alocar();
    }

    public FrameBuffer( int largura, int altura ) {
        this( 1, largura, altura );
    }

    public FrameBuffer( int numRenderBuffer ) {
        this( numRenderBuffer, 1, 1 );
    }

    public FrameBuffer() {
        this( 1, 1, 1 );
    }

    public void setNumRenderBuffer( int numRenderBuffer ) {
        if ( numRenderBuffer < 1 ) {
            numRenderBuffer = 1;
        }

        this.numRenderBuffer = numRenderBuffer;
    }
    
    public void setLargura( int largura ) {
        if ( largura < 1 ) {
            largura = 1;
        }

        this.largura = largura;
    }

    public void setAltura( int altura ) {
        if ( altura < 1 ) {
            altura = 1;
        }

        this.altura = altura;
    }

    public int getNumRenderBuffer() {
        return this.numRenderBuffer;
    }
    
    public int getLargura() {
        return this.largura;
    }

    public int getAltura() {
        return this.altura;
    }

    public int getId() {
        return this.id[0];
    }

    public int getNumPix() {
        return this.largura * this.altura;
    }

    public int getNumBytes() {
        return this.largura * this.altura * FrameBuffer.numCompCor;
    }

    public void alocar() {
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, id[0] );
        rb = new RenderBuffer[getNumRenderBuffer()];
        for ( int i = 0; i < rb.length; i++ ) {
            rb[i] = new RenderBuffer( getLargura(), getAltura() );
            gl4.glFramebufferRenderbuffer( GL4.GL_DRAW_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0 + i, GL4.GL_RENDERBUFFER, rb[i].getId() );
        }
    }

    @Override
    public void close() {
        for ( RenderBuffer r : rb )
            r.close();
        gl4.glDeleteFramebuffers( 1, id, 0 );
    }
}
