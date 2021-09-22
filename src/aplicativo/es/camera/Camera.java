package aplicativo.es.camera;

import java.nio.ByteBuffer;

public abstract class Camera implements AutoCloseable {
    private int 
        larguraImagem, alturaImagem,
        numeroComponentesCorImagem;
    protected ByteBuffer
        buffer, visBuffer;
    
    protected boolean ligada = false;
    
    public void setLargImg( int larguraImagem ) {
        if ( larguraImagem < 1 )
            larguraImagem = 1;
        
        this.larguraImagem = larguraImagem;
    }
    
    public void setAltImg( int alturaImagem ) {
        if ( alturaImagem < 1 )
            alturaImagem = 1;
        
        this.alturaImagem = alturaImagem;
    }
    
    public void setNumeroComponentesCor( int numeroComponentesCor ) {
        if ( numeroComponentesCor < 1 )
            numeroComponentesCor = 1;
        else if ( numeroComponentesCor > 4 )
            numeroComponentesCor = 4;
        
        this.numeroComponentesCorImagem = numeroComponentesCor;
    }
    
    protected void setBuffer() {
        buffer = ByteBuffer.allocateDirect( getNumeroBytesImagem() );
        visBuffer = buffer.asReadOnlyBuffer();
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
        if ( visBuffer == null )
            return null;
        
        visBuffer.rewind();
        
        return visBuffer;
    }
    
    public boolean getLigada() {
        return ligada;
    }
    
    public abstract void ligar();
    public abstract void desligar();
    
    public void reiniciar() {
        desligar();
        ligar();
    }
    
    @Override
    public void close() {
        desligar();
    }
}