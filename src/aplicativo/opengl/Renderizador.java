package aplicativo.opengl;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import aplicativo.Aplicativo;
import aplicativo.es.Bluetooth;
import aplicativo.es.camera.CameraLocal;
import aplicativo.es.camera.CameraRemota;
import aplicativo.es.dispositivo.Dispositivo;
import aplicativo.es.dispositivo.DispositivoRemoto;
import aplicativo.opengl.framebuffer.Tela;

public class Renderizador extends OpenGL implements GLEventListener {
    private Dispositivo olhoVirtual;
    private DispositivoRemoto smartphone;
    private Dispositivo[] dispositivo;
    
    private Bluetooth bluetooth;
    
    private Desenho linhasCentrais;
    
    @Override
    public void init( GLAutoDrawable drawable ) {
        // Executar sempre primeiro
        gl4 = drawable.getGL().getGL4();
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        
        bluetooth = new Bluetooth();
        bluetooth.conectarDispositivo( "304B0745112F" ); // Smartphone
        
        int numCameraLocal = 0;
        CameraLocal.imprimirDispositivos();
        System.out.print( "Escolha a câmera informando o índice: " );   
        numCameraLocal = Aplicativo.entrada.nextInt();
        olhoVirtual = new Dispositivo(
            "Olho virtual", new CameraLocal( numCameraLocal, 640, 480, 1 )
        );
        olhoVirtual.alocar();
        olhoVirtual.ligar();
        
        smartphone = new DispositivoRemoto( "Smartphone", new CameraRemota( 320, 240, 1 ) );
        smartphone.alocar();
        smartphone.esperarEntradaRemota( bluetooth );
        
        dispositivo = new Dispositivo[] { olhoVirtual, smartphone };
        
        linhasCentrais = new Desenho( 
            2, 3,
            new float[]{
               -1.0f,   0.0f,   1.0f,   0.0f,   0.0f,
                1.0f,   0.0f,   1.0f,   0.0f,   0.0f,
                0.0f,   1.0f,   1.0f,   0.0f,   0.0f,
                0.0f,  -1.0f,   1.0f,   0.0f,   0.0f
            },
            Desenho.LINHAS
        );
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
        for ( Dispositivo disp : dispositivo ) {
            disp.atualizarTextura();
            disp.draw();
            disp.atualizarImagemDetector( 3 );
            
            disp.getFrameBufferObject().draw( linhasCentrais );
        }
        
        // System.out.println();
        
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
        
        linhasCentrais.close();
        olhoVirtual.close();
        smartphone.close();
        bluetooth.close();
    }
    
    public boolean getExecutando() {
        return executando;
    }
}