package aplicativo;

import java.io.IOException;

import java.util.Scanner;
import java.util.List;

import aplicativo.es.camera.CameraLocal;
import aplicativo.es.dispositivo.Dispositivo;
import aplicativo.pontos.Ponto2D;

public class Aplicativo {
    public final static Scanner ENTRADA = new Scanner( System.in );
    private static Thread entradaAssincrona;
    
    //  0.20, 0.4 - Olho virtual
    //  0.25, 0.75 - Smartphone
    public final static Parametros[] PARAMETROS = new Parametros[2];
    
    public static void main( String[] args ) {
        PARAMETROS[0] = new Parametros(
            "Olho virtual", new String[]{ "Intensidade gradiente", "Ângulo gradiente" }, 2
        );
        PARAMETROS[1] = PARAMETROS[0].clone( "Smartphone" );
        
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
    
    private static Object travaImpressao = new Object();
    private static boolean imprimindo = false;
    
    public static boolean getImprimindo() {
        synchronized ( travaImpressao ) {
            return imprimindo;
        }
    }
    
    private static String apagarLinha = "\u001B[2K\r";
    private static int lista = 0;
    private static int numeroLinhas = 0; 
    
    public static void imprimir( Dispositivo[] dispositivo ) {
        synchronized ( travaImpressao ) {
            if ( !imprimindo )
                return;
            
            List<Ponto2D> listaPontos =
                dispositivo[lista].getDetectorPontos().getListaPontosAgrupados();
            
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
            for ( Ponto2D ponto : listaPontos ) {
                if ( ponto == null ) {
                    if ( linhas > numeroLinhas )
                        numeroLinhas = linhas;
                    
                    System.out.print( "\u001B[" + linhas + "A" );
                    colunas += 20;
                    tabulacao = "\u001B[" + colunas + "C";
                    linhas = 0;
                    
                    continue;
                }
                
                System.out.println( tabulacao + ponto );
                linhas++;
            }
            
            if ( linhas > numeroLinhas )
                numeroLinhas = linhas;
            
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
                    System.out.print( "(L)er parâmetros / (I)mprimir saída / (E)ncerrar: " );
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
                            System.out.print( "Lista de pontos a ser impressa: " );
                            lista = ENTRADA.nextInt();
                            
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