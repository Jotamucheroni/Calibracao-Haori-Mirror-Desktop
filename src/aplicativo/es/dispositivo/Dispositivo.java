package aplicativo.es.dispositivo;

import java.util.List;

import aplicativo.Aplicativo;
import aplicativo.es.camera.Camera;
import aplicativo.opengl.Programa;
import aplicativo.opengl.Desenho;
import aplicativo.opengl.Textura;
import aplicativo.opengl.framebuffer.FrameBufferObject;
import aplicativo.otimizacao.EnxameParticulas;
import aplicativo.otimizacao.EstrategiaEvolutiva;
import aplicativo.otimizacao.FuncaoParametrosExtrinsecos;
import aplicativo.otimizacao.FuncaoParametrosIntrinsecos;
import aplicativo.otimizacao.FuncaoParametrosProjecao;
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
    
    // private final int numeroTestes = 1000;
    
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
                float
                    aptidaoRecozimento, aptidaoEstrategia, aptidaoEnxame;/* ,
                    aptidaoMediaRecozimento = 0,
                    aptidaoMediaEstrategia = 0,
                    aptidaoMediaEnxame = 0;
                String mediaRecozimentoFormatada, mediaEstrategiaFormatada, mediaEnxameFormatada;
                int numeroTestesAtual = 0; */
                
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
                    
                    /* aptidaoMediaRecozimento += aptidaoRecozimento;
                    aptidaoMediaEstrategia += aptidaoEstrategia;
                    aptidaoMediaEnxame += aptidaoEnxame;
                    
                    numeroTestesAtual++;
                    if ( numeroTestesAtual == numeroTestes && id.equals( "Smartphone" ) ) {
                        aptidaoMediaRecozimento /= numeroTestes;
                        aptidaoMediaEstrategia /= numeroTestes;
                        aptidaoMediaEnxame /= numeroTestes;
                        
                        mediaRecozimentoFormatada = String.format( "%.15f", aptidaoMediaRecozimento );
                        mediaEstrategiaFormatada = String.format( "%.15f", aptidaoMediaEstrategia );
                        mediaEnxameFormatada = String.format( "%.15f", aptidaoMediaEnxame );
                        
                        numeroTestesAtual = 0;
                        aptidaoMediaRecozimento = 0;
                        aptidaoMediaEstrategia = 0;
                        aptidaoMediaEnxame = 0;
                    } */
                    
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
                synchronized ( travaEstimativaDistancia ) {
                    distanciaMarcadorEstimada = 0;
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
    private final float anguloRadianoExtrinsecos = (float) Math.toRadians( 5 );
    
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
                float aptidaoRecozimento, aptidaoEstrategia, aptidaoEnxame;/* ,
                    aptidaoMediaRecozimento = 0,
                    aptidaoMediaEstrategia = 0,
                    aptidaoMediaEnxame = 0;
                String mediaRecozimentoFormatada, mediaEstrategiaFormatada, mediaEnxameFormatada;
                int numeroTestesAtual = 0; */
                
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
                    minimo[3] = -anguloRadianoExtrinsecos;
                    minimo[4] = -anguloRadianoExtrinsecos;
                    minimo[5] = -anguloRadianoExtrinsecos;
                    maximo[0] = xMedio + xMedio * 0.5f;
                    maximo[1] = yMedio + yMedio * 0.5f;
                    maximo[2] = zMedio + zMedio * 0.5f;
                    maximo[3] = +anguloRadianoExtrinsecos;
                    maximo[4] = +anguloRadianoExtrinsecos;
                    maximo[5] = +anguloRadianoExtrinsecos;
                    
                    funcao.setMarcador( marcador );
                    funcao.setMarcadorReferencia( marcadorReferencia );
                    
                    resultadoRecozimento = recozimentoSimulado.otimizar( minimo, maximo );
                    resultadoEstrategia = estrategiaEvolutiva.otimizar( minimo, maximo );
                    resultadoEnxame = enxameParticulas.otimizar( minimo, maximo );
                    
                    aptidaoRecozimento = funcao.f( resultadoRecozimento );
                    aptidaoEstrategia = funcao.f( resultadoEstrategia );
                    aptidaoEnxame = funcao.f( resultadoEnxame );
                    
                    /* aptidaoMediaRecozimento += aptidaoRecozimento;
                    aptidaoMediaEstrategia += aptidaoEstrategia;
                    aptidaoMediaEnxame += aptidaoEnxame;
                    
                    numeroTestesAtual++;
                    if ( numeroTestesAtual == numeroTestes ) {
                        aptidaoMediaRecozimento /= numeroTestes;
                        aptidaoMediaEstrategia /= numeroTestes;
                        aptidaoMediaEnxame /= numeroTestes;
                        
                        mediaRecozimentoFormatada = String.format( "%.15f", aptidaoMediaRecozimento );
                        mediaEstrategiaFormatada = String.format( "%.15f", aptidaoMediaEstrategia );
                        mediaEnxameFormatada = String.format( "%.15f", aptidaoMediaEnxame );
                        
                        numeroTestesAtual = 0;
                        aptidaoMediaRecozimento = 0;
                        aptidaoMediaEstrategia = 0;
                        aptidaoMediaEnxame = 0;
                    } */
                    
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
    
    private Thread calibracaoParametrosProjecao;
    private final Object travaParametrosProjecao = new Object();
    private float
        escalaProjecaoX, escalaProjecaoY,
        rotacaoTelaX, rotacaoTelaY, rotacaoTelaZ,
        translacaoTelaX, translacaoTelaY,
        aptidaoProjecaoOtima = Float.MAX_VALUE;
    private final float anguloRadianoProjecao = (float) Math.toRadians( 5 );
    public static final float EXPOENTE_ZX = 1.5f;
    public static final float EXPOENTE_ZY = 1.5f;
    
    public void calibrarParametrosProjecao() {
        if ( calibracaoParametrosProjecao != null && calibracaoParametrosProjecao.isAlive() )
            return;
        
        calibracaoParametrosProjecao = new Thread(
            () ->
            {
                final FuncaoParametrosProjecao funcao = new FuncaoParametrosProjecao();
                final int
                    linhas = 4,
                    colunas = 6; 
                
                RecozimentoSimulado recozimentoSimulado = new RecozimentoSimulado( funcao );
                EstrategiaEvolutiva estrategiaEvolutiva = new EstrategiaEvolutiva( funcao );
                EnxameParticulas enxameParticulas = new EnxameParticulas( funcao );
                float[] resultadoRecozimento, resultadoEstrategia, resultadoEnxame;
                float aptidaoRecozimento, aptidaoEstrategia, aptidaoEnxame;/* ,
                    aptidaoMediaRecozimento = 0,
                    aptidaoMediaEstrategia = 0,
                    aptidaoMediaEnxame = 0;
                String mediaRecozimentoFormatada, mediaEstrategiaFormatada, mediaEnxameFormatada;
                int numeroTestesAtual = 0; */
                
                List<PontoMarcador> listaPontos;
                Marcador marcador;
                int linha, coluna;
                
                Ponto3D
                    superiorEsquerdo3D, superiorDireito3D, inferiorDireito3D, inferiorEsquerdo3D,
                    pontoMundo;
                Ponto2D
                    superiorEsquerdo2D = new Ponto2D(),
                    superiorDireito2D = new Ponto2D(),
                    inferiorDireito2D = new Ponto2D(),
                    inferiorEsquerdo2D = new Ponto2D(),
                    pontoTela;
                Ponto2D[] ponto2D;
                float
                    escalaXQuadrado, escalaYQuadrado,
                    superiorEsquerdoX, superiorEsquerdoY,
                    rotacaoX, rotacaoY, rotacaoZ,
                    z,
                    zxn, zxnM1,
                    zyn, zynM1,
                    escalaXMedia, escalaYMedia,
                    translacaoXMedia, translacaoYMedia;
                float[]
                    minimo = new float[7],
                    maximo = new float[7];
                
                synchronized ( travaParametrosProjecao ) {
                    aptidaoProjecaoOtima = Float.MAX_VALUE;
                }
                while( !Thread.currentThread().isInterrupted() ) {
                    listaPontos =
                        detectorPontos.getListaPontosMarcador();
                    
                    if ( listaPontos.size() < 4 )
                        synchronized ( travaDetector ) {
                            try {
                                travaDetector.wait();
                                continue;
                            } catch ( InterruptedException e ) {
                                return;
                            }
                        }
                    
                    marcador = new Marcador( linhas, colunas, listaPontos );
                    
                    linha   =   (int) Aplicativo.PARAMETROS[3].getValor( 7 );
                    coluna  =   (int) Aplicativo.PARAMETROS[3].getValor( 8 );
                    
                    try {
                        superiorEsquerdo3D =
                            marcador.getPontoGrade( linha, coluna + 1 ).getPontoMundo();
                        superiorDireito3D =
                            marcador.getPontoGrade( linha, coluna ).getPontoMundo();
                        inferiorDireito3D =
                            marcador.getPontoGrade( linha + 1, coluna ).getPontoMundo();
                        inferiorEsquerdo3D =
                            marcador.getPontoGrade( linha + 1, coluna + 1 ).getPontoMundo();
                    }
                    catch ( NullPointerException e ) {
                        continue;
                    }
                    
                    superiorEsquerdoX = Aplicativo.PARAMETROS[3].getValor( 0 ) - 1;
                    superiorEsquerdoY = Aplicativo.PARAMETROS[3].getValor( 1 ) - 1;
                    escalaXQuadrado = Aplicativo.PARAMETROS[3].getValor( 2 );
                    escalaYQuadrado = Aplicativo.PARAMETROS[3].getValor( 3 );
                    rotacaoX = (float) Math.toRadians( Aplicativo.PARAMETROS[3].getValor( 4 ) - 90 );
                    rotacaoY = (float) Math.toRadians( Aplicativo.PARAMETROS[3].getValor( 5 ) - 90 );
                    rotacaoZ = (float) Math.toRadians( Aplicativo.PARAMETROS[3].getValor( 6 ) - 90 );
                    
                    superiorEsquerdo2D.setCoordenadas( -1, 1 );
                    superiorDireito2D.setCoordenadas( 1, 1 );
                    inferiorDireito2D.setCoordenadas( 1, -1 );
                    inferiorEsquerdo2D.setCoordenadas( -1, -1 );
                    
                    ponto2D = new Ponto2D[] {
                        superiorEsquerdo2D, superiorDireito2D, inferiorDireito2D, inferiorEsquerdo2D
                    };
                    
                    for ( Ponto2D ponto : ponto2D ) {
                        ponto.escalar( escalaXQuadrado, escalaYQuadrado );
                        ponto.rotacionar( rotacaoX, rotacaoY, rotacaoZ );
                        ponto.transladar(
                            escalaXQuadrado + superiorEsquerdoX,
                            -escalaYQuadrado + superiorEsquerdoY
                        );
                    }
                    
                    escalaXMedia = 0;
                    escalaYMedia = 0;
                    
                    z = superiorEsquerdo3D.getZ();
                    zxn  = (float) Math.pow( z, EXPOENTE_ZX );
                    zxnM1 = (float) Math.pow( z, EXPOENTE_ZX - 1 );
                    zyn  = (float) Math.pow( z, EXPOENTE_ZY );
                    zynM1 = (float) Math.pow( z, EXPOENTE_ZY - 1 );
                    
                    escalaXMedia += Math.sqrt(
                        ( z * z ) * (float) (
                                Math.pow( superiorEsquerdo2D.getX() - inferiorDireito2D.getX(), 2 )
                            +   Math.pow( superiorEsquerdo2D.getY() - inferiorDireito2D.getY(), 2 )
                        ) / (float) (
                                Math.pow( superiorEsquerdo3D.getX() - inferiorDireito3D.getX(), 2 )
                            +   Math.pow( superiorEsquerdo3D.getY() - inferiorDireito3D.getY(), 2 )
                        )
                    );
                    escalaXMedia += Math.sqrt(
                        ( z * z ) * (float) (
                                Math.pow( inferiorEsquerdo2D.getX() - superiorDireito2D.getX(), 2 )
                            +   Math.pow( inferiorEsquerdo2D.getY() - superiorDireito2D.getY(), 2 )
                        ) / (float) (
                                Math.pow( inferiorEsquerdo3D.getX() - superiorDireito3D.getX(), 2 )
                            +   Math.pow( inferiorEsquerdo3D.getY() - superiorDireito3D.getY(), 2 )
                        )
                    );
                    escalaXMedia /= 2;
                    escalaYMedia = escalaXMedia;
                    
                    Ponto3D[] ponto3D = {
                        superiorEsquerdo3D, superiorDireito3D, inferiorDireito3D, inferiorEsquerdo3D
                    };
                    
                    translacaoXMedia = 0;
                    translacaoYMedia = 0;
                    
                    for ( int i = 0; i < ponto3D.length; i++ ) {
                        pontoMundo = ponto3D[i];
                        pontoTela = ponto2D[i];
                        
                        translacaoXMedia += pontoTela.getX() * zxn - pontoMundo.getX() * escalaXMedia * zxnM1;
                        translacaoYMedia += pontoTela.getY() * zyn - pontoMundo.getY() * escalaYMedia * zynM1;
                    }
                    
                    translacaoXMedia /= ponto3D.length;
                    translacaoYMedia /= ponto3D.length;
                    
                    minimo[0] = escalaXMedia - escalaXMedia * 0.5f;
                    minimo[1] = escalaYMedia - escalaYMedia * 0.5f;
                    minimo[2] = -anguloRadianoProjecao;
                    minimo[3] = -anguloRadianoProjecao;
                    minimo[4] = -anguloRadianoProjecao;
                    minimo[5] = translacaoXMedia - translacaoXMedia * 0.5f;
                    minimo[6] = translacaoYMedia - translacaoYMedia * 0.5f;
                    
                    maximo[0] = escalaXMedia + escalaXMedia * 0.5f;
                    maximo[1] = escalaYMedia + escalaYMedia * 0.5f;
                    maximo[2] = +anguloRadianoProjecao;
                    maximo[3] = +anguloRadianoProjecao;
                    maximo[4] = +anguloRadianoProjecao;
                    maximo[5] = translacaoXMedia + translacaoXMedia * 0.5f;
                    maximo[6] = translacaoYMedia + translacaoYMedia * 0.5f;
                    
                    funcao.setPontos3D(
                        superiorEsquerdo3D, superiorDireito3D, inferiorDireito3D, inferiorEsquerdo3D
                    );
                    funcao.setPontos2D(
                        superiorEsquerdo2D, superiorDireito2D, inferiorDireito2D, inferiorEsquerdo2D
                    );
                    
                    resultadoRecozimento = recozimentoSimulado.otimizar( minimo, maximo );
                    resultadoEstrategia = estrategiaEvolutiva.otimizar( minimo, maximo );
                    resultadoEnxame = enxameParticulas.otimizar( minimo, maximo );
                    
                    aptidaoRecozimento = funcao.f( resultadoRecozimento );
                    aptidaoEstrategia = funcao.f( resultadoEstrategia );
                    aptidaoEnxame = funcao.f( resultadoEnxame );
                    
                    /* aptidaoMediaRecozimento += aptidaoRecozimento;
                    aptidaoMediaEstrategia += aptidaoEstrategia;
                    aptidaoMediaEnxame += aptidaoEnxame;
                    
                    numeroTestesAtual++;
                    if ( numeroTestesAtual == numeroTestes ) {
                        aptidaoMediaRecozimento /= numeroTestes;
                        aptidaoMediaEstrategia /= numeroTestes;
                        aptidaoMediaEnxame /= numeroTestes;
                        
                        mediaRecozimentoFormatada = String.format( "%.15f", aptidaoMediaRecozimento );
                        mediaEstrategiaFormatada = String.format( "%.15f", aptidaoMediaEstrategia );
                        mediaEnxameFormatada = String.format( "%.15f", aptidaoMediaEnxame );
                        
                        numeroTestesAtual = 0;
                        aptidaoMediaRecozimento = 0;
                        aptidaoMediaEstrategia = 0;
                        aptidaoMediaEnxame = 0;
                    } */
                    
                    synchronized ( travaParametrosProjecao ) {
                        if ( aptidaoRecozimento < aptidaoProjecaoOtima ) {
                            aptidaoProjecaoOtima = aptidaoRecozimento;
                            escalaProjecaoX = resultadoRecozimento[0];
                            escalaProjecaoY = resultadoRecozimento[1];
                            rotacaoTelaX = resultadoRecozimento[2];
                            rotacaoTelaY = resultadoRecozimento[3];
                            rotacaoTelaZ = resultadoRecozimento[4];
                            translacaoTelaX = resultadoRecozimento[5];
                            translacaoTelaY = resultadoRecozimento[6];
                        }
                        
                        if ( aptidaoEstrategia < aptidaoProjecaoOtima ) {
                            aptidaoProjecaoOtima = aptidaoEstrategia;
                            escalaProjecaoX = resultadoEstrategia[0];
                            escalaProjecaoY = resultadoEstrategia[1];
                            rotacaoTelaX = resultadoEstrategia[2];
                            rotacaoTelaY = resultadoEstrategia[3];
                            rotacaoTelaZ = resultadoEstrategia[4];
                            translacaoTelaX = resultadoEstrategia[5];
                            translacaoTelaY = resultadoEstrategia[6];
                        }
                        
                        if ( aptidaoEnxame < aptidaoProjecaoOtima ) {
                            aptidaoProjecaoOtima = aptidaoEnxame;
                            escalaProjecaoX = resultadoEnxame[0];
                            escalaProjecaoY = resultadoEnxame[1];
                            rotacaoTelaX = resultadoEnxame[2];
                            rotacaoTelaY = resultadoEnxame[3];
                            rotacaoTelaZ = resultadoEnxame[4];
                            translacaoTelaX = resultadoEnxame[5];
                            translacaoTelaY = resultadoEnxame[6];
                        }
                    }
                }
            }
        );
        calibracaoParametrosProjecao.start();
    }
    
    public boolean getCalibrandoParametrosProjecao() {
        if ( calibracaoParametrosProjecao != null && calibracaoParametrosProjecao.isAlive() )
            return true;
        else
            return false;
    }
    
    public float[] getParametrosProjecaoOtimos() {
        synchronized ( travaParametrosProjecao ) {
            return new float[]{
                escalaProjecaoX, escalaProjecaoY,
                rotacaoTelaX, rotacaoTelaY, rotacaoTelaZ,
                translacaoTelaX, translacaoTelaY,
                aptidaoProjecaoOtima
            };
        }
    }
    
    public float getEscalaProjecaoX() {
        synchronized ( travaParametrosProjecao ) {
            return escalaProjecaoX;
        }
    }
    
    public float getEscalaProjecaoY() {
        synchronized ( travaParametrosProjecao ) {
            return escalaProjecaoY;
        }
    }
    
    public float getRotacaoTelaX() {
        synchronized ( travaParametrosProjecao ) {
            return rotacaoTelaX;
        }
    }
    
    public float getRotacaoTelaY() {
        synchronized ( travaParametrosProjecao ) {
            return rotacaoTelaY;
        }
    }
    
    public float getRotacaoTelaZ() {
        synchronized ( travaParametrosProjecao ) {
            return rotacaoTelaZ;
        }
    }
    
    public float getTranslacaoTelaX() {
        synchronized ( travaParametrosProjecao ) {
            return translacaoTelaX;
        }
    }
    
    public float getTranslacaoTelaY() {
        synchronized ( travaParametrosProjecao ) {
            return translacaoTelaY;
        }
    }
    
    public float getAptidaoProjecaoOtima() {
        synchronized ( travaParametrosProjecao ) {
            return aptidaoProjecaoOtima;
        }
    }
    
    public void encerrarCalibracaoProjecao() {
        if ( calibracaoParametrosProjecao != null )
            calibracaoParametrosProjecao.interrupt();
    }
    
    private Thread testeCalibracao;
    private final Object travaTesteCalibracao = new Object();
    private float posicaoTesteX, posicaoTesteY, posicaoTesteZ;
    
    public void testarCalibracao( Dispositivo dispositivoReferencia ) {
        if ( testeCalibracao != null && testeCalibracao.isAlive() )
            return;
        
        testeCalibracao = new Thread(
            () ->
            {
                final int
                    linhas = 4,
                    colunas = 6; 
                
                List<PontoMarcador> listaPontos;
                Marcador marcador;
                Ponto3D superiorEsquerdo, superiorDireito, inferiorDireito, inferiorEsquerdo;
                float
                    mediaX, mediaY, mediaZ,
                    posicaoZ;
                
                while( !Thread.currentThread().isInterrupted() ) {
                    dispositivoReferencia.estimarDistanciaMarcador();
                    listaPontos =
                        dispositivoReferencia.getDetectorPontos().getListaPontosMarcador();
                    posicaoZ = dispositivoReferencia.getDistanciaMarcadorEstimada();
                    
                    if ( listaPontos.size() < 4 )
                        synchronized ( travaDetector ) {
                            try {
                                travaDetector.wait();
                                continue;
                            } catch ( InterruptedException e ) {
                                return;
                            }
                        }
                    
                    for ( PontoMarcador ponto : listaPontos )
                        if ( ponto != null )
                            ponto.getPontoMundo().setZ( posicaoZ );
                    
                    marcador = getMarcador( new Marcador( linhas, colunas, listaPontos ) );
                    
                    try {
                        superiorEsquerdo = marcador.getPontoGrade( 1, 2 ).getPontoMundo();
                        superiorDireito = marcador.getPontoGrade( 1, 1 ).getPontoMundo();
                        inferiorDireito = marcador.getPontoGrade( 2, 1 ).getPontoMundo();
                        inferiorEsquerdo = marcador.getPontoGrade( 2, 2 ).getPontoMundo();
                    }
                    catch ( NullPointerException e ) {
                        continue;
                    }
                    
                    mediaX = 0;
                    mediaY = 0;
                    mediaZ = 0;
                    
                    mediaX += ( superiorEsquerdo.getX() + superiorDireito.getX() ) / 2;
                    mediaX += ( inferiorEsquerdo.getX() + inferiorDireito.getX() ) / 2;
                    mediaX /= 2;
                    
                    mediaY += ( superiorEsquerdo.getY() + inferiorEsquerdo.getY() ) / 2;
                    mediaY += ( superiorDireito.getY() + inferiorDireito.getY() ) / 2;
                    mediaY /= 2;
                    
                    mediaZ += superiorEsquerdo.getZ();
                    mediaZ += superiorDireito.getZ();
                    mediaZ += inferiorDireito.getZ();
                    mediaZ += inferiorEsquerdo.getZ();
                    mediaZ /= 4;
                    
                    synchronized ( travaTesteCalibracao ) {
                        posicaoTesteX = mediaX;
                        posicaoTesteY = mediaY;
                        this.posicaoTesteZ = mediaZ;
                    }
                }
            }
        );
        testeCalibracao.start();
    }
    
    public boolean getTestandoCalibracao() {
        if ( testeCalibracao != null && testeCalibracao.isAlive() )
            return true;
        else
            return false;
    }
    
    public float[] getPosicaoTeste() {
        synchronized ( travaTesteCalibracao ) {
            return new float[]{
                posicaoTesteX, posicaoTesteY, posicaoTesteZ
            };
        }
    }
    
    public float getPosicaoX() {
        synchronized ( travaTesteCalibracao ) {
            return posicaoTesteX;
        }
    }
    
    public float getPosicaoY() {
        synchronized ( travaTesteCalibracao ) {
            return posicaoTesteY;
        }
    }
    
    public float getPosicaoZ() {
        synchronized ( travaTesteCalibracao ) {
            return posicaoTesteZ;
        }
    }
    
    public void encerrarTesteCalibracao() {
        if ( testeCalibracao != null )
            testeCalibracao.interrupt();
    }
    
    @Override
    public void close() {
        encerrarCalibracaoIntrinsecos();
        encerrarEstimativa();
        encerrarCalibracaoExtrinsecos();
        encerrarCalibracaoProjecao();
        encerrarTesteCalibracao();
        
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