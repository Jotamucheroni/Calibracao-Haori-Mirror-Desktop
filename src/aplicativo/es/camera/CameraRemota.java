package aplicativo.es.camera;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.microedition.io.StreamConnection;

import aplicativo.es.Bluetooth;

public class CameraRemota extends Camera implements Runnable {
    private DataInputStream entradaRemota;
    private DataOutputStream saidaRemota;
    
    private final Object
        travaLigada = new Object(),
        travaEntradaRemota = new Object(),
        travaSaidaRemota = new Object();
    
    public CameraRemota( DataInputStream entradaRemota, int largImg, int altImg, int numCompCor ) {
        setEntradaRemota( entradaRemota );
        setLargImg( largImg );
        setAltImg( altImg );
        setNumeroComponentesCor( numCompCor );
    }
    
    public CameraRemota( StreamConnection entradaRemota, int largImg, int altImg, int numCompCor ) {
        setEntradaRemota( entradaRemota );
        setLargImg( largImg );
        setAltImg( altImg );
        setNumeroComponentesCor( numCompCor );
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
    
    public void setEntradaRemota( StreamConnection conexaoRemota ) {
        if ( conexaoRemota == null ) {
            setEntradaRemota( (DataInputStream) null );
            
            return;
        }
        
        try {
            setEntradaRemota( conexaoRemota.openDataInputStream() );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    public void setSaidaRemota( DataOutputStream saidaRemota ) {
        synchronized( travaSaidaRemota ) {
            this.saidaRemota = saidaRemota;
        }
    }
    
    public void setSaidaRemota( StreamConnection conexaoRemota ) {
        if ( conexaoRemota == null ) {
            setSaidaRemota( (DataOutputStream) null );
            
            return;
        }
        
        try {
            setSaidaRemota( conexaoRemota.openDataOutputStream() );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    Thread esperaConexaoRemota;
    
    public void esperarConexaoRemota( Bluetooth bluetooth ) {
        if ( bluetooth == null )
            return;
        
        esperaConexaoRemota = new Thread(
            () ->
            {
                StreamConnection conexao = bluetooth.esperarConexao();
                setEntradaRemota( conexao );
                setSaidaRemota( conexao );
                ligar();
            }
        );
        esperaConexaoRemota.start();
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
    
    public DataOutputStream getSaidaRemota() {
        synchronized( travaSaidaRemota ) {
            return saidaRemota;
        }
    }
    
    private Thread atualizaBuffer;
    
    @Override
    public void ligar() {
        synchronized( travaEntradaRemota ) {
            if ( entradaRemota == null )
                return;
        }
        
        synchronized( travaSaidaRemota ) {
            if ( saidaRemota == null )
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
    
    public void enviarDados( float[] vetorDados ) {
        synchronized ( travaSaidaRemota ) {
            if ( saidaRemota == null )
                return;
            
            try {
                for ( float dado : vetorDados )
                    saidaRemota.writeFloat( dado );
            } catch ( IOException ignored ) {}
        }
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
        if( esperaConexaoRemota != null )
                esperaConexaoRemota.interrupt();
        desligar();
    }
}