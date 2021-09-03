package aplicativo;

import java.util.Scanner;

public class Aplicativo {
    public final static Scanner entrada = new Scanner( System.in );
    
    public static void main( String[] args ) {
        Thread janela = new Thread( new Janela() );
        janela.start();
        
        try {
            janela.join();
        } catch ( InterruptedException ignored ) {}
        entrada.close();
    }
}