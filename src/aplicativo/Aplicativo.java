package aplicativo;

import java.io.IOException;
import java.util.Scanner;

import aplicativo.es.camera.CameraLocal;

public class Aplicativo {
    public final static Scanner ENTRADA = new Scanner( System.in );
    private static Thread entradaAssincrona;
    
    //  0.2, 0.4f - Olho virtual
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
    
    public static void imprimir( Object objeto ) {
        synchronized ( travaImpressao ) {
            if ( !imprimindo )
                return;
            
            System.out.print( "\u001B[1K\rSaída: " + objeto );
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
                            System.out.println();
                            System.out.println( "\u001B[1mImpressão de saída iniciada\u001B[0m" );
                            System.out.println( "Pressione <Enter> para parar" );
                            System.out.println();
                            
                            synchronized ( travaImpressao ) {
                                imprimindo = true;
                            }
                            
                            try {
                                System.in.read();
                            } catch ( IOException ignorada ) {}
                            
                            synchronized ( travaImpressao ) {
                                imprimindo = false;
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