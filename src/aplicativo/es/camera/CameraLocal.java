package aplicativo.es.camera;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;

public class CameraLocal extends Camera implements Runnable {
    private int numCamera;
    private OpenCVFrameGrabber grabber;
    
    public static void imprimirDispositivos() {
        String nomeArquivoDispositivo;
        
        System.out.println( "Dispositivos de vídeo disponíveis: " );        
        for ( int i = 0; i <= 10; i++ ) {
            nomeArquivoDispositivo = "/dev/video" + i;
            
            if ( Files.exists( Paths.get( nomeArquivoDispositivo ) ) ) {
                System.out.println( i + " - " + nomeArquivoDispositivo );
                i++;
            }
        }
    }
    
    public CameraLocal( int numCamera, int largImg, int altImg, int numCompCor ) {
        setCamera( numCamera );
        setLargImg( largImg );
        setAltImg( altImg );
        setNumeroComponentesCor( numCompCor );
    }
    
    public CameraLocal( int numCamera, int largImg, int altImg ) {
        this( numCamera, largImg, altImg, 3 );
    }
    
    public CameraLocal( int numCamera, int numCompCor ) {
        this( numCamera, 640, 480, numCompCor );
    }
    
    public CameraLocal( int numCamera ) {
        this( numCamera, 640, 480, 3 );
    }
    
    public CameraLocal() {
        this( 0, 640, 480, 3 );
    }
    
    public void setCamera( int numCamera ) {
        if ( numCamera < 0 )
            numCamera = 0;
        
        this.numCamera = numCamera;
        grabber = new OpenCVFrameGrabber( numCamera );
    }
    
    public int getCamera() {
        return numCamera;
    }
    
    private Thread atualizaBuffer;
    
    @Override
    public void run() {
        try {
            grabber.start();
            
            Frame imagem;           
            while( !Thread.currentThread().isInterrupted() ) {
                buffer.rewind();
                imagem = grabber.grab();
                buffer.put( (ByteBuffer) imagem.image[0] );
                imagem.close();
            }
            grabber.stop();
        } catch ( Exception ignored )  {
            try {
                ligada = false;
                grabber.stop();
            } catch ( Exception ignored2 ) {
                return;
            }
        }
    }
    
    @Override
    public void ligar() {
        if ( ligada )
            return;
        
        setBuffer();
        
        switch ( getNumeroComponentesCorImagem() ) {
            case 1:
                grabber.setImageMode( FrameGrabber.ImageMode.GRAY );
                break;
            default:
                grabber.setImageMode( FrameGrabber.ImageMode.COLOR );
                break;
        }
        grabber.setImageWidth( getLarguraImagem() );
        grabber.setImageHeight( getAlturaImagem() );
        
        atualizaBuffer = new Thread( this );
        atualizaBuffer.start();
        ligada = true;
    }
    
    public void desligar() {
        if ( !ligada )
            return;
        
        ligada = false;
        atualizaBuffer.interrupt();
        try {
            atualizaBuffer.join();
        } catch ( InterruptedException ignored ) {}
    }
    
    @Override
    public void close() {
        desligar();
        try {
            grabber.close();
        } catch ( Exception ignored ) {}
    }
}