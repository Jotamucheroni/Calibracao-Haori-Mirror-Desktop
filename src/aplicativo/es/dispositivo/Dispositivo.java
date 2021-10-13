package aplicativo.es.dispositivo;

import java.util.List;

import aplicativo.es.camera.Camera;
import aplicativo.opengl.Programa;
import aplicativo.opengl.Desenho;
import aplicativo.opengl.Textura;
import aplicativo.opengl.framebuffer.FrameBufferObject;
import aplicativo.otimizacao.EnxameParticulas;
import aplicativo.otimizacao.EstrategiaEvolutiva;
import aplicativo.otimizacao.FuncaoParametrosExtrinsecos;
import aplicativo.otimizacao.FuncaoParametrosIntrinsecos;
import aplicativo.otimizacao.RecozimentoSimulado;
import aplicativo.pontos.DetectorPontos;
import aplicativo.pontos.Marcador;
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
    
    private Thread calibracaoParametrosIntrinsecos;
    private final Object travaParametrosIntrinsecos = new Object();
    private float
        fatorEscalaXOtimo, fatorEscalaYOtimo,
        aptidaoIntrinsecosOtima = Float.MAX_VALUE;
    
    public void calibrarParametrosIntrinsecos() {
        if ( calibracaoParametrosIntrinsecos != null && calibracaoParametrosIntrinsecos.isAlive() )
            return;
        
        calibracaoParametrosIntrinsecos = new Thread(
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
                
                synchronized ( travaParametrosIntrinsecos ) {
                    aptidaoIntrinsecosOtima = Float.MAX_VALUE;
                }
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
                    
                    synchronized ( travaParametrosIntrinsecos ) {
                        if ( aptidaoRecozimento < aptidaoIntrinsecosOtima ) {
                            aptidaoIntrinsecosOtima = aptidaoRecozimento;
                            fatorEscalaXOtimo = resultadoRecozimento[0];
                            fatorEscalaYOtimo = resultadoRecozimento[1];
                        }
                        
                        if ( aptidaoEstrategia < aptidaoIntrinsecosOtima ) {
                            aptidaoIntrinsecosOtima = aptidaoEstrategia;
                            fatorEscalaXOtimo = resultadoEstrategia[0];
                            fatorEscalaYOtimo = resultadoEstrategia[1];
                        }
                        
                        if ( aptidaoEnxame < aptidaoIntrinsecosOtima ) {
                            aptidaoIntrinsecosOtima = aptidaoEnxame;
                            fatorEscalaXOtimo = resultadoEnxame[0];
                            fatorEscalaYOtimo = resultadoEnxame[1];
                        }
                    }
                }
            }
        );
        calibracaoParametrosIntrinsecos.start();
    }
    
    public boolean getCalibrandoParametrosIntrinsecos() {
        if ( calibracaoParametrosIntrinsecos != null && calibracaoParametrosIntrinsecos.isAlive() )
            return true;
        else
            return false;
    }
    
    public float[] getParametrosIntrinsecosOtimos() {
        synchronized ( travaParametrosIntrinsecos ) {
            return new float[]{ fatorEscalaXOtimo, fatorEscalaYOtimo, aptidaoIntrinsecosOtima };
        }
    }
    
    public float getFatorEscalaXOtimo() {
        synchronized ( travaParametrosIntrinsecos ) {
            return fatorEscalaXOtimo;
        }
    }
    
    public float getFatorEscalaYOtimo() {
        synchronized ( travaParametrosIntrinsecos ) {
            return fatorEscalaYOtimo;
        }
    }
    
    public float getAptidaoIntrinsecosOtima() {
        synchronized ( travaParametrosIntrinsecos ) {
            return aptidaoIntrinsecosOtima;
        }
    }
    
    public void encerrarCalibracaoIntrinsecos() {
        if ( calibracaoParametrosIntrinsecos != null )
            calibracaoParametrosIntrinsecos.interrupt();
    }
    
    private Thread estimativaDistancia;
    private final Object travaEstimativaDistancia = new Object();
    private float distanciaMarcadorEstimada;
    
    public void estimarDistanciaMarcador() {
        if ( estimativaDistancia != null && estimativaDistancia.isAlive() )
            return;
        
        estimativaDistancia = new Thread(
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
                    
                    synchronized ( travaEstimativaDistancia ) {
                        distanciaMarcadorEstimada = zMedio;
                    }
                }
            }
        );
        estimativaDistancia.start();
    }
    
    public boolean getEstimando() {
        if ( estimativaDistancia != null && estimativaDistancia.isAlive() )
            return true;
        else
            return false;
    }
    
    public float getDistanciaMarcadorEstimada() {
        synchronized ( travaEstimativaDistancia ) {
            return distanciaMarcadorEstimada;
        }
    }
    
    public void encerrarEstimativa() {
        if ( estimativaDistancia != null )
            estimativaDistancia.interrupt();
    }
    
    private Thread calibracaoParametrosExtrinsecos;
    private final Object travaParametrosExtrinsecos = new Object();
    private float
        translacaoXOtimo, translacaoYOtimo, translacaoZOtimo,
        rotacaoXOtimo, rotacaoYOtimo, rotacaoZOtimo,
        aptidaoExtrinsecosOtima = Float.MAX_VALUE;
    private final float anguloRadiano = (float) Math.toRadians( 5 );
    
    public void calibrarParametrosExtrinsecos( Dispositivo dispositivoReferencia ) {
        if (
            dispositivoReferencia == null ||
            ( calibracaoParametrosExtrinsecos != null && calibracaoParametrosExtrinsecos.isAlive() )
        )
            return;
        
        calibracaoParametrosExtrinsecos = new Thread(
            () ->
            {
                final FuncaoParametrosExtrinsecos funcao = new FuncaoParametrosExtrinsecos();
                final int
                    linhas = 4,
                    colunas = 6; 
                
                RecozimentoSimulado recozimentoSimulado = new RecozimentoSimulado( funcao );
                EstrategiaEvolutiva estrategiaEvolutiva = new EstrategiaEvolutiva( funcao );
                EnxameParticulas enxameParticulas = new EnxameParticulas( funcao );
                float[] resultadoRecozimento, resultadoEstrategia, resultadoEnxame;
                float aptidaoRecozimento, aptidaoEstrategia, aptidaoEnxame;
                
                List<PontoMarcador> listaPontos, listaPontosReferencia;
                Marcador marcador, marcadorReferencia;
                int pontosValidos;
                
                PontoMarcador pontoMarcador, pontoMarcadorReferencia;
                Ponto3D pontoMundo, pontoMundoReferencia;
                float xMedio, yMedio, zMedio;
                float[]
                    minimo = new float[6],
                    maximo = new float[6];
                
                synchronized ( travaParametrosExtrinsecos ) {
                    aptidaoExtrinsecosOtima = Float.MAX_VALUE;
                }
                while( !Thread.currentThread().isInterrupted() ) {
                    listaPontos =
                        detectorPontos.getListaPontosMarcador();
                    listaPontosReferencia =
                        dispositivoReferencia.getDetectorPontos().getListaPontosMarcador();
                    
                    if ( listaPontos.size() < 4  || listaPontosReferencia.size() < 4 )
                        synchronized ( travaDetector ) {
                            try {
                                travaDetector.wait();
                                continue;
                            } catch ( InterruptedException e ) {
                                return;
                            }
                        }
                    
                    marcador = new Marcador( linhas, colunas, listaPontos );
                    marcadorReferencia = new Marcador( linhas, colunas, listaPontosReferencia );
                    
                    pontosValidos = 0;
                    xMedio = 0;
                    yMedio = 0;
                    zMedio = 0;
                    
                    for ( int i = 0; i < linhas; i++ )
                        for ( int j = 0; j < colunas; j++ )
                            if (
                                ( pontoMarcador = marcador.getPontoGrade( i, j ) ) != null &&
                                ( pontoMarcadorReferencia = marcadorReferencia.getPontoGrade( i, j ) ) != null
                            ) {
                                pontoMundo = pontoMarcador.getPontoMundo();
                                pontoMundoReferencia = pontoMarcadorReferencia.getPontoMundo();
                                
                                xMedio += pontoMundo.getX() - pontoMundoReferencia.getX();
                                yMedio += pontoMundo.getY() - pontoMundoReferencia.getY();
                                zMedio += pontoMundo.getZ() - pontoMundoReferencia.getZ();
                                
                                pontosValidos++;
                            }
                    
                    if ( pontosValidos == 0 )
                        continue;        
                    
                    xMedio /= pontosValidos;
                    yMedio /= pontosValidos;
                    zMedio /= pontosValidos;
                    
                    minimo[0] = xMedio - xMedio * 0.5f;
                    minimo[1] = yMedio - yMedio * 0.5f;
                    minimo[2] = zMedio - zMedio * 0.5f;
                    minimo[3] = -anguloRadiano;
                    minimo[4] = -anguloRadiano;
                    minimo[5] = -anguloRadiano;
                    maximo[0] = xMedio + xMedio * 0.5f;
                    maximo[1] = yMedio + yMedio * 0.5f;
                    maximo[2] = zMedio + zMedio * 0.5f;
                    maximo[3] = +anguloRadiano;
                    maximo[4] = +anguloRadiano;
                    maximo[5] = +anguloRadiano;
                    
                    funcao.setMarcador( marcador );
                    funcao.setMarcadorReferencia( marcadorReferencia );
                    
                    resultadoRecozimento = recozimentoSimulado.otimizar( minimo, maximo );
                    resultadoEstrategia = estrategiaEvolutiva.otimizar( minimo, maximo );
                    resultadoEnxame = enxameParticulas.otimizar( minimo, maximo );
                    
                    aptidaoRecozimento = funcao.f( resultadoRecozimento );
                    aptidaoEstrategia = funcao.f( resultadoEstrategia );
                    aptidaoEnxame = funcao.f( resultadoEnxame );
                    
                    synchronized ( travaParametrosExtrinsecos ) {
                        if ( aptidaoRecozimento < aptidaoExtrinsecosOtima ) {
                            aptidaoExtrinsecosOtima = aptidaoRecozimento;
                            translacaoXOtimo = resultadoRecozimento[0];
                            translacaoYOtimo = resultadoRecozimento[1];
                            translacaoZOtimo = resultadoRecozimento[2];
                            rotacaoXOtimo = resultadoRecozimento[3];
                            rotacaoYOtimo = resultadoRecozimento[4];
                            rotacaoZOtimo = resultadoRecozimento[5];
                        }
                        
                        if ( aptidaoEstrategia < aptidaoExtrinsecosOtima ) {
                            aptidaoExtrinsecosOtima = aptidaoEstrategia;
                            translacaoXOtimo = resultadoEstrategia[0];
                            translacaoYOtimo = resultadoEstrategia[1];
                            translacaoZOtimo = resultadoEstrategia[2];
                            rotacaoXOtimo = resultadoEstrategia[3];
                            rotacaoYOtimo = resultadoEstrategia[4];
                            rotacaoZOtimo = resultadoEstrategia[5];
                        }
                        
                        if ( aptidaoEnxame < aptidaoExtrinsecosOtima ) {
                            aptidaoExtrinsecosOtima = aptidaoEnxame;
                            translacaoXOtimo = resultadoEnxame[0];
                            translacaoYOtimo = resultadoEnxame[1];
                            translacaoZOtimo = resultadoEnxame[2];
                            rotacaoXOtimo = resultadoEnxame[3];
                            rotacaoYOtimo = resultadoEnxame[4];
                            rotacaoZOtimo = resultadoEnxame[5];
                        }
                    }
                }
            }
        );
        calibracaoParametrosExtrinsecos.start();
    }
    
    public boolean getCalibrandoParametrosExtrinsecos() {
        if ( calibracaoParametrosExtrinsecos != null && calibracaoParametrosExtrinsecos.isAlive() )
            return true;
        else
            return false;
    }
    
    public float[] getParametrosExtrinsecosOtimos() {
        synchronized ( travaParametrosExtrinsecos ) {
            return new float[]{
                translacaoXOtimo, translacaoYOtimo, translacaoZOtimo,
                rotacaoXOtimo, rotacaoYOtimo, rotacaoZOtimo,
                aptidaoExtrinsecosOtima
            };
        }
    }
    
    public float getTranslacaoXOtimo() {
        synchronized ( travaParametrosExtrinsecos ) {
            return translacaoXOtimo;
        }
    }
    
    public float getTranslacaoYOtimo() {
        synchronized ( travaParametrosExtrinsecos ) {
            return translacaoYOtimo;
        }
    }
    
    public float getTranslacaoZOtimo() {
        synchronized ( travaParametrosExtrinsecos ) {
            return translacaoZOtimo;
        }
    }
    
    public float getRotacaoXOtimo() {
        synchronized ( travaParametrosExtrinsecos ) {
            return rotacaoXOtimo;
        }
    }
    
    public float getRotacaoYOtimo() {
        synchronized ( travaParametrosExtrinsecos ) {
            return rotacaoYOtimo;
        }
    }
    
    public float getRotacaoZOtimo() {
        synchronized ( travaParametrosExtrinsecos ) {
            return rotacaoZOtimo;
        }
    }
    
    public float getAptidaoExtrinsecosOtima() {
        synchronized ( travaParametrosExtrinsecos ) {
            return aptidaoExtrinsecosOtima;
        }
    }
    
    public void encerrarCalibracaoExtrinsecos() {
        if ( calibracaoParametrosExtrinsecos != null )
            calibracaoParametrosExtrinsecos.interrupt();
    }
    
    public Marcador getMarcador( Marcador marcadorReferencia ) {
        Marcador marcador = marcadorReferencia.clone();
        
        PontoMarcador pontoMarcadorReferencia;
        Ponto3D pontoMundoReferencia;
        float
            xReferencia, yReferencia, zReferencia,
            xEstimado, yEstimado, zEstimado,
            yEstimadoAuxiliar,
            sinX, cosX,
            sinY, cosY,
            sinZ, cosZ;
        
        for ( int i = 0; i < marcador.getLinhasGrade(); i++ )
            for ( int j = 0; j < marcador.getColunasGrade(); j++ )
                if ( ( pontoMarcadorReferencia = marcadorReferencia.getPontoGrade( i, j ) ) != null ) {
                    pontoMundoReferencia = pontoMarcadorReferencia.getPontoMundo();
                    
                    xReferencia = pontoMundoReferencia.getX();
                    yReferencia = pontoMundoReferencia.getY();
                    zReferencia = pontoMundoReferencia.getZ();
                    
                    sinX = (float) Math.sin( rotacaoXOtimo ); cosX = (float) Math.cos( rotacaoXOtimo );
                    sinY = (float) Math.sin( rotacaoYOtimo ); cosY = (float) Math.cos( rotacaoYOtimo );
                    sinZ = (float) Math.sin( rotacaoZOtimo ); cosZ = (float) Math.cos( rotacaoZOtimo );
                    
                    xEstimado =  xReferencia * cosZ - yReferencia * sinZ;
                    yEstimado =  xReferencia * sinZ + yReferencia * cosZ;
                    
                    zEstimado = -xEstimado * sinY + zReferencia * cosY;
                    xEstimado =  xEstimado * cosY + zReferencia * sinY;
                    
                    yEstimadoAuxiliar = yEstimado;
                    yEstimado =  yEstimadoAuxiliar * cosX - zEstimado * sinX;
                    zEstimado =  yEstimadoAuxiliar * sinX + zEstimado * cosX;
                    
                    xEstimado += translacaoXOtimo;
                    yEstimado += translacaoYOtimo;
                    zEstimado += translacaoZOtimo;
                    
                    marcador.getPontoGrade( i, j ).getPontoMundo().setCoordenadas(
                        xEstimado, yEstimado, zEstimado
                    );
                }
        
        return marcador;
    }
    
    @Override
    public void close() {
        encerrarCalibracaoIntrinsecos();
        encerrarEstimativa();
        encerrarCalibracaoExtrinsecos();
        
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