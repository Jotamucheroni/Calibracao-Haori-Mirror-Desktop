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
    private Bluetooth bluetooth;
    
    private Dispositivo olhoVirtual;
    private DispositivoRemoto smartphone;
    private Dispositivo[] dispositivo;
    
    private final int NUMERO_PARAMETROS_TEXTURA = 2;
    
    private Desenho linhasCentrais;
    
    @Override
    public void init( GLAutoDrawable drawable ) {
        gl4 = drawable.getGL().getGL4();    // Executar sempre primeiro
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        
        bluetooth = new Bluetooth();
        bluetooth.conectarDispositivo( "304B0745112F" );    // Smartphone
        
        Desenho desenho;
        
        olhoVirtual = new Dispositivo(
            "Olho virtual", new CameraLocal( Aplicativo.lerNumeroCameraLocal(), 640, 480, 1 )
        );
        olhoVirtual.ligar();
        desenho = olhoVirtual.getDesenho();
        desenho.setParametroTextura( 0, 0.2f );
        desenho.setParametroTextura( 1, 0.4f );
        
        smartphone = new DispositivoRemoto(
            "Smartphone", new CameraRemota( 320, 240, 1 )
        );
        smartphone.esperarEntradaRemota( bluetooth );
        desenho = smartphone.getDesenho();
        desenho.setParametroTextura( 0, 0.25f );
        desenho.setParametroTextura( 1, 0.75f );
        
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
        
        Aplicativo.lerEntradaAssincrona();
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
        for ( int i = 0; i < dispositivo.length; i++ ) {
            Dispositivo disp = dispositivo[i];
            
            disp.atualizarTextura();
            
            if ( Aplicativo.PARAMETROS[i].getAtualizado() ) {
                Desenho desenho = disp.getDesenho();
                
                for ( int n = 0; n < NUMERO_PARAMETROS_TEXTURA; n++ )
                    desenho.setParametroTextura( n, Aplicativo.PARAMETROS[i].getValor( n ) );
            }
            
            disp.draw();
            disp.atualizarImagemDetector( 3 );
            disp.getFrameBufferObject().draw( linhasCentrais );
        }
        
        tela.clear();
        olhoVirtual.getFrameBufferObject().copiar(
            tela, 0, tela.getAltura() / 2, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );      
        smartphone.getFrameBufferObject().copiar(
            tela, 0, 0, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );
        
        if ( Aplicativo.getImprimindo() )
            Aplicativo.imprimir( dispositivo );
    }
    
    private boolean executando = true;
    
    @Override
    public void dispose( GLAutoDrawable drawable ) {
        executando = false;
        
        linhasCentrais.close();
        
        for ( Dispositivo disp : dispositivo )
            disp.close();
        
        bluetooth.close();
    }
    
    public boolean getExecutando() {
        return executando;
    }
}