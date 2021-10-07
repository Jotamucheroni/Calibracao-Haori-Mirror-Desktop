package aplicativo.es.dispositivo;

import java.util.List;

import aplicativo.es.camera.Camera;
import aplicativo.opengl.Programa;
import aplicativo.opengl.Desenho;
import aplicativo.opengl.Textura;
import aplicativo.opengl.framebuffer.FrameBufferObject;
import aplicativo.otimizacao.EnxameParticulas;
import aplicativo.otimizacao.EstrategiaEvolutiva;
import aplicativo.otimizacao.FuncaoParametrosIntrinsecos;
import aplicativo.otimizacao.RecozimentoSimulado;
import aplicativo.pontos.DetectorPontos;
import aplicativo.pontos.Ponto2D;
import aplicativo.pontos.Ponto3D;
import aplicativo.pontos.PontoMarcador;

public class Dispositivo implements AutoCloseable {
    private static int instancia = 0;
    
    private final String id;
    private final Camera camera;
    private final Textura textura;
    private final FrameBufferObject frameBufferObject;
    private final Desenho desenho;
    private final DetectorPontos detectorPontos;
    
    public Dispositivo( String id, Camera camera ) {
        if ( id == null )
            id = "Dispositivo " + instancia;
        this.id = id;
        instancia++;
        
        this.camera = camera;
        
        if ( camera != null )
            textura = new Textura(
                camera.getLarguraImagem(), camera.getAlturaImagem(),
                camera.getNumeroComponentesCorImagem()
            );
        else
            textura = null;
        
        frameBufferObject = new FrameBufferObject( Programa.MAXIMO_SAIDAS, 640, 480 );
        
        if ( textura != null )
            desenho = new Desenho( 2, 2, Desenho.getRefQuad(), Desenho.getRefElementos(), textura );
        else
            desenho = null;
        
        detectorPontos = new DetectorPontos(
            frameBufferObject.getLargura(),
            frameBufferObject.getAltura(),
            textura == null ? 4 : textura.getNumeroComponentesCor()
        );
    }
    
    public Dispositivo(
        Camera camera
    ) {
        this( null, camera );
    }
    
    public String getId() {
        return id;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public Textura getTextura() {
        return textura;
    }
    
    public FrameBufferObject getFrameBufferObject() {
        return frameBufferObject;
    }
    
    public Desenho getDesenho() {
        return desenho;
    }
    
    public DetectorPontos getDetectorPontos() {
        return detectorPontos;
    }
    
    public void ligar() {
        if ( camera == null )
            return;
        
        camera.ligar();
    }
    
    public void desligar() {
        if ( camera == null )
            return;
        
        camera.desligar();
    }
    
    public boolean getLigado() {
        if( camera == null )
            return false;
        
        return camera.getLigada();
    }
    
    public void atualizarTextura() {
        if ( textura == null || camera == null )
            return;
        
        textura.carregarImagem( camera.getImagem() );
    }
    
    public void draw() {
        if ( frameBufferObject == null )
            return;
        
        frameBufferObject.clear();
        
        if ( desenho == null )
            return;
        
        frameBufferObject.draw( desenho );
    }
    
    private final Object travaDetector = new Object();
    
    public void atualizarImagemDetector( int numeroRenderBuffer ) {
        if ( detectorPontos == null )
            return;
        
        if ( detectorPontos.ocupado() || frameBufferObject == null )
            return;
        
        synchronized ( travaDetector ) {
            if ( detectorPontos.getNumeroPontosMarcador() >= 4 )
                travaDetector.notifyAll();
        }
        
        frameBufferObject.lerRenderBuffer(
            numeroRenderBuffer,
            detectorPontos.getNumeroComponentesCorImagem(), detectorPontos.getImagem()
        );
        detectorPontos.executar();
    }
    
    public void atualizarImagemDetector() {
        atualizarImagemDetector( 1 );
    }
    
    private Thread calibracao;
    private final Object travaParametros = new Object();
    private float
        fatorEscalaXOtimo, fatorEscalaYOtimo,
        aptidaoOtima = Float.MAX_VALUE;
    
    public void calibrar() {
        if ( calibracao != null && calibracao.isAlive() )
            return;
        
        calibracao = new Thread(
            () ->
            {
                final FuncaoParametrosIntrinsecos funcao = new FuncaoParametrosIntrinsecos();
                
                RecozimentoSimulado recozimentoSimulado = new RecozimentoSimulado( funcao );
                EstrategiaEvolutiva estrategiaEvolutiva = new EstrategiaEvolutiva( funcao );
                EnxameParticulas enxameParticulas = new EnxameParticulas( funcao );
                float[] resultadoRecozimento, resultadoEstrategia, resultadoEnxame;
                float aptidaoRecozimento, aptidaoEstrategia, aptidaoEnxame;
                
                List<PontoMarcador> listaPontos;
                PontoMarcador[] vetorPontos;
                int i, xValido, yValido;
                
                Ponto3D ponto3D;
                Ponto2D ponto2D;
                float
                    x, y, z,
                    xMedio, yMedio;
                float[]
                    minimo = new float[2],
                    maximo = new float[2];
                
                while( !Thread.currentThread().isInterrupted() ) {
                    listaPontos = detectorPontos.getListaPontosMarcador();
                    
                    if ( listaPontos.size() < 4 )
                        synchronized ( travaDetector ) {
                            try {
                                travaDetector.wait();
                                continue;
                            } catch ( InterruptedException e ) {
                                return;
                            }
                        }
                    
                    i = listaPontos.size();
                    for ( PontoMarcador ponto : listaPontos )
                        if ( ponto == null )
                            i--;
                    
                    vetorPontos = new PontoMarcador[i];
                    
                    i = 0;
                    xValido = 0;
                    yValido = 0;
                    xMedio = 0;
                    yMedio = 0;
                    
                    for ( PontoMarcador ponto : listaPontos )
                        if ( ponto != null ) {
                            vetorPontos[i] = ponto;
                            
                            ponto3D = ponto.getPontoMundo();
                            ponto2D = ponto.getPontoImagem();
                            x = ponto3D.getX();
                            y = ponto3D.getY();
                            z = ponto3D.getZ();
                            
                            if ( x != 0 ) {
                                xMedio += ponto2D.getX() * z / x;
                                xValido++;
                            }
                            
                            if ( y != 0 ) {
                                yMedio += ponto2D.getY() * z / y;
                                yValido++;
                            }
                            
                            i++;
                        }
                    
                    xMedio /= xValido;
                    yMedio /= yValido;
                    
                    minimo[0] = xMedio - xMedio * 0.5f;
                    minimo[1] = yMedio - yMedio * 0.5f;
                    maximo[0] = xMedio + xMedio * 0.5f;
                    maximo[1] = yMedio + yMedio * 0.5f;
                    
                    funcao.setPontoMarcador( vetorPontos );
                    
                    resultadoRecozimento = recozimentoSimulado.otimizar( minimo, maximo );
                    resultadoEstrategia = estrategiaEvolutiva.otimizar( minimo, maximo );
                    resultadoEnxame = enxameParticulas.otimizar( minimo, maximo );
                    
                    aptidaoRecozimento = funcao.f( resultadoRecozimento );
                    aptidaoEstrategia = funcao.f( resultadoEstrategia );
                    aptidaoEnxame = funcao.f( resultadoEnxame );
                    
                    synchronized ( travaParametros ) {
                        if ( aptidaoRecozimento < aptidaoOtima ) {
                            aptidaoOtima = aptidaoRecozimento;
                            fatorEscalaXOtimo = resultadoRecozimento[0];
                            fatorEscalaYOtimo = resultadoRecozimento[1];
                        }
                        
                        if ( aptidaoEstrategia < aptidaoOtima ) {
                            aptidaoOtima = aptidaoEstrategia;
                            fatorEscalaXOtimo = resultadoEstrategia[0];
                            fatorEscalaYOtimo = resultadoEstrategia[1];
                        }
                        
                        if ( aptidaoEnxame < aptidaoOtima ) {
                            aptidaoOtima = aptidaoEnxame;
                            fatorEscalaXOtimo = resultadoEnxame[0];
                            fatorEscalaYOtimo = resultadoEnxame[1];
                        }
                    }
                }
            }
        );
        calibracao.start();
    }
    
    public boolean getCalibrando() {
        if ( calibracao != null && calibracao.isAlive() )
            return true;
        else
            return false;
    }
    
    public float[] getParametrosOtimos() {
        synchronized ( travaParametros ) {
            return new float[]{ fatorEscalaXOtimo, fatorEscalaYOtimo, aptidaoOtima };
        }
    }
    
    public float getFatorEscalaXOtimo() {
        synchronized ( travaParametros ) {
            return fatorEscalaXOtimo;
        }
    }
    
    public float getFatorEscalaYOtimo() {
        synchronized ( travaParametros ) {
            return fatorEscalaYOtimo;
        }
    }
    
    public float getAptidaoOtima() {
        synchronized ( travaParametros ) {
            return aptidaoOtima;
        }
    }
    
    public void encerrarCalibracao() {
        if ( calibracao != null )
            calibracao.interrupt();
    }
    
    private Thread estimativa;
    private final Object travaEstimativa = new Object();
    private float distanciaMarcadorEstimada;
    
    public void estimarDistanciaMarcador() {
        if ( estimativa != null && estimativa.isAlive() )
            return;
        
        estimativa = new Thread(
            () ->
            {   
                List<PontoMarcador> listaPontos;
                int zValido;
                
                Ponto3D ponto3D;
                Ponto2D ponto2D;
                float
                    x, y,
                    zMedio;
                
                while( !Thread.currentThread().isInterrupted() ) {
                    listaPontos = detectorPontos.getListaPontosMarcador();
                    
                    if ( listaPontos.size() < 4 )
                        synchronized ( travaDetector ) {
                            try {
                                travaDetector.wait();
                                continue;
                            } catch ( InterruptedException e ) {
                                return;
                            }
                        }
                    
                    zValido = 0;
                    zMedio = 0;
                    
                    for ( PontoMarcador ponto : listaPontos )
                        if ( ponto != null ) {
                            ponto3D = ponto.getPontoMundo();
                            ponto2D = ponto.getPontoImagem();
                            x = ponto2D.getX();
                            y = ponto2D.getY();
                            
                            if ( x != 0 ) {
                                zMedio += ponto3D.getX() * fatorEscalaXOtimo / x;
                                zValido++;
                            }
                            
                            if ( y != 0 ) {
                                zMedio += ponto3D.getY() * fatorEscalaYOtimo / y;
                                zValido++;
                            }
                        }
                    
                    zMedio /= zValido;
                    
                    synchronized ( travaEstimativa ) {
                        distanciaMarcadorEstimada = zMedio;
                    }
                }
            }
        );
        estimativa.start();
    }
    
    public boolean getEstimando() {
        if ( estimativa != null && estimativa.isAlive() )
            return true;
        else
            return false;
    }
    
    public float getDistanciaMarcadorEstimada() {
        synchronized ( travaEstimativa ) {
            return distanciaMarcadorEstimada;
        }
    }
    
    public void encerrarEstimativa() {
        if ( estimativa != null )
            estimativa.interrupt();
    }
    
    @Override
    public void close() {
        encerrarCalibracao();
        encerrarEstimativa();
        
        if ( detectorPontos != null )
            detectorPontos.close();
        
        if ( desenho != null )
            desenho.close();
        
        if ( frameBufferObject != null )
            frameBufferObject.close();
        
        if ( textura != null )
            textura.close();
        
        if ( camera != null )
            camera.close();
    }
}