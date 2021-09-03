package aplicativo.opengl;

import java.util.logging.Logger;
import java.util.Arrays;

import com.jogamp.opengl.GL4;

public class Config extends OpenGL {
    private static Logger log = Logger.getLogger( Config.class.getName() );
    
    public static void imprimir( int[] configs ) {
        final int[] valConfig =  new int[1];
        
        for ( int config : configs ) {
            gl4.glGetIntegerv( config, valConfig, 0 );
            log.info( "OpenGL Config.: " + valConfig[0] );
        }
    }
    
    public static void imprimir( int[] configs, String[] nomesConfigs ) {
        final int[] valConfig =  new int[1];
        
        for ( int i = 0; i < configs.length; i++ ) {
            gl4.glGetIntegerv( configs[i], valConfig, 0 );
            log.info( "OpenGL Config.: " + nomesConfigs[i] + ": " + valConfig[0] );
        }
    }
    
    public static void imprimir( int[] configs, int[] configNumComp ) {
        final int[] valConfig =  new int[Arrays.stream( configNumComp ).max().getAsInt()];
        
        for ( int i = 0; i < configs.length; i++ ) {
            gl4.glGetIntegerv( configs[i], valConfig, 0 );
            
            StringBuilder info = new StringBuilder( Integer.toString( valConfig[0] ) );
            for ( int j = 1; j < configNumComp[i]; j++ )
                info.append(", ").append( valConfig[j] );
            log.info( "OpenGL Config.: " + String.valueOf( info ) );
        }
    }
    
    public static void imprimir( int[] configs, String[] nomesConfigs, int[] configNumComp ) {
        final int[] valConfig =  new int[Arrays.stream( configNumComp ).max().getAsInt()];
        
        for ( int i = 0; i < configs.length; i++ ) {
            gl4.glGetIntegerv( configs[i], valConfig, 0 );
            
            StringBuilder info = new StringBuilder( Integer.toString( valConfig[0] ) );
            for ( int j = 1; j < configNumComp[i]; j++ )
                info.append(", ").append( valConfig[j] );
            log.info( "OpenGL Config.: " + nomesConfigs[i] + ": " + String.valueOf( info ) );
        }
    }
    
    public static void imprimirConfigBuffer( int tipo, int buffer ) {
        final int[] bufferConfig =  new int[10];
        
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, bufferConfig, 0
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, bufferConfig, 1
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE, bufferConfig, 2
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE, bufferConfig, 3
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE, bufferConfig, 4
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE, bufferConfig, 5
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE, bufferConfig, 6
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE, bufferConfig, 7
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE, bufferConfig, 8
        );
        gl4.glGetFramebufferAttachmentParameteriv(
            tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, bufferConfig, 9
        );
        
        String tipoAlvo;
        switch ( bufferConfig[0] ) {
            case GL4.GL_NONE:
                tipoAlvo = "nenhum";
                break;
            case GL4.GL_FRAMEBUFFER_DEFAULT:
                tipoAlvo = "default framebuffer";
                break;
            case GL4.GL_TEXTURE:
                tipoAlvo = "textura";
                break;
            case GL4.GL_RENDERBUFFER:
                tipoAlvo = "renderbuffer";
                break;
            default:
                tipoAlvo = "indeterminado";
        }
        log.info( "Buffer Config - Tipo: " + tipoAlvo );
        
        log.info( "Buffer Config - Nome: " + Integer.toString( bufferConfig[1] ) );
        
        log.info( "Buffer Config - Red: " + bufferConfig[2] + " bits" );
        log.info( "Buffer Config - Green: " + bufferConfig[3] + " bits" );
        log.info( "Buffer Config - Blue: " + bufferConfig[4] + " bits" );
        log.info( "Buffer Config - Alpha: " + bufferConfig[5] + " bits" );
        log.info( "Buffer Config - Depth: " + bufferConfig[6] + " bits" );
        log.info( "Buffer Config - Stencil: " + bufferConfig[7] + " bits" );
        
        String formatoInterno;
        switch ( bufferConfig[8] ) {
            case GL4.GL_FLOAT:
                formatoInterno = "float";
                break;
            case GL4.GL_INT:
                formatoInterno = "int";
                break;
            case GL4.GL_UNSIGNED_INT:
                formatoInterno = "unsigned int";
                break;
            case GL4.GL_SIGNED_NORMALIZED:
                formatoInterno = "signed normalized";
                break;
            case GL4.GL_UNSIGNED_NORMALIZED:
                formatoInterno = "unsigned normalized";
                break;
            default:
                formatoInterno = "indeterminado";
        }
        log.info( "Buffer Config - Formato interno: " + formatoInterno );
        
        String espacoCor;
        switch ( bufferConfig[9] ) {
            case GL4.GL_LINEAR:
                espacoCor = "linear";
                break;
            case GL4.GL_SRGB:
                espacoCor = "sRGB";
                break;
            default:
                espacoCor = "indeterminado";
        }
        log.info( "Buffer Config - Espaço de cores: " + espacoCor );
    }
}