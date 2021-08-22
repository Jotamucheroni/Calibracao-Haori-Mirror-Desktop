import javax.swing.JFrame;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import opengl.Renderizador;

public class Janela extends JFrame implements Runnable {
    GLCapabilities capacidades;
    Renderizador renderizadorOpenGL;
    GLCanvas canvas;
    
    Janela() {
        capacidades = new GLCapabilities( GLProfile.getDefault() );
        capacidades.setRedBits( 8 );
        capacidades.setBlueBits( 8 );
        capacidades.setGreenBits( 8 );
        capacidades.setAlphaBits( 8 );
        
        canvas = new GLCanvas( capacidades );
        canvas.addGLEventListener( renderizadorOpenGL = new Renderizador() );
        
        setBounds( 323, 204, 720, 360 );
        setDefaultCloseOperation( WindowConstants.DISPOSE_ON_CLOSE );
        getContentPane().setLayout( new BorderLayout() );
        add( canvas );
        
        setTitle( "Calibração Haori Mirror" );
    }
    
    @Override
    public void run() {
        setVisible( true );
        
        while( renderizadorOpenGL.getExecutando() )  {
            canvas.repaint();
            try {
                Thread.sleep( 8 );  // ~120 Hz
            }
            catch ( InterruptedException e ) {
                break;
            }
        }
    }
}