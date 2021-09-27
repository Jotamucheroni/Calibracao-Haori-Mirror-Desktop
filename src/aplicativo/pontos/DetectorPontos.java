package aplicativo.pontos;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import java.util.ArrayList;

public class DetectorPontos implements AutoCloseable {
    private final int
        larguraImagem, alturaImagem,
        numeroComponentesCorImagem;
    
    private final ByteBuffer imagem, visImagem;
    
    private final Object travaDetector = new Object();
    private final Thread detector;
    
    private final Object travaSaida = new Object();
    private List<Ponto2D> listaPontos = new ArrayList<Ponto2D>();
    
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
                final List<Ponto2D> listaPontos = new ArrayList<Ponto2D>( getNumeroPixeisImagem() );
                final int
                    par = 1 - this.larguraImagem % 2,
                    raioMaximo = ( this.larguraImagem - 1 - par ) / 2;
                final Ponto2D
                    centroImagem = new Ponto2D(
                        ( this.larguraImagem - 1 - par ) / 2,  ( this.alturaImagem - 1 - par ) / 2
                    ),
                    cantoSuperiorEsquerdo   = new Ponto2D(),
                    cantoSuperiorDireito    = new Ponto2D(),
                    cantoInferiorEsquerdo   = new Ponto2D(),
                    cantoInferiorDireito    = new Ponto2D();
                float x, y;
                
                do {
                    // long t = System.currentTimeMillis();
                    listaPontos.clear();
                    
                    System.out.print( "ponto: " );
                    
                    if ( par == 0 )
                        adicionarPonto( listaPontos, centroImagem.x, centroImagem.y );
                    
                    for( int raio = 0; raio <= raioMaximo; raio += 1 ) {
                        // System.out.println( "raio: " + raio );
                        
                        cantoSuperiorEsquerdo.setCoordenadas(
                            centroImagem.x - raio, centroImagem.y - raio
                        );
                        cantoSuperiorDireito.setCoordenadas(
                            centroImagem.x + par + raio, centroImagem.y - raio
                        );
                        cantoInferiorDireito.setCoordenadas(
                            centroImagem.x + par + raio, centroImagem.y + par + raio
                        );
                        cantoInferiorEsquerdo.setCoordenadas(
                            centroImagem.x - raio, centroImagem.y + par + raio
                        );
                        
                        for(
                            x = cantoSuperiorEsquerdo.x,
                            y = cantoSuperiorEsquerdo.y;
                            
                            x < cantoSuperiorDireito.x;
                            
                            x++
                        )
                            adicionarPonto( listaPontos, x, y );
                        
                        for( ; y < cantoInferiorDireito.y; y++ )
                            adicionarPonto( listaPontos, x, y );
                        
                        for( ; x > cantoInferiorEsquerdo.x; x-- ) 
                            adicionarPonto( listaPontos, x, y );
                        
                        for( ; y > cantoSuperiorEsquerdo.y; y-- ) 
                            adicionarPonto( listaPontos, x, y );
                    }
                    
                    /* long dif = System.currentTimeMillis() - t;
                    System.out.println( "Tempo: " + dif );
                    System.out.println( "Quadros/s: " + 1 / ( (float) dif / 1000 ) ); */
                    System.out.println();
                    
                    synchronized( travaSaida ) {
                        this.listaPontos = new ArrayList<Ponto2D>( listaPontos );
                    }
                    
                    System.out.println( listaPontos.size() );
                    for ( Ponto2D ponto : listaPontos )
                        System.out.print( ponto );
                    System.out.println();
                    
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
    
    private void adicionarPonto( List<Ponto2D> listaPontos, float x, float y ) {
        System.out.print( (int) ( y * this.larguraImagem + x ) + " " );
        visImagem.position( (int) ( y * this.larguraImagem + x ) );
        if ( Byte.toUnsignedInt( visImagem.get() ) == 255 )
            listaPontos.add( new Ponto2D( x, y ) );
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
    
    public int getNumeroPontos() {
        synchronized ( travaSaida ) {
            return listaPontos.size();
        }
    }
    
    public List<Ponto2D> getListaPontos() {
        synchronized ( travaSaida ) {
            return Collections.unmodifiableList( listaPontos );
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