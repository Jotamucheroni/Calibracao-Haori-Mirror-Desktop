import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;

public class Camera implements Runnable {
    private int numCamera = 0;
    private boolean ligada = false;
    private int largImg = 640;
    private int altImg = 480;
    private ByteBuffer bufferCamera;
    public ByteBuffer visBufferCamera;

    public void setCamera( int numCamera ) {
        if ( numCamera < 0 )
            return;

        String caminhoVideo = "/dev/video" + numCamera;
        while ( ! Files.exists( Paths.get( caminhoVideo ) )
                && numCamera <= 10 ) {
            // System.out.println( "Não foi possível encontrar " + caminhoVideo );
            numCamera++;
            caminhoVideo = "/dev/video" + numCamera;
        }

        this.numCamera = numCamera;
    }

    public void setDimImg( int largImg, int altImg ) {
        if ( largImg < 1 || altImg < 1 )
            return; 
        
        this.largImg = largImg;
        this.altImg = altImg;
    }

    Camera() {}

    Camera( int numCamera ) {
        setCamera( numCamera );
    }

    Camera( int largImg, int altImg ) {
        setDimImg( largImg, altImg );
    }

    Camera( int numCamera, int largImg, int altImg ) {
        setCamera( numCamera );
        setDimImg( largImg, altImg );
    }

    @Override
    public void run() {
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber( numCamera );
        grabber.setImageMode( FrameGrabber.ImageMode.GRAY );
        grabber.setImageWidth( largImg );
        grabber.setImageHeight( altImg );

        try {
            grabber.start();

            Frame imagem;           
            while( ligada ) {
                bufferCamera.rewind();
                imagem = grabber.grab();
                bufferCamera.put( (ByteBuffer) imagem.image[0] );
                imagem.close();
            }
            grabber.close();
        } catch ( Exception e )  { e.printStackTrace();
            try {
                ligada = false;
                grabber.close();
            } catch ( Exception e2 ) { e2.printStackTrace(); }
        }
    }

    private Thread exe;

    public void ligar() {
        if ( ligada )
            return;

        ligada = true;
        bufferCamera = ByteBuffer.allocateDirect( largImg * altImg );
        visBufferCamera = bufferCamera.asReadOnlyBuffer();
        exe = new Thread( this );
        exe.start();
    }

    public void desligar() {
        ligada = false;
        try { exe.join(); } 
        catch ( InterruptedException e ) { e.printStackTrace(); }
    }

    public void reiniciar() {
        desligar();
        ligar();
    }
}
