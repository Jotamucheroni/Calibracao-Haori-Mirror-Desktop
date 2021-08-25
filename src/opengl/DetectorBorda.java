package opengl;

import java.nio.ByteBuffer;

public class DetectorBorda implements AutoCloseable {
    private int tamanhoImagem;
    private int numeroComponentesCor;
    
    private ByteBuffer imagem;
    
    DetectorBorda( int tamanhoImagem, int numeroComponentesCor ) {
        setTamanhoImagem( tamanhoImagem );
        setNumeroComponentesCor( numeroComponentesCor );
    }
    
    DetectorBorda( int tamanhoImagem ) {
        this( tamanhoImagem, 4 );
    }
    
    public void setTamanhoImagem( int tamanhoImagem ) {
        if ( tamanhoImagem < 1 )
            tamanhoImagem = 1;
        
        this.tamanhoImagem = tamanhoImagem;
    }
    
    public void setNumeroComponentesCor( int numeroComponentesCor ) {
        if ( numeroComponentesCor < 1 )
            numeroComponentesCor = 1;
        else if ( numeroComponentesCor > 4 )
            numeroComponentesCor = 4;
        
        this.numeroComponentesCor = numeroComponentesCor;
    }
    
    public ByteBuffer getImagem() {
        imagem.rewind();
        
        return imagem;
    }
    
    public void alocar() {
        imagem = ByteBuffer.allocateDirect( tamanhoImagem );
    }
    
    private Object sincronizador = new Object();
    public int saida = 0;
    
    private Thread detector = new Thread(
        () ->
        {
            int contador;
            
            while ( true ) {
                contador = 0;
                for( int i = 0; i < tamanhoImagem; i += numeroComponentesCor ) {
                    imagem.position( i );
                    if ( Byte.toUnsignedInt( imagem.get() ) == 255 )
                        contador++;
                }
                saida = contador;
                
                synchronized( sincronizador ) {
                    try {
                        sincronizador.wait();
                    } catch ( InterruptedException e ) {
                        return;
                    }
                }
            }
        }
    );
    
    // Verifica se todos os parâmetros obrigatórios foram devidamente inicializados
    public boolean preparado() {
        return imagem != null;
    }
    
    // Verifica se o objeto está preparado e se não há outra execução ainda em curso
    public boolean pronto() {
        if ( !detector.isAlive() || detector.getState() == Thread.State.WAITING )
            return true;
        
        return false;
    }
    
    // Realiza a detecção de borda
    public void executar() {
        if ( !preparado() || !pronto() )
            return;
        
        if ( !detector.isAlive() )
            detector.start();
        else
            synchronized( sincronizador ) {
                sincronizador.notify();
            }
    }
    
    @Override
    public void close() {
        if ( detector.isAlive() )
            detector.interrupt();
    }
}