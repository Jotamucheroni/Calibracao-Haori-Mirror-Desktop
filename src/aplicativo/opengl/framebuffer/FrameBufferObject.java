package aplicativo.opengl.framebuffer;

import java.nio.ByteBuffer;

import com.jogamp.opengl.GL4;

import aplicativo.opengl.RenderBuffer;

public class FrameBufferObject extends FrameBuffer implements AutoCloseable {
    private final int
        numeroRenderBuffer,
        numeroComponentesCor;
    
    private final RenderBuffer[] renderBuffer;
    
    public FrameBufferObject(
        int numeroRenderBuffer, int largura, int altura, int numeroComponentesCor
    ) {
        int[] bufferId = new int[1];
        gl4.glGenFramebuffers( 1, bufferId, 0 );
        setId( bufferId[0] );
        
        if ( numeroRenderBuffer < 1 )
            numeroRenderBuffer = 1;
        this.numeroRenderBuffer = numeroRenderBuffer;
        
        int[] drawBuffers = new int[this.numeroRenderBuffer];
        for ( int i = 0; i < this.numeroRenderBuffer; i++ )
            drawBuffers[i] = GL4.GL_COLOR_ATTACHMENT0 + i;
        bindDraw();
        gl4.glDrawBuffers( numeroRenderBuffer, drawBuffers, 0 );
        
        setLargura( largura );
        setAltura( altura );
        
        if ( numeroComponentesCor < 1 )
            numeroComponentesCor = 1;
        else if ( numeroComponentesCor > 4 )
            numeroComponentesCor = 4;
        this.numeroComponentesCor = numeroComponentesCor;
        
        renderBuffer = new RenderBuffer[getNumeroRenderBuffer()];
        int
            larguraFinal = getLargura(),
            alturaFinal = getAltura();
        
        for ( int i = 0; i < renderBuffer.length; i++ ) {
            renderBuffer[i] = new RenderBuffer(
                larguraFinal, alturaFinal, this.numeroComponentesCor
            );
            gl4.glFramebufferRenderbuffer(
                GL4.GL_DRAW_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0 + i,
                GL4.GL_RENDERBUFFER, renderBuffer[i].getId()
            );
        }
        unbindDraw();
    }
    
    public FrameBufferObject( int numeroRenderBuffer, int largura, int altura ) {
        this( numeroRenderBuffer, largura, altura, 4 );
    }
    
    public FrameBufferObject( int largura, int altura ) {
        this( 1, largura, altura, 4 );
    }
    
    public int getNumeroRenderBuffer() {
        return numeroRenderBuffer;
    }
    
    public int getNumeroComponentesCor() {
        return numeroComponentesCor;
    }
    
    public int getNumeroBytes() {
        return getNumeroPixeis() * numeroComponentesCor;
    }
    
    public void copiar(
        FrameBuffer destino,
        int x, int y, int largura, int altura,
        int numColunas, int numLinhas
    ) {
        if ( destino == null )
            return;
        
        if( x < 0 )
            x = 0;
        
        if( y < 0 )
            y = 0;
        
        if( largura < 1 )
            largura = 1;
        
        if( altura < 1 )
            altura = 1;
        
        if( numColunas < 1 )
            numColunas = 1;
        
        if( numLinhas < 1 )
            numLinhas = 1;
        
        int
            numCelulas = numColunas * numLinhas,
            largColuna = largura / numColunas, altLinha = altura / numLinhas;
        
        bindRead();
        destino.bindDraw();
        for ( int i = 0; i < numCelulas; i++ ) {
            int coluna = i % numColunas;
            int linha = ( numLinhas - 1 ) - ( i / numColunas );
            
            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT0 + i );
            gl4.glBlitFramebuffer(
                0, 0, getLargura(), getAltura(),
                x + coluna * largColuna, y + linha * altLinha, 
                x + ( coluna + 1 ) * largColuna, y + ( linha  + 1 ) * altLinha,
                GL4.GL_COLOR_BUFFER_BIT, GL4.GL_LINEAR
            );
        }
        destino.unbindDraw();
        unbindRead();
    }
    
    public void copiar(
        FrameBuffer destino,
        int largura, int altura,
        int numColunas, int numLinhas
    ) {
        copiar( destino, 0, 0, largura, altura, numColunas, numLinhas );
    }
    
    public void copiar(
        FrameBuffer destino,
        int largura, int altura
    ) {
        copiar( destino, 0, 0, largura, altura, 1, 1 );
    }
    
    public void lerRenderBuffer(
        int numero,
        int numeroComponentesCor,
        int x, int y, int largura, int altura,
        ByteBuffer destino
    ) {
        if ( destino == null )
            return;
        
        if ( numero < 1 )
            numero = 1;
        else if ( numero > numeroRenderBuffer )
            numero = numeroRenderBuffer;
        
        int formato;
        switch ( numeroComponentesCor ) {
            case 1:
                formato = GL4.GL_RED;
                break;
            case 3:
                formato = GL4.GL_RGB;
                break;
            case 4:
            default:
                formato = GL4.GL_RGBA;
                break;
        }
        
        bindRead();
        gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT0 + ( numero - 1 ) );
        gl4.glReadPixels(
            x, y, largura, altura, 
            formato, GL4.GL_UNSIGNED_BYTE,
            destino
        );
    }
    
    public void lerRenderBuffer(
        int numero,
        int x, int y, int largura, int altura,
        ByteBuffer destino
    ) {
        lerRenderBuffer( numero, 4, x, y, largura, altura, destino );
    }
    
    public void lerRenderBuffer(
        int numero,
        int numeroComponentesCor,
        int largura, int altura,
        ByteBuffer destino
    ) {
        lerRenderBuffer( numero, numeroComponentesCor, 0, 0, largura, altura, destino );
    }
    
    public void lerRenderBuffer(
        int numero,
        int largura, int altura,
        ByteBuffer destino
    ) {
        lerRenderBuffer( numero, 4, 0, 0, largura, altura, destino );
    }
    
    public void lerRenderBuffer(
        int numero,
        int numeroComponentesCor,
        ByteBuffer destino
    ) {
        lerRenderBuffer( numero, numeroComponentesCor, 0, 0, getLargura(), getAltura(), destino );
    }
    
    public void lerRenderBuffer(
        int numero,
        ByteBuffer destino
    ) {
        lerRenderBuffer( numero, 4, 0, 0, getLargura(), getAltura(), destino );
    }
    
    @Override
    public void close() {
        for ( RenderBuffer rb : renderBuffer )
            rb.close();
        gl4.glDeleteFramebuffers( 1, new int[]{ getId() }, 0 );
    }
}