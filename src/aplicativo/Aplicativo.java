package aplicativo;

import java.util.Scanner;

import aplicativo.es.camera.CameraLocal;

public class Aplicativo {
    public final static Scanner ENTRADA = new Scanner( System.in );
    private static Thread entradaAssincrona;
    
    //  0.16, 0.45f - Olho virtual
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
    
    public static void lerEntradaAssincrona() {
        if ( PARAMETROS == null )
            return;
        
        entradaAssincrona = new Thread(
            () ->
            {
                System.out.println();
                System.out.println( "\u001B[1mLeitura de parâmetros iniciada\u001B[0m" );
                System.out.println();
                
                if ( PARAMETROS.length == 1 )
                {
                    PARAMETROS[0].ler( ENTRADA );
                    
                    return;
                }
                
                Thread linhaAtual = Thread.currentThread();
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