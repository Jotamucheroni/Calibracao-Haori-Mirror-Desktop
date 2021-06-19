package es.camera;

import java.io.DataInputStream;
import java.io.IOException;

public class CameraRemota extends Camera implements Runnable {
    private DataInputStream entradaRemota;

    public CameraRemota( DataInputStream entradaRemota, int largImg, int altImg, int numCompCor ) {
        setEntradaRemota( entradaRemota );
        setLargImg( largImg );
        setAltImg( altImg );
        setNumCompCor( numCompCor );
    }

    public CameraRemota( DataInputStream entradaRemota, int largImg, int altImg ) {
        this( entradaRemota, largImg, altImg, 3 );
    }

    public CameraRemota( int largImg, int altImg, int numCompCor ) {
        this( null, largImg, altImg, numCompCor );
    }

    public CameraRemota( DataInputStream entradaRemota, int numCompCor ) {
        this( entradaRemota, 640, 480, numCompCor );
    }

    public CameraRemota( DataInputStream entradaRemota ) {
        this( entradaRemota, 640, 480, 3 );
    }

    public CameraRemota() {
        this( null, 640, 480, 3 );
    }

    public void setEntradaRemota( DataInputStream entradaRemota ) {
        this.entradaRemota = entradaRemota;
    }

    public DataInputStream getEntradaRemota() {
        return entradaRemota;
    }

    private byte[] vetorByte;
    private Thread atualizaBuffer;

    @Override
    public void ligar() {
        if ( ligada || entradaRemota == null )
            return;
        
        setBuffer();
        vetorByte = new byte[ getLargImg() * getAltImg() * getNumCompCor() ];
        atualizaBuffer = new Thread( this );
        ligada = true;
        atualizaBuffer.start();
    }

    @Override
    public void desligar() {
        if ( !ligada )
            return;
        
        ligada = false;
        try { atualizaBuffer.join(); } 
        catch ( InterruptedException ignored ) { return; }
    }

    @Override
    public void run() {
        try {
            while( ligada ) {
                entradaRemota.readFully( vetorByte );
                buffer.rewind();
                buffer.put( vetorByte );
            }   
        } 
        catch ( IOException ignored ) {}
    }
    
}
