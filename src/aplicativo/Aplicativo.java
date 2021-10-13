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
    
    //  0.20, 0.4 - Olho virtual
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
                27.6f,
                9.5f
            }
        ),
        new Parametros(
            "Projeção",
            new String[]{
                "Canto superior esquerdo - posição x",
                "Canto superior esquerdo - posição y",
                "Lado do quadrado"
            },
            new float[] {
                0.75f,
                1.25f,
                0.25f
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
    
    private static final Object travaImpressao = new Object();
    private static boolean imprimindo = false;
    
    public static boolean getImprimindo() {
        synchronized ( travaImpressao ) {
            return imprimindo;
        }
    }
    
    private static final String apagarLinha = "\u001B[2K\r";
    private static int lista = 0;
    private static boolean ponto3D = false;
    private static int numeroLinhas = 0; 
    
    public static void imprimirPontos( Dispositivo[] dispositivo ) {
        synchronized ( travaImpressao ) {
            if ( !imprimindo )
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
                +   String.format( "%.4f",parametros[1] )
                +   ", "
                +   String.format( "%.4f",parametros[2] )
                +   ", "
                +   String.format( "%.4f",parametros[3] )
                +   ", "
                +   String.format( "%.4f",parametros[4] )
                +   ", "
                +   String.format( "%.4f",parametros[5] )
                +   " ) Erro: "
                +   String.format( "%.15f", parametros[6] )
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
        
        synchronized ( travaEstimativa ) {
            if ( !estimando )
                return;
            
            System.out.print( "\u001B[s" );
            
            for ( int i = 0; i < dispositivo.length; i++ )
                System.out.println(
                    apagarLinha + dispositivo[i].getId() + ":\t" + String.format( "%.4f", distancia[i] )
                );
            
            System.out.print( "\u001B[u" );
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
                        "(L)er parâmetros / (I)mprimir saída / (C)alibrar / E(t)imativa / (E)ncerrar: "
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
                                
                                PARAMETROS[indice].ler( ENTRADA );
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
                            
                            synchronized ( travaImpressao ) {
                                System.out.print( "\u001B[?25l" );
                                numeroLinhas = 0;
                                imprimindo = true;
                            }
                            
                            try {
                                System.in.read();
                            } catch ( IOException ignorada ) {}
                            
                            synchronized ( travaImpressao ) {
                                imprimindo = false;
                                System.out.print( "\u001B[10B\n" );
                                System.out.print( "\u001B[?25h" );
                            }
                            
                            break;
                        
                        case 'C':
                            char opcaoCalibracao;
                            
                            System.out.print( "(I)ntrínsecos / (E)xtrínsecos: " );
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