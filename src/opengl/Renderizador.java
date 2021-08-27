package opengl;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import es.Bluetooth;
import es.camera.CameraLocal;
import es.camera.CameraRemota;
import opengl.renderbuffer.FrameBufferObject;
import opengl.renderbuffer.Tela;

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
    private DetectorBorda detectorBordaOlhoVirtual, detectorBordaSmartphone, detectorTeste;
    
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
        
        detectorBordaOlhoVirtual = new DetectorBorda( frameBufferOlhoVirtual.getNumPix(), 1 );
        detectorBordaOlhoVirtual.alocar();
        
        detectorBordaSmartphone = new DetectorBorda( frameBufferOlhoVirtual.getNumPix(), 1 );
        detectorBordaSmartphone.alocar();
        
        detectorTeste = new DetectorBorda( frameBufferOlhoVirtual.getNumPix(), 1 );
        detectorTeste.alocar();
        
        bluetooth = new Bluetooth();
        bluetooth.conectarDispositivo( "304B0745112F" ); // Smartphone
    }
    
    private final Tela tela = Tela.getInstance();
    
    @Override
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height )
	{
        tela.setLargura( width );
        tela.setAltura( height );
	}
    
    @Override
    public void display( GLAutoDrawable drawable ) {
        if ( !cameraSmartphone.ligada() && bluetooth.getConectado() ) {
            cameraSmartphone.setEntradaRemota( bluetooth.getConexao() );
            cameraSmartphone.ligar();
        }
        
        texturaOlhoVirtual.carregarImagem( cameraOlhoVirtual.getImagem() );
        texturaSmartphone.carregarImagem( cameraSmartphone.getImagem() );
        
        frameBufferOlhoVirtual.clear();
        frameBufferOlhoVirtual.draw( imagemOlhoVirtual );
        
        if ( detectorBordaOlhoVirtual.pronto() ) {
            // System.out.println( "Píxeis(1): " + detectorBordaOlhoVirtual.getSaida() );
            frameBufferOlhoVirtual.lerRenderBuffer( 1, 1, detectorBordaOlhoVirtual.getImagem() );
            detectorBordaOlhoVirtual.executar();
        }
        
        frameBufferSmartphone.clear();
        frameBufferSmartphone.draw( imagemSmartphone );
        
        if ( detectorBordaSmartphone.pronto() ) {
            // System.out.println( "Píxeis(2): " + detectorBordaSmartphone.getSaida() );
            frameBufferOlhoVirtual.lerRenderBuffer( 2, 1, detectorBordaSmartphone.getImagem() );
            detectorBordaSmartphone.executar();
        }
        
        if ( detectorTeste.pronto() ) {
            // System.out.println( "Píxeis(3): " + detectorTeste.getSaida() );
            frameBufferOlhoVirtual.lerRenderBuffer( 3, 1, detectorTeste.getImagem() );
            detectorTeste.executar();
        }
        
        // System.out.println();
        
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
        
        detectorBordaOlhoVirtual.close();
        detectorBordaSmartphone.close();
        detectorTeste.close();
        texturaOlhoVirtual.close();
        texturaSmartphone.close();
        frameBufferOlhoVirtual.close();
        cameraOlhoVirtual.close();
        cameraSmartphone.close();
        bluetooth.close();
    }
    
    public boolean getExecutando() {
        return executando;
    }
}