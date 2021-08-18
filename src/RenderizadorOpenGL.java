import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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
    
    // private final int numLinhas = 2, numColunas = 3, numLinhasM1 = numLinhas - 1;
    
    private FrameBuffer frameBuffer;
    
    private final int[] texturas = new int[4];
    
    private void setTexParams() {
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR );
    }
    
    // Objetos
    private final Objeto[] objetos = new Objeto[1];
    
    private DetectorBorda detectorOlhoVirtual, detectorSmartphone;
    private ByteBuffer bufferBordaOlhoVirtual, bufferBordaSmartphone;
    
    private ProgramaOpenGL programaOpenGL;
    
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
        
        // Cria e abre a câmera para capturar as imagens do olho virtual
        olhoVirtual = new CameraLocal( 0, 640, 480, 1 );
        olhoVirtual.ligar();
        
        // Cria a câmera para receber as imagens do smartphone
        smartphone = new CameraRemota( 320, 240, 1 );
        
        // Cria um Framebuffer e seus respectivos Renderbuffers
        frameBuffer = new FrameBuffer( 6, 640, 480 );
        
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
        
        // Determina a cor de fundo
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        // Cria objetos para exibir as imagens das câmeras
        objetos[0] = new Objeto(
            GL4.GL_TRIANGLES, 2, 2,
            refQuad, refElementos, new int[]{ texturas[0], texturas[1] }, true
        );
        
        bufferBordaOlhoVirtual = ByteBuffer.allocateDirect( frameBuffer.getNumBytes() );
        bufferBordaSmartphone = ByteBuffer.allocateDirect( frameBuffer.getNumBytes() );
        
        detectorOlhoVirtual = new DetectorBorda( frameBuffer.getNumBytes(), FrameBuffer.numCompCor );
        detectorSmartphone = new DetectorBorda( frameBuffer.getNumBytes(), FrameBuffer.numCompCor );
        
        // Inicia a comunicação por Bluetooth para receber as imagens da câmera do smartphone
        bluetooth = new Bluetooth();
        /* new Thread(
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
        ).start(); */
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
        // Imagens das câmeras
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
        
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, frameBuffer.getId() );
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
        gl4.glViewport( 0, 0, frameBuffer.getLargura(), frameBuffer.getAltura() );
        for ( Objeto obj: objetos )
            obj.draw();
        
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, 0 );
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
        frameBuffer.exibir( larguraTela, alturaTela, 3, 2 );
        
        if ( detectorOlhoVirtual.pronto() ) {
            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT2 );
            bufferBordaOlhoVirtual.rewind();
            gl4.glReadPixels(
                0, 0, frameBuffer.getLargura(), frameBuffer.getAltura(), 
                GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE, 
                bufferBordaOlhoVirtual
            );
            
            detectorOlhoVirtual.setImagem( bufferBordaOlhoVirtual );
            detectorOlhoVirtual.executar();
        }
        if ( detectorSmartphone.pronto() ) {
            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT5 );
            bufferBordaSmartphone.rewind();
            gl4.glReadPixels(
                0, 0, frameBuffer.getLargura(), frameBuffer.getAltura(),
                GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE,
                bufferBordaSmartphone
            );
            
            detectorSmartphone.setImagem( bufferBordaSmartphone );
            detectorSmartphone.executar();
        }
    }
    
    @Override
    public void dispose( GLAutoDrawable drawable ) {
        detectorOlhoVirtual.close();
        detectorSmartphone.close();
        programaOpenGL.close();
        gl4.glDeleteTextures( texturas.length, texturas, 0 );
        frameBuffer.close();
        olhoVirtual.close();
        smartphone.close();
        synchronized( bluetooth ) {
            bluetooth.close();
        }
        Aplicativo.close();
    }
}