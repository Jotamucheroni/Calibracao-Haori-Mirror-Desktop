package opengl;

import java.nio.ByteBuffer;

public class DetectorBorda implements AutoCloseable {
    private int tamImagem;
    private ByteBuffer imagem;
    private int deslocamento;
    
    DetectorBorda( int tamImagem, ByteBuffer imagem, int deslocamento ) {
        setTamImagem( tamImagem );
        setImagem( imagem );
        setDeslocamento( deslocamento );
    }
    
    DetectorBorda( int tamImagem, int deslocamento ) {
        this( tamImagem, null, deslocamento );
    }
    
    DetectorBorda( int tamImagem, ByteBuffer imagem ) {
        this( tamImagem, imagem, 1 );
    }
    
    DetectorBorda( ByteBuffer imagem, int deslocamento ) {
        this( 0, imagem, deslocamento );
    }
    
    DetectorBorda( ByteBuffer imagem ) {
        this( 0, imagem, 1 );
    }
    
    DetectorBorda( int tamImagem ) {
        this( tamImagem, null, 1 );
    }
    
    DetectorBorda() {
        this( 0, null, 1 );
    }
    
    public void setImagem( ByteBuffer imagem ) {
        this.imagem = imagem;
        
        if ( tamImagem == 0 && this.imagem != null )
            this.tamImagem = this.imagem.capacity();
    }
    
    public void setTamImagem( int tamImagem ) {
        if ( tamImagem < 0 ) {
            this.tamImagem = 0;
            return;
        }
        
        this.tamImagem = tamImagem;
    }
    
    public void setDeslocamento( int deslocamento ) {
        if ( deslocamento <= 0 ) {
            this.deslocamento = 1;
            return;
        }
        
        this.deslocamento = deslocamento;
    }
    
    private Object sinc = new Object();
    
    private Thread detector = new Thread( () ->
        {
            /* long[] t = new long[2];
            long tempoTotal; */
            
            while ( true ) {
                /* t[0] = System.currentTimeMillis();
                int soma = 0; */
                for( int i = 0; i < tamImagem; i += deslocamento ) {
                    imagem.position( i );
                    /* if ( Byte.toUnsignedInt( imagem.get() ) == 255 )
                        soma++; */
                }
                /* t[1] = System.currentTimeMillis();
                tempoTotal = t[1] - t[0];
                
                System.out.println( 
                    "Análise - Soma: " + soma + " píxel(is)\t|\tTempo: " + tempoTotal
                    + " ms\t\t|\tAnálises/s: " +  ( tempoTotal > 0 ? ( 1000 / tempoTotal ) : "+inf" ) 
                ); */
                
                synchronized( sinc ) {
                    try {
                        sinc.wait();
                    } catch ( InterruptedException e ) {
                        return;
                    }
                }
            }
        }
    );
    
    // Verifica se todos os parâmetros obrigatórios foram devidamente inicializados
    public boolean preparado() {
        return ( imagem != null && tamImagem != 0 );
    }
    
    // Verifica se o objeto está preparado e se não há outra execução ainda em curso
    public boolean pronto() {
        if ( !detector.isAlive() || detector.getState() == Thread.State.WAITING )
            return true;
        
        return false;
    }
    
    // Realiza a detecção de borda
    public void executar() {
        if ( !pronto() || !preparado() )
            return;
        
        if ( !detector.isAlive() )
            detector.start();
        else
            synchronized( sinc ) {
                sinc.notify();
            }
    }
    
    @Override
    public void close() {
        if ( detector.isAlive() )
            detector.interrupt();
    }
}