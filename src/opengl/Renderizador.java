package opengl;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import es.Bluetooth;
import es.camera.CameraLocal;
import es.camera.CameraRemota;
import es.dispositivo.Dispositivo;
import es.dispositivo.DispositivoRemoto;
import opengl.framebuffer.Tela;

public class Renderizador extends OpenGL implements GLEventListener {
    private Dispositivo olhoVirtual;
    private DispositivoRemoto smartphone;
    private Dispositivo[] dispositivo;
      
    private Bluetooth bluetooth;
    
    @Override
    public void init( GLAutoDrawable drawable ) {
        // Executar sempre primeiro
        gl4 = drawable.getGL().getGL4();
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );

        olhoVirtual = new Dispositivo( "Olho virtual", new CameraLocal( 0, 640, 480, 1 ) );
        olhoVirtual.alocar();
        olhoVirtual.ligar();
        smartphone = new DispositivoRemoto( "Smartphone", new CameraRemota( 320, 240, 1 ) );
        smartphone.alocar();
        
        dispositivo = new Dispositivo[] { olhoVirtual, smartphone };
        
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
        if ( !smartphone.getLigado() && bluetooth.getConectado() ) {
            smartphone.setEntradaRemota( bluetooth.getConexao() );
            smartphone.ligar();
        }
        
        for ( Dispositivo disp : dispositivo ) {
            disp.atualizarTextura();
            disp.draw();
            disp.atualizarImagemDetector( 3 );
        }
        
        System.out.println();
        
        tela.clear();
        olhoVirtual.getFrameBufferObject().copiar(
            tela, 0, tela.getAltura() / 2, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );
        smartphone.getFrameBufferObject().copiar(
            tela, 0, 0, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );
    }
    
    private boolean executando = true;
    
    @Override
    public void dispose( GLAutoDrawable drawable ) {
        executando = false;
        
        olhoVirtual.close();
        smartphone.close();
        bluetooth.close();
    }
    
    public boolean getExecutando() {
        return executando;
    }
}