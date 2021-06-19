package es.camera;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;

public class CameraLocal extends Camera implements Runnable {
    private int numCamera;

    public CameraLocal( int numCamera, int largImg, int altImg, int numCompCor ) {
        setCamera( numCamera );
        setLargImg( largImg );
        setAltImg( altImg );
        setNumCompCor( numCompCor );
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

        String caminhoVideo = "/dev/video" + numCamera;
        while ( ! Files.exists( Paths.get( caminhoVideo ) )
                && numCamera <= 10 ) {
            numCamera++;
            caminhoVideo = "/dev/video" + numCamera;
        }

        this.numCamera = numCamera;
    }

    public int getCamera() {
        return numCamera;
    }

    private Thread atualizaBuffer;

    @Override
    public void run() {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber( numCamera );
        switch ( getNumCompCor() ) {
            case 1:
                grabber.setImageMode( FrameGrabber.ImageMode.GRAY );
                break;
            default:
                grabber.setImageMode( FrameGrabber.ImageMode.COLOR );
                break;
        }
        grabber.setImageWidth( getLargImg() );
        grabber.setImageHeight( getAltImg() );

        try {
            grabber.start();

            Frame imagem;           
            while( ligada ) {
                buffer.rewind();
                imagem = grabber.grab();
                buffer.put( (ByteBuffer) imagem.image[0] );
                imagem.close();
            }
            grabber.close();
        } catch ( Exception ignored )  {
            try {
                ligada = false;
                grabber.close();
            } catch ( Exception ignored2 ) { return; }
        }
    }

    @Override
    public void ligar() {
        if ( ligada )
            return;

        setBuffer();
        atualizaBuffer = new Thread( this );
        ligada = true;
        atualizaBuffer.start();
    }

    public void desligar() {
        if ( !ligada )
            return;
        
        ligada = false;
        try { atualizaBuffer.join(); } 
        catch ( InterruptedException ignored ) { return; }
    }
}
