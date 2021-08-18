import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.BorderLayout;

public class Janela extends JFrame implements Runnable {
    GLCapabilities capacidades;
    GLCanvas canvas;
    
    Janela() {
        capacidades = new GLCapabilities( GLProfile.getDefault() );
        capacidades.setRedBits( 8 );
        capacidades.setBlueBits( 8 );
        capacidades.setGreenBits( 8 );
        capacidades.setAlphaBits( 8 );
        
        canvas = new GLCanvas( capacidades );
        canvas.addGLEventListener( new RenderizadorOpenGL() );
        
        setBounds( 0, 0, 720, 360 );
        setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
        getContentPane().setLayout( new BorderLayout() );
        add( canvas );
        
        setTitle( "OpenGL" );
    }
    
    @Override
    public void run() {
        setVisible( true );
        
        while( ! Thread.interrupted() )  {
            canvas.repaint();
            try { Thread.sleep( 8 ); }  // ~120 Hz
            catch ( InterruptedException e ) { return; }
        }
    }
}