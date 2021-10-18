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
import aplicativo.pontos.DetectorPontos;

public class Renderizador extends OpenGL implements GLEventListener {
    private Bluetooth bluetooth;
    
    private Dispositivo olhoVirtual;
    private DispositivoRemoto smartphone;
    private Dispositivo[] dispositivo;
    
    private final int NUMERO_PARAMETROS_TEXTURA = 2;
    
    private Desenho linhasCentrais;
    
    @Override
    public void init( GLAutoDrawable drawable ) {
        gl4 = drawable.getGL().getGL4();
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );
        
        bluetooth = new Bluetooth();
        bluetooth.conectarDispositivo( "304B0745112F" );
        
        Desenho desenho;
        DetectorPontos detector;
        
        olhoVirtual = new Dispositivo(
            "Olho virtual", new CameraLocal( Aplicativo.lerNumeroCameraLocal(), 640, 480, 1 )
        );
        olhoVirtual.ligar();
        desenho = olhoVirtual.getDesenho();
        desenho.setParametroTextura( 0, 0.2f );
        desenho.setParametroTextura( 1, 0.4f );
        detector = olhoVirtual.getDetectorPontos();
        detector.setMaximoColunasAEsquerda( 1 );
        detector.setDistanciaImagem( detector.getDistanciaImagem() + 9.5f );
        
        smartphone = new DispositivoRemoto(
            "Smartphone", new CameraRemota( 320, 240, 1 )
        );
        smartphone.esperarEntradaRemota( bluetooth );
        desenho = smartphone.getDesenho();
        desenho.setParametroTextura( 0, 0.15f );
        desenho.setParametroTextura( 1, 0.75f );
        smartphone.getDetectorPontos().setMinimoPontosColuna( 2 );
        
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
    
    private int
        i,
        sinal = 0,
        sinalAnterior = 0;
    
    @Override
    public void display( GLAutoDrawable drawable ) {
        for ( i = 0; i < dispositivo.length; i++ ) {
            Dispositivo disp = dispositivo[i];
            
            disp.atualizarTextura();
            
            if ( Aplicativo.PARAMETROS[i].getAtualizado() ) {
                Desenho desenho = disp.getDesenho();
                
                int n;
                for ( n = 0; n < NUMERO_PARAMETROS_TEXTURA; n++ )
                    desenho.setParametroTextura( n, Aplicativo.PARAMETROS[i].getValor( n ) );
                
                DetectorPontos detector = disp.getDetectorPontos();
                
                detector.setMaximoColunasAEsquerda( (int) Aplicativo.PARAMETROS[i].getValor( n ) );
                detector.setMaximoColunasADireita( (int) Aplicativo.PARAMETROS[i].getValor( n + 1 ) );
                detector.setMaximoLinhasAcima( (int) Aplicativo.PARAMETROS[i].getValor( n + 2 ) );
                detector.setMaximoLinhasAbaixo( (int) Aplicativo.PARAMETROS[i].getValor( n + 3 ) );
                detector.setMinimoPontosColuna( (int) Aplicativo.PARAMETROS[i].getValor( n + 4 ) );
            }
            
            disp.draw();
            disp.atualizarImagemDetector( 3 );
            disp.getFrameBufferObject().draw( linhasCentrais );
            
            if ( Aplicativo.getCalibrandoIntrinsecos() ) {
                if ( !disp.getCalibrandoParametrosIntrinsecos() )
                    disp.calibrarParametrosIntrinsecos();
            }
            else if ( disp.getCalibrandoParametrosIntrinsecos() )
                disp.encerrarCalibracaoIntrinsecos();
            
            if ( Aplicativo.getEstimando() ) {
                if ( !disp.getEstimando() )
                    disp.estimarDistanciaMarcador();
            }
            else if ( disp.getEstimando() )
                disp.encerrarEstimativa();
        }
        
        if ( Aplicativo.getCalibrandoExtrinsecos() ) {
            if ( !olhoVirtual.getCalibrandoParametrosExtrinsecos() )
                olhoVirtual.calibrarParametrosExtrinsecos( smartphone );
        }
        else if ( olhoVirtual.getCalibrandoParametrosExtrinsecos() )
            olhoVirtual.encerrarCalibracaoExtrinsecos();
        
        if ( Aplicativo.getCalibrandoProjecao() ) {
            if ( !olhoVirtual.getCalibrandoParametrosProjecao() )
                olhoVirtual.calibrarParametrosProjecao();
        }
        else if ( olhoVirtual.getCalibrandoParametrosProjecao() )
            olhoVirtual.encerrarCalibracaoProjecao();
        
        if ( Aplicativo.PARAMETROS[i].getAtualizado() ) {
            float ladoQuadrado = Aplicativo.PARAMETROS[i].getValor( 0 );
            
            for ( Dispositivo disp : dispositivo )
                disp.getDetectorPontos().setLadoQuadradoReal( ladoQuadrado );
            
            float distanciaMarcador = Aplicativo.PARAMETROS[i].getValor( 1 );
            smartphone.getDetectorPontos().setDistanciaImagem( distanciaMarcador );
            olhoVirtual.getDetectorPontos().setDistanciaImagem( 
                distanciaMarcador + Aplicativo.PARAMETROS[i].getValor( 2 )
             );
        }
        
        if ( smartphone.getLigado() ) {
            i++;
            sinal = Aplicativo.getSinal();
            
            if ( sinal < 0 ) {
                smartphone.getCameraRemota().enviarDados( new float[]{ sinal } );
                sinalAnterior = sinal;
            }
            else if ( sinalAnterior == -1 && Aplicativo.PARAMETROS[i].getAtualizado() )
                smartphone.getCameraRemota().enviarDados(
                    new float[]{
                        Aplicativo.PARAMETROS[i].getValor( 0 ),
                        Aplicativo.PARAMETROS[i].getValor( 1 ),
                        Aplicativo.PARAMETROS[i].getValor( 2 ),
                        Aplicativo.PARAMETROS[i].getValor( 3 ),
                        Aplicativo.PARAMETROS[i].getValor( 4 ),
                        Aplicativo.PARAMETROS[i].getValor( 5 ),
                        Aplicativo.PARAMETROS[i].getValor( 6 )
                    }
                );
        }
        
        tela.clear();
        olhoVirtual.getFrameBufferObject().copiar(
            tela, 0, tela.getAltura() / 2, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );      
        smartphone.getFrameBufferObject().copiar(
            tela, 0, 0, tela.getLargura(), tela.getAltura() / 2, 3, 1
        );
        
        if ( Aplicativo.getImprimindoPontos() )
            Aplicativo.imprimirPontos( dispositivo );
        
        if ( Aplicativo.getCalibrandoIntrinsecos() )
            Aplicativo.imprimirParametrosIntrinsecos( dispositivo );
        
        if ( Aplicativo.getCalibrandoExtrinsecos() )
            Aplicativo.imprimirParametrosExtrinsecos( olhoVirtual );
        
        if ( Aplicativo.getCalibrandoProjecao() )
            Aplicativo.imprimirParametrosProjecao( olhoVirtual );    
        
        if ( Aplicativo.getEstimando() )
            Aplicativo.imprimirEstimativaDistancia( dispositivo );
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