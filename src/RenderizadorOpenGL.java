import java.nio.ByteBuffer;
import java.io.DataInputStream;
import java.io.IOException;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import es.Bluetooth;
import es.camera.CameraLocal;
import es.camera.CameraRemota;
import es.tela.FrameBuffer;
import es.tela.RenderBuffer;

public class RenderizadorOpenGL implements GLEventListener {
    private static GL4 gl4;
    
    private final float[] 
        refQuad = {
            // Coordenadas  Textura
            -1.0f,  1.0f,   0.0f, 0.0f,
            -1.0f, -1.0f,   0.0f, 1.0f,
            1.0f, -1.0f,    1.0f, 1.0f,
            1.0f,  1.0f,    1.0f, 0.0f
        };
    
    private final int[] refElementos = { 0, 1, 2, 2, 3, 0 };
    
    private CameraLocal olhoVirtual;
    private CameraRemota smartphone;    
    private Bluetooth bluetooth;
    private FrameBuffer frameBufferOlhoVirtual, frameBufferSmartphone;
    
    private final int[] texturas = new int[4]; 
    private void setTexParams() {
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR );
    }
    
    private ProgramaOpenGL programaOpenGL;
    private Objeto imagemOlhoVirtual, imagemSmartphone;
    private DetectorBorda detectorOlhoVirtual, detectorSmartphone;
    private ByteBuffer bufferBordaOlhoVirtual, bufferBordaSmartphone;
    
    @Override
    public void init( GLAutoDrawable drawable ) {
        // Executar sempre primeiro
        gl4 = drawable.getGL().getGL4();
        programaOpenGL = new ProgramaOpenGL( gl4 );
        Objeto.gl4 = gl4;
        Objeto.programaOpenGL = programaOpenGL;
        RenderBuffer.gl4 = gl4;
        FrameBuffer.gl4 = gl4;
        ImagemOpenGL.gl4 = gl4;
        
        olhoVirtual = new CameraLocal( 0, 640, 480, 1 );
        olhoVirtual.ligar();
        smartphone = new CameraRemota( 320, 240, 1 );
        
        frameBufferOlhoVirtual = new FrameBuffer( 3, 640, 480 );
        frameBufferSmartphone = new FrameBuffer( 3, 640, 480 );
        
        // Cria as texturas e configura seus parâmetros
        gl4.glGenTextures( texturas.length, texturas, 0 );
        for( int textura : texturas ) {
            gl4.glBindTexture( GL4.GL_TEXTURE_2D, textura );
            setTexParams();
        }
        
        // Aloca espaço para a textura 0 para receber imagens do olho virtual
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[0] );
        gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
            olhoVirtual.getLargImg(), olhoVirtual.getAltImg(), 0,
            GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, null
        );
        
        // Aloca espaço para a textura 1 para receber imagens da câmera do smartphone
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[1] );
        gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
            smartphone.getLargImg(), smartphone.getAltImg(), 0,
            GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, null
        );
        
        // Carrega imagens nas demais texturas
        /* new ImagemOpenGL( "imagens/cachorrinho.png", texturas[2] ).carregar();
        new ImagemOpenGL( "imagens/gatinho.png", texturas[3] ).carregar(); */
        
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        imagemOlhoVirtual = new Objeto(
            GL4.GL_TRIANGLES, 2, 2,
            refQuad, refElementos, texturas[0], true
        );
        imagemSmartphone = new Objeto(
            GL4.GL_TRIANGLES, 2, 2,
            refQuad, refElementos, texturas[1], true
        );
        
        bufferBordaOlhoVirtual = ByteBuffer.allocateDirect( frameBufferOlhoVirtual.getNumBytes() );
        bufferBordaSmartphone = ByteBuffer.allocateDirect( frameBufferOlhoVirtual.getNumBytes() );
        
        detectorOlhoVirtual = new DetectorBorda( frameBufferOlhoVirtual.getNumBytes(), FrameBuffer.numCompCor );
        detectorSmartphone = new DetectorBorda( frameBufferOlhoVirtual.getNumBytes(), FrameBuffer.numCompCor );
        
        // Inicia a comunicação por Bluetooth para receber as imagens da câmera do smartphone
        bluetooth = new Bluetooth();
        new Thread(
            () -> {
                try {
                    DataInputStream entradaRemota;
                    
                    synchronized( bluetooth ) {
                        entradaRemota = 
                            bluetooth.conectarDispositivo( "304B0745112F" ).openDataInputStream();
                    }
                    
                    synchronized( smartphone ) {
                        smartphone.setEntradaRemota( entradaRemota );
                        smartphone.ligar();
                    }
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        ).start();
    }
    
    private int larguraTela, alturaTela;
    
    @Override
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height )
	{
        larguraTela = width;
        alturaTela = height;
	}
    
    @Override
    public void display( GLAutoDrawable drawable ) {
        // Copia a imagem atual do olho virtual para a textura 0
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[0] );
        gl4.glTexSubImage2D(
            GL4.GL_TEXTURE_2D, 0, 
            0, 0, olhoVirtual.getLargImg(), olhoVirtual.getAltImg(),
            GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, olhoVirtual.getImagem()
        );
        
        // Copia a imagem atual do smartphone para a textura 1
        synchronized( smartphone ) {
            if( smartphone.ligada() ) {
                gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[1] );
                gl4.glTexSubImage2D(
                    GL4.GL_TEXTURE_2D, 0, 
                    0, 0, smartphone.getLargImg(), smartphone.getAltImg(),
                    GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, smartphone.getImagem()
                );
            }
        }
        
        // Desenha no framebuffer intermediário
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, frameBufferOlhoVirtual.getId() );
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
        gl4.glViewport( 0, 0, frameBufferOlhoVirtual.getLargura(), frameBufferOlhoVirtual.getAltura() );
        imagemOlhoVirtual.draw();
        
        gl4.glBindFramebuffer( GL4.GL_READ_FRAMEBUFFER, frameBufferOlhoVirtual.getId() );
        if ( detectorOlhoVirtual.pronto() ) {
            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT2 );
            bufferBordaOlhoVirtual.rewind();
            gl4.glReadPixels(
                0, 0, frameBufferOlhoVirtual.getLargura(), frameBufferOlhoVirtual.getAltura(), 
                GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE, 
                bufferBordaOlhoVirtual
            );
            
            detectorOlhoVirtual.setImagem( bufferBordaOlhoVirtual );
            detectorOlhoVirtual.executar();
        }
        
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, frameBufferSmartphone.getId() );
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
        gl4.glViewport( 0, 0, frameBufferSmartphone.getLargura(), frameBufferSmartphone.getAltura() );
        imagemSmartphone.draw();
        
        gl4.glBindFramebuffer( GL4.GL_READ_FRAMEBUFFER, frameBufferSmartphone.getId() );
        if ( detectorSmartphone.pronto() ) {
            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT2 );
            bufferBordaSmartphone.rewind();
            gl4.glReadPixels(
                0, 0, frameBufferSmartphone.getLargura(), frameBufferSmartphone.getAltura(),
                GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE,
                bufferBordaSmartphone
            );
            
            detectorSmartphone.setImagem( bufferBordaSmartphone );
            detectorSmartphone.executar();
        }    
        
        // Desenha na tela
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, 0 );
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
        frameBufferOlhoVirtual.exibir( 0, alturaTela / 2, larguraTela, alturaTela / 2, 3, 1 );
        frameBufferSmartphone.exibir( 0, 0, larguraTela, alturaTela / 2, 3, 1 );
    }
    
    @Override
    public void dispose( GLAutoDrawable drawable ) {
        detectorOlhoVirtual.close();
        detectorSmartphone.close();
        programaOpenGL.close();
        gl4.glDeleteTextures( texturas.length, texturas, 0 );
        frameBufferOlhoVirtual.close();
        olhoVirtual.close();
        smartphone.close();
        synchronized( bluetooth ) {
            bluetooth.close();
        }
        Aplicativo.close();
    }
}