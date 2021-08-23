package opengl;

import java.nio.ByteBuffer;
import java.io.DataInputStream;
import java.io.IOException;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import es.Bluetooth;
import es.camera.CameraLocal;
import es.camera.CameraRemota;

public class Renderizador extends OpenGL implements GLEventListener {
    private final float[] 
        refQuad = {
            // Coordenadas  Textura
            -1.0f,  1.0f,   0.0f, 0.0f,
            -1.0f, -1.0f,   0.0f, 1.0f,
            1.0f, -1.0f,    1.0f, 1.0f,
            1.0f,  1.0f,    1.0f, 0.0f
        };
    
    private final int[] refElementos = { 0, 1, 2, 2, 3, 0 };
    
    private CameraLocal cameraOlhoVirtual;
    private CameraRemota cameraSmartphone;    
    private Bluetooth bluetooth;
    private FrameBufferObject frameBufferOlhoVirtual, frameBufferSmartphone;
    private Textura texturaOlhoVirtual, texturaSmartphone;
    
    private Objeto imagemOlhoVirtual, imagemSmartphone;
    private DetectorBorda detectorOlhoVirtual, detectorSmartphone;
    private ByteBuffer bufferBordaOlhoVirtual, bufferBordaSmartphone;
    
    @Override
    public void init( GLAutoDrawable drawable ) {
        // Executar sempre primeiro
        gl4 = drawable.getGL().getGL4();

        cameraOlhoVirtual = new CameraLocal( 0, 640, 480, 1 );
        cameraOlhoVirtual.ligar();
        cameraSmartphone = new CameraRemota( 320, 240, 1 );
        
        frameBufferOlhoVirtual = new FrameBufferObject( 3, 640, 480 );
        frameBufferSmartphone = new FrameBufferObject( 3, 640, 480 );
        
        texturaOlhoVirtual = new Textura(
            cameraOlhoVirtual.getLargImg(), cameraOlhoVirtual.getAltImg(), true
        );
        texturaOlhoVirtual.alocar();
        texturaSmartphone = new Textura(
            cameraSmartphone.getLargImg(), cameraSmartphone.getAltImg(), true
        );
        texturaSmartphone.alocar();
        
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        imagemOlhoVirtual = new Objeto(
            GL4.GL_TRIANGLES, 2, 2,
            refQuad, refElementos, texturaOlhoVirtual
        );
        imagemSmartphone = new Objeto(
            GL4.GL_TRIANGLES, 2, 2,
            refQuad, refElementos, texturaSmartphone
        );
        
        bufferBordaOlhoVirtual = ByteBuffer.allocateDirect( frameBufferOlhoVirtual.getNumBytes() );
        bufferBordaSmartphone = ByteBuffer.allocateDirect( frameBufferOlhoVirtual.getNumBytes() );
        
        detectorOlhoVirtual = new DetectorBorda( frameBufferOlhoVirtual.getNumBytes(), FrameBufferObject.numCompCor );
        detectorSmartphone = new DetectorBorda( frameBufferOlhoVirtual.getNumBytes(), FrameBufferObject.numCompCor );
        
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
                    
                    synchronized( cameraSmartphone ) {
                        cameraSmartphone.setEntradaRemota( entradaRemota );
                        cameraSmartphone.ligar();
                    }
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        ).start(); */
    }
    
    private Tela tela = Tela.getInstance();
    
    @Override
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height )
	{
        tela.setLargura( width );
        tela.setAltura( height );
	}
    
    @Override
    public void display( GLAutoDrawable drawable ) {
        texturaOlhoVirtual.carregarImagem( cameraOlhoVirtual.getImagem() );
        
        synchronized( cameraSmartphone ) {
            if( cameraSmartphone.ligada() )
                texturaSmartphone.carregarImagem( cameraSmartphone.getImagem() );
        }
        
        frameBufferOlhoVirtual.clear();
        frameBufferOlhoVirtual.draw( imagemOlhoVirtual );
        
        frameBufferOlhoVirtual.bindRead();
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
        
        frameBufferSmartphone.clear();
        frameBufferSmartphone.draw( imagemSmartphone );
        
        frameBufferSmartphone.bindRead();
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
        
        tela.clear();
        frameBufferOlhoVirtual.copiar(
            tela, 0, tela.getAltura() / 2, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );
        frameBufferSmartphone.copiar(
            tela, 0, 0, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );
    }
    
    private boolean executando = true;
    
    @Override
    public void dispose( GLAutoDrawable drawable ) {
        executando = false;
        
        detectorOlhoVirtual.close();
        detectorSmartphone.close();
        texturaOlhoVirtual.close();
        texturaSmartphone.close();
        frameBufferOlhoVirtual.close();
        cameraOlhoVirtual.close();
        cameraSmartphone.close();
        synchronized( bluetooth ) {
            bluetooth.close();
        }
    }
    
    public boolean getExecutando() {
        return executando;
    }
}