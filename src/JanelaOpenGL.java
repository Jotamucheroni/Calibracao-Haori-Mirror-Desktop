import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.BorderLayout;

public class JanelaOpenGL extends JFrame implements Runnable {
    GLCapabilities cap;
    GLCanvas canvas;
    
    JanelaOpenGL() {
        cap = new GLCapabilities( GLProfile.getDefault() );
        cap.setRedBits( 8 );
        cap.setBlueBits( 8 );
        cap.setGreenBits( 8 );
        cap.setAlphaBits( 8 );

        canvas = new GLCanvas( cap );
        canvas.addGLEventListener( new MyGLRenderer() );
        
        setBounds( 0, 0, 720, 360 );
        setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
        getContentPane().setLayout( new BorderLayout() );
        add( canvas );

        setTitle( "OpenGL" );
    }

    @Override
    public void run() {
        setVisible( true );
        
        while( true /* ! Thread.interrupted() */ )  {
            canvas.repaint();
            try { Thread.sleep( 8 ); } 
            catch ( InterruptedException e ) { return; }
        }
    }
}
