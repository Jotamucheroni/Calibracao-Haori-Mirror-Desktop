package aplicativo.opengl;

import java.nio.ByteBuffer;

/* import java.util.Deque;
import java.util.List;
import java.util.Vector; */

public class DetectorPontos implements AutoCloseable {
    private final int
        larguraImagem, alturaImagem,
        numeroComponentesCorImagem;
    
    private final ByteBuffer imagem, visImagem;
    
    // private final List<Deque<Ponto>> listaPontos = new Vector<Deque<Ponto>>( 16 );
    private final Object travaDetector = new Object();
    private final Thread detector;
    
    private final Object travaSaida = new Object();
    private int saida = 0;
    
    public DetectorPontos( int larguraImagem, int alturaImagem, int numeroComponentesCorImagem ) {
        if ( larguraImagem < 1 )
            larguraImagem = 1;
        this.larguraImagem = larguraImagem;
        
        if ( alturaImagem < 1 )
            alturaImagem = 1;
        this.alturaImagem = alturaImagem;
        
        if ( numeroComponentesCorImagem < 1 )
            numeroComponentesCorImagem = 1;
        else if ( numeroComponentesCorImagem > 4 )
            numeroComponentesCorImagem = 4;
        this.numeroComponentesCorImagem = numeroComponentesCorImagem;
        
        imagem = ByteBuffer.allocateDirect( getNumeroBytesImagem() );
        visImagem = imagem.asReadOnlyBuffer();
        
        detector = new Thread(
            () ->
            {
                int contador;
                
                do {
                    contador = 0;
                    
                    for(
                        int i = 0; i < visImagem.capacity(); i += this.numeroComponentesCorImagem
                    ) {
                        visImagem.position( i );
                        if ( Byte.toUnsignedInt( visImagem.get() ) == 255 )
                            contador++;
                    }
                    
                    synchronized( travaSaida ) {
                        saida = contador;
                    }
                    
                    synchronized( travaDetector ) {
                        try {
                            travaDetector.wait();
                        } catch ( InterruptedException ignorada ) {
                            return;
                        }
                    }
                } while ( !Thread.currentThread().isInterrupted() );
            }
        );
    }
    
    public DetectorPontos( int larguraImagem, int alturaImagem ) {
        this( larguraImagem, alturaImagem, 4 );
    }
    
    public int getLarguraImagem() {
        return larguraImagem;
    }
    
    public int getAlturaImagem() {
        return alturaImagem;
    }
    
    public int getNumeroComponentesCorImagem() {
        return numeroComponentesCorImagem;
    }
    
    public int getNumeroPixeisImagem() {
        return larguraImagem * alturaImagem;
    }
    
    public int getNumeroBytesImagem() {
        return getNumeroPixeisImagem() * numeroComponentesCorImagem;
    }
    
    public ByteBuffer getImagem() {
        if ( imagem == null )
            return null;
        
        imagem.rewind();
        
        return imagem;
    }
    
    public int getSaida() {
        synchronized ( travaSaida ) {
            return saida;
        }
    }
    
    public boolean ocupado() {
        return detector.isAlive() && detector.getState() != Thread.State.WAITING;
    }
    
    public void executar() {
        if ( ocupado() )
            return;
        
        if ( !detector.isAlive() )
            detector.start();
        else
            synchronized( travaDetector ) {
                travaDetector.notify();
            }
    }
    
    @Override
    public void close() {
        detector.interrupt();
    }
}