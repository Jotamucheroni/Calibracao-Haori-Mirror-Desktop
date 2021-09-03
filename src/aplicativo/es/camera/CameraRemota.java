package aplicativo.es.camera;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.io.StreamConnection;

import aplicativo.es.Bluetooth;

public class CameraRemota extends Camera implements Runnable {
    private DataInputStream entradaRemota;
    
    private final Object
        travaLigada = new Object(),
        travaEntradaRemota = new Object();
    
    public CameraRemota( DataInputStream entradaRemota, int largImg, int altImg, int numCompCor ) {
        setEntradaRemota( entradaRemota );
        setLargImg( largImg );
        setAltImg( altImg );
        setNumCompCor( numCompCor );
    }
    
    public CameraRemota( StreamConnection entradaRemota, int largImg, int altImg, int numCompCor ) {
        setEntradaRemota( entradaRemota );
        setLargImg( largImg );
        setAltImg( altImg );
        setNumCompCor( numCompCor );
    }
    
    public CameraRemota( DataInputStream entradaRemota, int largImg, int altImg ) {
        this( entradaRemota, largImg, altImg, 3 );
    }
    
    public CameraRemota( StreamConnection entradaRemota, int largImg, int altImg ) {
        this( entradaRemota, largImg, altImg, 3 );
    }
    
    public CameraRemota( int largImg, int altImg, int numCompCor ) {
        this( (DataInputStream) null, largImg, altImg, numCompCor );
    }
    
    public CameraRemota( DataInputStream entradaRemota, int numCompCor ) {
        this( entradaRemota, 640, 480, numCompCor );
    }
    
    public CameraRemota( StreamConnection entradaRemota, int numCompCor ) {
        this( entradaRemota, 640, 480, numCompCor );
    }
    
    public CameraRemota( DataInputStream entradaRemota ) {
        this( entradaRemota, 640, 480, 3 );
    }
    
    public CameraRemota( StreamConnection entradaRemota ) {
        this( entradaRemota, 640, 480, 3 );
    }
    
    public CameraRemota() {
        this( (DataInputStream) null, 640, 480, 3 );
    }
    
    public void setEntradaRemota( DataInputStream entradaRemota ) {
        synchronized( travaEntradaRemota ) {
            this.entradaRemota = entradaRemota;
        }
    }
    
    public void setEntradaRemota( StreamConnection entradaRemota ) {
        if ( entradaRemota == null ) {
            setEntradaRemota( (DataInputStream) null );
            
            return;
        }
        
        try {
            setEntradaRemota( entradaRemota.openDataInputStream() );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    Thread esperaEntradaRemota;
    
    public void esperarEntradaRemota( Bluetooth bluetooth ) {
        if ( bluetooth == null )
            return;
        
        esperaEntradaRemota = new Thread(
            () ->
            {
                setEntradaRemota( bluetooth.esperarConexao() );
                ligar();
            }
        );
        esperaEntradaRemota.start();
    }
    
    @Override
    public ByteBuffer getImagem() {
        synchronized( travaLigada ) {
            return super.getImagem();
        }
    }
    
    @Override
    public boolean getLigada() {
        synchronized( travaLigada ) {
            return super.getLigada();
        }
    }
    
    public DataInputStream getEntradaRemota() {
        synchronized( travaEntradaRemota ) {
            return entradaRemota;
        }
    }
    
    private Thread atualizaBuffer;
    
    @Override
    public void ligar() {
        synchronized( travaEntradaRemota ) {
            if ( entradaRemota == null )
                return;
        }
        
        synchronized( travaLigada ) {
            if ( ligada )
                return;
            
            if ( Thread.currentThread().isInterrupted() )
                return;
            
            setBuffer();
            atualizaBuffer = new Thread( this );
            atualizaBuffer.start();
            ligada = true;
        }
    }
    
    @Override
    public void run() {
        try {
            DataInputStream entradaRemota;
            ByteBuffer buffer;
            
            synchronized( travaEntradaRemota ) {
                entradaRemota = this.entradaRemota;
            }
            
            synchronized( travaLigada ) {
                buffer = this.buffer;
            }
            
            byte[] vetorByte = new byte[buffer.capacity()];
            
            while( !Thread.currentThread().isInterrupted() ) {
                entradaRemota.readFully( vetorByte );
                buffer.rewind();
                buffer.put( vetorByte );
            }
        } 
        catch ( IOException ignored ) {}
    }
    
    @Override
    public void desligar() {
        synchronized ( travaLigada ) {
            if ( !ligada )
                return;
            
            ligada = false;
            
            atualizaBuffer.interrupt();
        }
    }
    
    @Override
    public void close() {
        if( esperaEntradaRemota != null )
                esperaEntradaRemota.interrupt();
        desligar();
    }
}