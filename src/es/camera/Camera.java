package es.camera;

import java.nio.ByteBuffer;

public abstract class Camera implements AutoCloseable {
    private int largImg, altImg;
    private int numCompCor;
    protected ByteBuffer buffer, visBuffer;
    
    protected boolean ligada;
    
    public void setLargImg( int largImg ) {
        if ( largImg < 1 )
            largImg = 1;
        
        this.largImg = largImg;
    }
    
    public void setAltImg( int altImg ) {
        if ( altImg < 1 )
            altImg = 1;
        
        this.altImg = altImg;
    }
    
    public void setNumCompCor( int numCompCor ) {
        if ( numCompCor < 1 )
            numCompCor = 1;
        else if ( numCompCor > 4 )
            numCompCor = 4;
        
        this.numCompCor = numCompCor;
    }
    
    protected void setBuffer() {
        buffer = ByteBuffer.allocateDirect( getTamImg() );
        visBuffer = buffer.asReadOnlyBuffer();
    }
    
    public int getLargImg() {
        return largImg;
    }
    
    public int getAltImg() {
        return altImg;
    }
    
    public int getNumCompCor() {
        return numCompCor;
    }
    
    public int getTamImg() {
        return largImg * altImg * numCompCor;
    }
    
    public ByteBuffer getImagem() {
        visBuffer.rewind();

        return visBuffer;
    }
    
    public boolean ligada() {
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