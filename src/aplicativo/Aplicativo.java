package aplicativo;

import java.io.IOException;

import java.util.Scanner;
import java.util.List;

import aplicativo.es.camera.CameraLocal;
import aplicativo.es.dispositivo.Dispositivo;
import aplicativo.pontos.PontoMarcador;

public class Aplicativo {
    public final static Scanner ENTRADA = new Scanner( System.in );
    private static Thread entradaAssincrona;
    //  0.2, 0.4 - Olho virtual
    //  0.15, 0.75 - Smartphone
    private final static String[] nomeParametro = {
        "Intensidade gradiente", "Ângulo gradiente",
        "Máximo de colunas à esquerda", "Máximo de colunas à direita",
        "Máximo de linhas acima", "Máximo de linhas abaixo",
        "Mínimo de pontos por coluna"
    };
    public final static Parametros[] PARAMETROS = {
        new Parametros( "Olho virtual", nomeParametro, new float[] { 0.2f, 0.4f, 1, 5, 3, 3, 3 } ),
        new Parametros( "Smartphone", nomeParametro, new float[] { 0.15f, 0.75f, 5, 5, 3, 3, 2 } ),
        new Parametros(
            "Marcador",
            new String[]{
                "Lado do quadrado (cm)",
                "Distâcia marcador - smartphone (cm)",
                "Distâcia smartphone - olho virtual (cm)"
            },
            new float[] {
                5.34f,
                32.6f,
                9.3f
            }
        ),
        new Parametros(
            "Projeção",
            new String[]{
                "Posição x",
                "Posição y",
                "Escala X",
                "Escala Y",
                "Ângulo X",
                "Ângulo Y",
                "Ângulo Z",
                "Linha",
                "Coluna"
            },
            new float[] {
                0.98f,
                1.26f,
                0.25f,
                0.25f,
                90.00f,
                90.00f,
                88.00f,
                1,
                1
            }
        )
    };
    
    public static void main( String[] args ) {
        Thread janela = new Thread( new Janela() );
        janela.start();
        
        try {
            janela.join();
        } catch ( InterruptedException ignored ) {}
        fecharEntradaAssincrona();
        ENTRADA.close();
    }
    
    public static int lerNumeroCameraLocal() {
        if ( entradaAssincrona != null )
            if ( entradaAssincrona.isAlive() )
                return -1;
        
        CameraLocal.imprimirDispositivos();
        System.out.print( "Escolha a câmera informando o índice: " );   
        
        return ENTRADA.nextInt();
    }
    
    private static final Object travaImpressaoPontos = new Object();
    private static boolean imprimindoPontos = false;
    
    public static boolean getImprimindoPontos() {
        synchronized ( travaImpressaoPontos ) {
            return imprimindoPontos;
        }
    }
    
    private static final String apagarLinha = "\u001B[2K\r";
    private static int lista = 0;
    private static boolean ponto3D = false;
    private static int numeroLinhas = 0; 
    
    public static void imprimirPontos( Dispositivo[] dispositivo ) {
        synchronized ( travaImpressaoPontos ) {
            if ( !imprimindoPontos )
                return;
            
            List<PontoMarcador> listaPontos =
                dispositivo[lista].getDetectorPontos().getListaPontosMarcador();
            
            System.out.print( "\u001B[s" );
            System.out.println( apagarLinha + "Lista de pontos: " );
            
            for ( int i = 1; i <= numeroLinhas; i++ )
                System.out.println( apagarLinha );
            
            if ( numeroLinhas > 0 )
                System.out.print( "\u001B[" + numeroLinhas + "A" );
            
            int
                linhas = 0,
                colunas = 0;
            String tabulacao = "";
            for ( PontoMarcador ponto : listaPontos ) {
                if ( ponto == null ) {
                    if ( linhas > numeroLinhas )
                        numeroLinhas = linhas;
                    
                    System.out.print( "\u001B[" + linhas + "A" );
                    colunas += 21;
                    tabulacao = "\u001B[" + colunas + "C";
                    linhas = 0;
                    
                    continue;
                }
                
                System.out.println(
                    tabulacao + ( ponto3D ? ponto.getPontoMundo() : ponto.getPontoImagem() )
                );
                
                linhas++;
            }
            
            if ( linhas > numeroLinhas )
                numeroLinhas = linhas;
            
            System.out.print( "\u001B[u" );
        }
    }
    
    private static final Object travaCalibracaoIntrinsecos = new Object();
    private static boolean calibrandoIntrinsecos = false;
    
    public static boolean getCalibrandoIntrinsecos() {
        synchronized ( travaCalibracaoIntrinsecos ) {
            return calibrandoIntrinsecos;
        }
    }
    
    public static void imprimirParametrosIntrinsecos( Dispositivo[] dispositivo ) {
        float[][] parametros = new float[dispositivo.length][];
        
        for ( int i = 0; i < dispositivo.length; i++ )
            parametros[i] = dispositivo[i].getParametrosIntrinsecosOtimos();
        
        synchronized ( travaCalibracaoIntrinsecos ) {
            if ( !calibrandoIntrinsecos )
                return;
            
            System.out.print( "\u001B[s" );
            
            for ( int i = 0; i < dispositivo.length; i++ )
                System.out.println(
                        apagarLinha
                    +   dispositivo[i].getId()
                    +   ":\t( " + String.format( "%.4f", parametros[i][0] )
                    +   ", " + String.format( "%.4f",parametros[i][1] )
                    +   " ) Erro: "
                    +   String.format( "%.15f", parametros[i][2] )
                );
            
            System.out.print( "\u001B[u" );
        }
    }
    
    private static final Object travaCalibracaoExtrinsecos = new Object();
    private static boolean calibrandoExtrinsecos = false;
    
    public static boolean getCalibrandoExtrinsecos() {
        synchronized ( travaCalibracaoExtrinsecos ) {
            return calibrandoExtrinsecos;
        }
    }
    
    public static void imprimirParametrosExtrinsecos( Dispositivo dispositivo ) {
        float[] parametros = dispositivo.getParametrosExtrinsecosOtimos();
        
        synchronized ( travaCalibracaoExtrinsecos ) {
            if ( !calibrandoExtrinsecos )
                return;
            
            System.out.print( "\u001B[s" );
            
            System.out.println(
                    apagarLinha
                +   dispositivo.getId()
                +   ":\t( "
                +   String.format( "%.4f", parametros[0] )
                +   ", "
                +   String.format( "%.4f", parametros[1] )
                +   ", "
                +   String.format( "%.4f", parametros[2] )
                +   ", "
                +   String.format( "%.4f", Math.toDegrees( parametros[3] ) )
                +   ", "
                +   String.format( "%.4f", Math.toDegrees( parametros[4] ) )
                +   ", "
                +   String.format( "%.4f", Math.toDegrees( parametros[5] ) )
                +   " ) Erro: "
                +   String.format( parametros[6] < 1 ? "%.15f" : "%.0f", parametros[6] )
            );
            
            System.out.print( "\u001B[u" );
        }
    }
    
    private static final Object travaSinal = new Object();
    private static int sinal = 0;
    
    public static int getSinal() {
        synchronized ( travaSinal ) {
            int sinalOriginal = sinal;
            sinal = 0;
            
            return sinalOriginal;
        }
    }
    
    private static final Object travaCalibracaoProjecao = new Object();
    private static boolean calibrandoProjecao = false;
    
    public static boolean getCalibrandoProjecao() {
        synchronized ( travaCalibracaoProjecao ) {
            return calibrandoProjecao;
        }
    }
    
    public static void imprimirParametrosProjecao( Dispositivo dispositivo ) {
        float[] parametros = dispositivo.getParametrosProjecaoOtimos();
        
        synchronized ( travaCalibracaoProjecao ) {
            if ( !calibrandoProjecao )
                return;
            
            System.out.print( "\u001B[s" );
            
            System.out.println(
                    apagarLinha
                +   dispositivo.getId()
                +   ":\t( "
                +   String.format( "%.4f", parametros[0] )
                +   ", "
                +   String.format( "%.4f", parametros[1] )
                +   ", "
                +   String.format( "%.4f", Math.toDegrees( parametros[2] ) )
                +   ", "
                +   String.format( "%.4f", Math.toDegrees( parametros[3] ) )
                +   ", "
                +   String.format( "%.4f", Math.toDegrees( parametros[4] ) )
                +   ", "
                +   String.format( "%.4f", parametros[5] )
                +   ", "
                +   String.format( "%.4f", parametros[6] )
                +   " ) Erro: "
                +   String.format( parametros[7] < 1 ? "%.15f" : "%.0f", parametros[7] )
            );
            
            System.out.print( "\u001B[u" );
        }
    }
    
    private static final Object travaEstimativa = new Object();
    private static boolean estimando = false;
    
    public static boolean getEstimando() {
        synchronized ( travaEstimativa ) {
            return estimando;
        }
    }
    
    public static void imprimirEstimativaDistancia( Dispositivo[] dispositivo ) {
        float[] distancia = new float[dispositivo.length];
        
        for ( int i = 0; i < dispositivo.length; i++ )
            distancia[i] = dispositivo[i].getDistanciaMarcadorEstimada();
            
        boolean testandoCalibracao;
        
        synchronized ( travaTesteCalibracao ) {
            testandoCalibracao = Aplicativo.testandoCalibracao;
        }
        
        synchronized ( travaEstimativa ) {
            if ( !estimando && !testandoCalibracao )
                return;
            
            System.out.print( "\u001B[s" );
            
            for ( int i = 0; i < dispositivo.length; i++ )
                System.out.println(
                    apagarLinha + dispositivo[i].getId() + ":\t" + String.format( "%.4f", distancia[i] )
                );
            
            System.out.print( "\u001B[u" );
        }
    }
    
    private static final Object travaTesteCalibracao = new Object();
    private static boolean testandoCalibracao = false;
    
    public static boolean getTestandoCalibracao() {
        synchronized ( travaTesteCalibracao ) {
            return testandoCalibracao;
        }
    }
    
    public static void lerEntradaAssincrona() {
        entradaAssincrona = new Thread(
            () ->
            {
                Thread linhaAtual = Thread.currentThread();
                char opcao;
                
                do {
                    System.out.print(
                            "(L)er parâmetros / (I)mprimir pontos / (C)alibrar / "
                        +   "Es(t)imar distância / Te(s)tar calibração / (E)ncerrar: "
                    );
                    opcao = ENTRADA.next().charAt( 0 );
                    
                    switch ( Character.toUpperCase( opcao ) ) {
                        case 'L':
                            if ( PARAMETROS == null ) {
                                System.out.println( "Não há parâmetros para serem lidos" );
                                
                                return;
                            }
                            
                            System.out.println();
                            System.out.println( "\u001B[1mLeitura de parâmetros iniciada\u001B[0m" );
                            System.out.println();
                            
                            if ( PARAMETROS.length == 1 )
                            {
                                PARAMETROS[0].ler( ENTRADA );
                                
                                return;
                            }
                            
                            int indice;
                            
                            do {
                                System.out.println( "Grupos de parâmetros:" );
                                
                                for ( int i = 0; i < PARAMETROS.length; i++ )
                                    System.out.println( i + " - "  + PARAMETROS[i].getId() );
                                
                                System.out.print( "Escolha o grupo pelo índice (-1 para voltar): " );
                                indice = ENTRADA.nextInt();
                                
                                if ( indice < 0 )
                                    break;
                                
                                if ( indice >= PARAMETROS.length )
                                    indice = PARAMETROS.length - 1;
                                
                                if ( indice == 3 )
                                    synchronized ( travaSinal ) {
                                        sinal = -1;
                                    }
                                PARAMETROS[indice].ler( ENTRADA );
                                if ( indice == 3 )
                                    synchronized ( travaSinal ) {
                                        sinal = -2;
                                    }
                            } while( !linhaAtual.isInterrupted() );
                            
                            break;
                        
                        case 'I':
                            System.out.print( "(O)lho virtual / (S)martphone: " );
                            lista = Character.toUpperCase( ENTRADA.next().charAt( 0 ) ) == 'O' ? 0 : 1;
                            System.out.print( "(3)D / (2)D: " );
                            ponto3D = Character.toUpperCase( ENTRADA.next().charAt( 0 ) ) == '3';
                            
                            System.out.println();
                            System.out.println( "\u001B[1mImpressão de saída iniciada\u001B[0m" );
                            System.out.println( "Pressione <Enter> para parar" );
                            System.out.println();
                            
                            synchronized ( travaImpressaoPontos ) {
                                System.out.print( "\u001B[?25l" );
                                numeroLinhas = 0;
                                imprimindoPontos = true;
                            }
                            
                            try {
                                System.in.read();
                            } catch ( IOException ignorada ) {}
                            
                            synchronized ( travaImpressaoPontos ) {
                                imprimindoPontos = false;
                                System.out.print( "\u001B[10B\n" );
                                System.out.print( "\u001B[?25h" );
                            }
                            
                            break;
                        
                        case 'C':
                            char opcaoCalibracao;
                            
                            System.out.print( "(I)ntrínsecos / (E)xtrínsecos / (P)rojecao: " );
                            opcaoCalibracao = ENTRADA.next().charAt( 0 );
                            
                            System.out.println();
                            System.out.println( "\u001B[1mCalibração iniciada\u001B[0m" );
                            System.out.println( "Pressione <Enter> para parar" );
                            System.out.println();
                            
                            switch ( Character.toUpperCase( opcaoCalibracao ) ) {
                                case 'I':
                                    synchronized ( travaCalibracaoIntrinsecos ) {
                                        System.out.print( "\u001B[?25l" );
                                        calibrandoIntrinsecos = true;
                                    }
                                    
                                    try {
                                        System.in.read();
                                    } catch ( IOException ignorada ) {}
                                    
                                    synchronized ( travaCalibracaoIntrinsecos ) {
                                        calibrandoIntrinsecos = false;
                                        System.out.print( "\u001B[3B\n" );
                                        System.out.print( "\u001B[?25h" );
                                    }
                                    
                                    break;
                                
                                case 'E':
                                    synchronized ( travaCalibracaoExtrinsecos ) {
                                        System.out.print( "\u001B[?25l" );
                                        calibrandoExtrinsecos = true;
                                    }
                                    
                                    try {
                                        System.in.read();
                                    } catch ( IOException ignorada ) {}
                                    
                                    synchronized ( travaCalibracaoExtrinsecos ) {
                                        calibrandoExtrinsecos = false;
                                        System.out.print( "\u001B[3B\n" );
                                        System.out.print( "\u001B[?25h" );
                                    }
                                    
                                    break;
                                
                                case 'P':
                                    synchronized ( travaCalibracaoProjecao ) {
                                        System.out.print( "\u001B[?25l" );
                                        calibrandoProjecao = true;
                                    }
                                    
                                    try {
                                        System.in.read();
                                    } catch ( IOException ignorada ) {}
                                    
                                    synchronized ( travaCalibracaoProjecao ) {
                                        calibrandoProjecao = false;
                                        System.out.print( "\u001B[3B\n" );
                                        System.out.print( "\u001B[?25h" );
                                    }
                                    
                                    break;
                                
                                default:
                                    System.out.println( "Entrada inválida" );
                                    break;
                            }
                            
                            break;
                        
                        case 'T':
                            System.out.println();
                            System.out.println( "\u001B[1mEstimativa de distância iniciada\u001B[0m" );
                            System.out.println( "Pressione <Enter> para parar" );
                            System.out.println();
                            
                            synchronized ( travaEstimativa ) {
                                System.out.print( "\u001B[?25l" );
                                estimando = true;
                            }
                            
                            try {
                                System.in.read();
                            } catch ( IOException ignorada ) {}
                            
                            synchronized ( travaEstimativa ) {
                                estimando = false;
                                System.out.print( "\u001B[3B\n" );
                                System.out.print( "\u001B[?25h" );
                            }
                        
                        break;
                        
                        case 'S':
                            System.out.println();
                            System.out.println( "\u001B[1mTestando calibração\u001B[0m" );
                            System.out.println( "Pressione <Enter> para parar" );
                            System.out.println();
                            
                            synchronized ( travaTesteCalibracao ) {
                                System.out.print( "\u001B[?25l" );
                                testandoCalibracao = true;
                            }
                            synchronized ( travaSinal ) {
                                sinal = -3;
                            }
                            
                            try {
                                System.in.read();
                            } catch ( IOException ignorada ) {}
                            
                            synchronized ( travaSinal ) {
                                sinal = -4;
                            }
                            synchronized ( travaTesteCalibracao ) {
                                testandoCalibracao = false;
                                System.out.print( "\u001B[3B\n" );
                                System.out.print( "\u001B[?25h" );
                            }
                            
                            break;
                        
                        case 'E':
                            return;
                        
                        default:
                            System.out.println( "Entrada inválida" );
                    }
                } while( !linhaAtual.isInterrupted() );
            }
        );
        entradaAssincrona.start();
    }
    
    public static void fecharEntradaAssincrona() {
        if ( entradaAssincrona == null )
            return;
        
        if ( entradaAssincrona.isAlive() ) {
            entradaAssincrona.interrupt();
            try {
                entradaAssincrona.join();
            } catch ( InterruptedException ignored ) {}
        }
    }
}