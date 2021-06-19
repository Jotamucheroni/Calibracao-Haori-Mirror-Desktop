import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import es.Bluetooth;
import es.camera.Camera;
import es.camera.CameraLocal;

public class MyGLRenderer implements GLEventListener {
    private static Logger log = Logger.getLogger( MyGLRenderer.class.getName() );
    private static GL4 gl4;

    private final float[] /* refTriangulo = {
            // Coordenadas          // Cor
            0.0f,    0.622008459f,   1.0f, 0.0f, 0.0f,
           -0.5f,   -0.311004243f,   0.0f, 1.0f, 0.0f,
            0.5f,   -0.311004243f,   0.0f, 0.0f, 1.0f
    }, */
                        refQuad = {
           -1.0f,  1.0f,      0.0f, 0.0f,
           -1.0f, -1.0f,      0.0f, 1.0f,
            1.0f, -1.0f,      1.0f, 1.0f,
            1.0f,  1.0f,      1.0f, 0.0f
    };


    private final int[] refElementos = {
            0, 1, 2,
            2, 3, 0
    };

    // private CameraLocal olhoVirtual;
    private final Camera olhoVirtual = new CameraLocal( 0, 640, 480, 1 );
    private final int largImgSmartphone = 320,
                      altImgSmartphone = 240,
                      tamImgSmartphone = largImgSmartphone * altImgSmartphone;

    private Bluetooth bt;

    private final int numLinhas = 2, numColunas = 3,
                      numLinhasM1 = numLinhas - 1;

    private final int[] fbo = new int[1];
    private final int[] rbo = new int[numLinhas * numColunas];

    private final int[] texturas = new int[4];

    private void setTexParams() {
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_S, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_WRAP_T, GL4.GL_CLAMP_TO_EDGE );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MIN_FILTER, GL4.GL_LINEAR );
        gl4.glTexParameteri( GL4.GL_TEXTURE_2D, GL4.GL_TEXTURE_MAG_FILTER, GL4.GL_LINEAR );
    }

    private byte[] toBGR( byte[] imagemABGR ) {
        byte[] imagemBGR = new byte[ imagemABGR.length - imagemABGR.length / 4 ];

        for ( int i = 1, j = 0; i < imagemABGR.length; i += 4, j += 3 ) {
            imagemBGR[j] = imagemABGR[i];
            imagemBGR[j + 1] = imagemABGR[i + 1];
            imagemBGR[j + 2] = imagemABGR[i + 2]; 
        }

        return imagemBGR;
    }

    // Objetos
    private final Objeto[] objetos = new Objeto[1];

    DetectorBorda detectorOlhoVirtual, detectorSmartphone;

    @Override
    public void init( GLAutoDrawable drawable ) {
        // Executar sempre primeiro-----
        gl4 = drawable.getGL().getGL4();
        Objeto.gl4 = gl4;
        // -----------------------------

        // Abre a câmera para capturar as imagens do olho virtual
        olhoVirtual.ligar();
        
        // Inicia a comunicação por Bluetooth para receber as imagens da câmera do smartphone
        bt = new Bluetooth( tamImgSmartphone );
        // bt.conectarSmartphone();

        // Cria um Framebuffer e seus respectivos Renderbuffers
        gl4.glGenFramebuffers( fbo.length, fbo, 0 );
        gl4.glGenRenderbuffers( rbo.length, rbo, 0 );
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, fbo[0] );

        // Aloca espaço para os Renderbuffers
        for ( int i = 0; i < rbo.length; i++ ) {
            gl4.glBindRenderbuffer( GL4.GL_RENDERBUFFER, rbo[i] );
            gl4.glRenderbufferStorage( GL4.GL_RENDERBUFFER, GL4.GL_RGB8, olhoVirtual.getLargImg(), olhoVirtual.getAltImg() );
            gl4.glFramebufferRenderbuffer( GL4.GL_DRAW_FRAMEBUFFER, GL4.GL_COLOR_ATTACHMENT0 + i, GL4.GL_RENDERBUFFER, rbo[i] );
        }
        gl4.glDrawBuffers( 
            6,
            new int[]{ GL4.GL_COLOR_ATTACHMENT0, GL4.GL_COLOR_ATTACHMENT1, GL4.GL_COLOR_ATTACHMENT2,
                       GL4.GL_COLOR_ATTACHMENT3, GL4.GL_COLOR_ATTACHMENT4, GL4.GL_COLOR_ATTACHMENT5 },
            0
        );

        // Cria as texturas
        gl4.glGenTextures( texturas.length, texturas, 0 );

        // Aloca espaço para a textura 0 para receber imagens do olho virtual
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[0] );
        setTexParams();
        gl4.glTexImage2D( 
                    GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
                    olhoVirtual.getLargImg(), olhoVirtual.getAltImg(), 0,
                    GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, null
        );

        // Aloca espaço para a textura 1 para receber imagens da câmera do smartphone
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[1] );
        setTexParams();
        gl4.glTexImage2D( 
                    GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
                    largImgSmartphone, altImgSmartphone, 0,
                    GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, null
        );

        // Carrega imagens nas demais texturas
        BufferedImage[] imageTex;
        try {
            imageTex = new BufferedImage[]{ ImageIO.read( new File( "imagens/cachorrinho.png" ) ), 
                                            ImageIO.read( new File( "imagens/gatinho.png" ) ) };

            for (int i = 2; i < texturas.length; i++) {
                int indice = i - 2;

                gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[i] );
                setTexParams();
    
                byte[] imagem = toBGR( ( (DataBufferByte) imageTex[indice].
                                                          getData().
                                                          getDataBuffer() ).
                                                          getData() );
                ByteBuffer bb = ByteBuffer.allocateDirect( imagem.length );
                bb.order( ByteOrder.nativeOrder() );
                bb.put( imagem );
                bb.position( 0 );

                gl4.glTexImage2D( 
                    GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
                    imageTex[indice].getWidth(), imageTex[indice].getHeight(), 0,
                    GL4.GL_BGR, GL4.GL_UNSIGNED_BYTE, bb
                    );
            }
        } catch ( IOException e ) { e.printStackTrace(); }

        // Determina a cor de fundo
        gl4.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f );

/*         float[] copiaTri = refTriangulo.clone();

        objetos[0] = new Objeto( GL4.GL_TRIANGLES, 2, 3, copiaTri );

        copiaTri[1] = 0.3f;   copiaTri[2] = 0.0f;
        copiaTri[8] = 0.0f;
        copiaTri[14] = 0.0f;
        objetos[1] = new Objeto( GL4.GL_TRIANGLES, 2, 3, copiaTri );


        copiaTri[1] = -0.1f;    copiaTri[2] = 1.0f;
        copiaTri[8] = 1.0f;
        copiaTri[14] = 1.0f;
        objetos[2] = new Objeto( GL4.GL_TRIANGLES, 2, 3, copiaTri );

        for ( int i = 0; i < 3; i ++ ) {
            objetos[i].setEscala(0.5f, 0.5f, 0.0f );
            objetos[i].setTrans(0.5f, 0.5f, 0.0f );
        }

        objetos[3] = new Objeto( GL4.GL_TRIANGLES, 2, 2,
                                 refQuad, refElementos, texturas[0], false );
        objetos[3].setTrans(-0.5f, -0.5f, 0.0f );

        objetos[4] = new Objeto( GL4.GL_TRIANGLES, 2, 2,
                                 refQuad, refElementos, texturas[1], false );
        objetos[4].setTrans(-0.5f, 0.5f, 0.0f );

        for ( int i = 3; i < 5; i ++ )
            objetos[i].setEscala( 0.25f, 0.25f, 0.0f ); */

        // Cria objetos para exibir as imagens das câmeras
        /* refQuad[2] = refQuad[6] = 1.0f;
        refQuad[10] = refQuad[14] = 0.0f; */
        objetos[0] = new Objeto( GL4.GL_TRIANGLES, 2, 2,
                                 refQuad, refElementos, new int[]{ texturas[0], texturas[1] }, true );
    
        detectorOlhoVirtual = new DetectorBorda( olhoVirtual.getLargImg() * olhoVirtual.getAltImg() * 3, 3 );
        detectorSmartphone = new DetectorBorda( olhoVirtual.getLargImg() * olhoVirtual.getAltImg() * 3, 3 );
    }

    private int viewWidth;
    private int viewHeight;

    @Override
    public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height )
	{
        viewWidth = width / numColunas;
        viewHeight = height / numLinhas;
        
        gl4.glViewport( 0, 0, olhoVirtual.getLargImg(), olhoVirtual.getAltImg() );
	}

    private ByteBuffer bufferBordaOlhoVirtual = ByteBuffer.allocateDirect( olhoVirtual.getLargImg() * olhoVirtual.getAltImg() * 3 );
    private ByteBuffer bufferBordaSmartphone = ByteBuffer.allocateDirect( olhoVirtual.getLargImg() * olhoVirtual.getAltImg() * 3 );

    @Override
    public void display( GLAutoDrawable drawable ) {
        /* long[] t = new long[3];

        t[0] = System.currentTimeMillis(); */
        // Imagens das câmeras------------------------------------------------------------------------
        // Copia a imagem atual do olho virtual para a textura 0
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[0] );
        gl4.glTexSubImage2D( GL4.GL_TEXTURE_2D, 0, 
                             0, 0, olhoVirtual.getLargImg(), olhoVirtual.getAltImg(),
                             GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, olhoVirtual.getImagem()
        );

        // Copia a imagem atual do smartphone para a textura 1
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, texturas[1] );
        bt.visBufferEntrada.rewind();
        gl4.glTexSubImage2D( GL4.GL_TEXTURE_2D, 0, 
                             0, 0, largImgSmartphone, altImgSmartphone,
                             GL4.GL_RED, GL4.GL_UNSIGNED_BYTE, bt.visBufferEntrada
        );
        //--------------------------------------------------------------------------------------------

        // Conecta o framebuffer auxiliar
        gl4.glBindFramebuffer( GL4.GL_FRAMEBUFFER, fbo[0] );
        
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
        for ( Objeto obj: objetos )
            obj.draw();

        // Desenha os renderbuffers na tela
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, 0 );
        for ( int i = 0; i < rbo.length; i++ ) {
            int coluna = i % numColunas;
            int linha = numLinhasM1 - ( i / numColunas );

            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT0 + i );
            gl4.glBlitFramebuffer( 0, 0, olhoVirtual.getLargImg(), olhoVirtual.getAltImg(),
                    coluna * viewWidth, linha * viewHeight, 
                    ( coluna + 1 ) * viewWidth, ( linha  + 1 ) * viewHeight,
                    GL4.GL_COLOR_BUFFER_BIT, GL4.GL_LINEAR );
        }

        // t[1] = System.currentTimeMillis();

        if ( detectorOlhoVirtual.pronto() ) {
            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT2 );
            bufferBordaOlhoVirtual.rewind();
            gl4.glReadPixels( 0, 0, olhoVirtual.getLargImg(), olhoVirtual.getAltImg(), 
                              GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE, 
                              bufferBordaOlhoVirtual );

            detectorOlhoVirtual.setImagem( bufferBordaOlhoVirtual );
            detectorOlhoVirtual.executar();
        }
        if ( detectorSmartphone.pronto() ) {
            gl4.glReadBuffer( GL4.GL_COLOR_ATTACHMENT5 );
            bufferBordaSmartphone.rewind();
            gl4.glReadPixels( 0, 0, olhoVirtual.getLargImg(), olhoVirtual.getAltImg(),
                              GL4.GL_RGB, GL4.GL_UNSIGNED_BYTE,
                              bufferBordaSmartphone );

            detectorSmartphone.setImagem( bufferBordaSmartphone );
            detectorSmartphone.executar();
        }
        /* t[2] = System.currentTimeMillis();
        long tempoTotal = t[2] - t[0];
        System.out.println(
            "Desenho - Tempos: " + ( t[1] - t[0] ) + " ms, " + ( t[2] - t[1] ) + " ms\t|\tTempo total: " 
            + tempoTotal + " ms\t|\tQuadros/s: " +  ( tempoTotal > 0 ? ( 1000 / tempoTotal ) : "+inf" )
        ); */
    }

    public static void printGLConfig( int[] configs ) {
        final int[] valConfig =  new int[1];

        for ( int config : configs ) {
            gl4.glGetIntegerv( config, valConfig, 0 );
            log.info( "OpenGL Config.: " + valConfig[0] );
        }
    }

    public static void printGLConfig( int[] configs, int[] configNumComp ) {
        final int[] valConfig =  new int[ Arrays.stream( configNumComp ).max().getAsInt() ];

        for ( int i = 0; i < configs.length; i++ ) {
            gl4.glGetIntegerv( configs[i], valConfig, 0 );

            StringBuilder info = new StringBuilder( Integer.toString(valConfig[0]) );
            for ( int j = 1; j < configNumComp[i]; j++ )
                info.append(", ").append( valConfig[j] );
            log.info( "OpenGL Config.: " + String.valueOf( info ) );
        }
    }

    public static void printGLConfig( int[] configs, String[] configNames) {
        final int[] valConfig =  new int[1];

        for ( int i = 0; i < configs.length; i++ ) {
            gl4.glGetIntegerv( configs[i], valConfig, 0 );
            log.info( "OpenGL Config.: " + configNames[i] + ": " + valConfig[0] );
        }
    }

    public static void printBufferConfig( int tipo, int buffer ) {
        final int[] bufferConfig =  new int[10];

        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE, bufferConfig, 0 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME, bufferConfig, 1 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE, bufferConfig, 2 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE, bufferConfig, 3 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE, bufferConfig, 4 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE, bufferConfig, 5 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE, bufferConfig, 6 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE, bufferConfig, 7 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE, bufferConfig, 8 );
        gl4.glGetFramebufferAttachmentParameteriv( tipo, buffer, GL4.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, bufferConfig, 9 );

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

    public static int loadShader( int type, String shaderCode ) {
        int shader = gl4.glCreateShader( type );

        gl4.glShaderSource( shader, 1, new String[]{ shaderCode }, new int[]{ shaderCode.length() }, 0 );
        gl4.glCompileShader( shader );
        int[] compilado = new int[1];
        gl4.glGetShaderiv( shader, GL4.GL_COMPILE_STATUS, compilado, 0 );

        // Verifica se houve erro de compilação
        if ( compilado[0] == 0 ) {
            // Imprime a mensagem de erro
            int[] tamLog = new int[1];
            gl4.glGetShaderiv( shader, GL4.GL_INFO_LOG_LENGTH, tamLog, 0 );

            byte[] b =  new byte[tamLog[0]];
            gl4.glGetShaderInfoLog( shader, tamLog[0], null, 0, b, 0 );
            String infoLog = new String( b, StandardCharsets.UTF_8 );
            log.info( "\nUm programa não pôde ser compilado:\n" + infoLog + "\n" + "Código fonte:" );
            int i = 1;
            for( String linha: shaderCode.split( "\n" ) ) {
                System.out.println( i + "\t" + linha );
                i++;
            }
        }

        return shader;
    }

    public static int gerarPrograma( String vertexShaderCode, String fragmentShaderCode ) {
        int program = gl4.glCreateProgram();

        int vertexShader = loadShader( GL4.GL_VERTEX_SHADER, vertexShaderCode );
        int fragmentShader = loadShader( GL4.GL_FRAGMENT_SHADER, fragmentShaderCode );

        gl4.glAttachShader( program, vertexShader );
        gl4.glAttachShader( program, fragmentShader );
        gl4.glLinkProgram( program );

        return program;
    }

    private static final String vertexShaderCabRef = 
    """
    #version 460

    uniform mat4 escala;
    uniform mat4 rotX;
    uniform mat4 rotY;
    uniform mat4 rotZ;
    uniform mat4 trans;

    in vec4 pos;
    in vec4 cor;
    in vec2 tex;
    """
    ;

    private static final String vertexShaderMainRef =
    """
    void main() {
        gl_Position = trans * rotZ * rotY * rotX * escala * pos;
    """
    ;

    private static final String fragmentCabRef =
    """
    #version 460

    layout(binding = 0) uniform sampler2D imagem[2];
    """
/*     +
    """
    vec4 getInv( in vec4 corOrig ) {

        return vec4( 1.0 ) - corOrig;
    }

    vec4[3][3] getInv( in vec4 janela[3][3] ) {

        for ( int i = 0; i < 9; i++ )
            janela[ i / 3 ][ i % 3 ] = getInv( janela[ i / 3 ][ i % 3 ] );

        return janela;
    }

    vec4 getCinzaMed( in vec4 corOrig ) {
        float cinza = ( corOrig.r + corOrig.g + corOrig.b ) / 3.0;

        return vec4( cinza, cinza, cinza, corOrig.a );
    }

    vec4[3][3] getCinzaMed( in vec4 janela[3][3] ) {

        for ( int i = 0; i < 9; i++ )
            janela[ i / 3 ][ i % 3 ] = getCinzaMed( janela[ i / 3 ][ i % 3 ] );

        return janela;
    }

    vec4 getCinzaPond( in vec4 corOrig ) {
        float cinza = 0.2126 * corOrig.r + 0.7152 * corOrig.g + 0.0722 * corOrig.b;

        return vec4( cinza, cinza, cinza, corOrig.a );
    }

    vec4[3][3] getCinzaPond( in vec4 janela[3][3] ) {

        for ( int i = 0; i < 9; i++ )
            janela[ i / 3 ][ i % 3 ] = getCinzaPond( janela[ i / 3 ][ i % 3 ] );

        return janela;
    }

    vec4 getBor( in vec4 janela[3][3] ) {
        vec4 soma = vec4( 0.0 );

        for ( int i = 0; i < 9; i++ )
            soma += janela[ i / 3 ][ i % 3 ];

        return soma / 9.0;
    }

    vec4 getSobel( in vec4 janela[3][3] ) {
        vec4 dx = - ( janela[0][0] + 2.0 * janela[1][0] + janela[2][0] ) + ( janela[0][2] + 2.0 * janela[1][2] + janela[2][2] );
        vec4 dy =   ( janela[0][0] + 2.0 * janela[0][1] + janela[0][2] ) - ( janela[2][0] + 2.0 * janela[2][1] + janela[2][2] );

        return sqrt( dx * dx + dy * dy );
    }
    """ */
    ;

    private static final String fragmentCabMainRef =
    """
    void main() {
    """
    ;

    private static final int numSaidas = 6;

    private static final String corFragRef =
    """
    saida[5] = saida[4] = saida[3] = saida[2] = saida[1] = saida[0] = corFrag; 
    /* saida[0] = saida[4] = corFrag;
    saida[1] = saida[5] = corFrag;
    saida[2] = saida[6] = corFrag;
    saida[3] = saida[7] = corFrag; */
    """
    ;

    private static final int[][][] programas = new int[][][] { { { 0, 0 }, { 0, 0 } }, { { 0, 0 }, { 0, 0 } } };

    public static int gerarPrograma( int numCompCor, int numCompTex, boolean texPb ) {
        int cor = (numCompCor > 0) ? 1 : 0, tex = (numCompTex > 0) ? 1 : 0, pb = texPb ? 1 : 0;

        if ( programas[cor][tex][pb] != 0 )
            return programas[cor][tex][pb];

        StringBuilder vertexShaderCode = new StringBuilder( vertexShaderCabRef );
        StringBuilder fragmentShaderCode = new StringBuilder( fragmentCabRef );

        if ( numCompCor > 0 ) {
            vertexShaderCode.append( "out vec4 corFrag;\n" );
            fragmentShaderCode.append( "in vec4 corFrag;\n" );
        }
        if ( numCompTex > 0 ) {
            vertexShaderCode.append( "out vec2 texFrag;\n" );
            fragmentShaderCode.append( "in vec2 texFrag;\n" );
        }

        vertexShaderCode.append( vertexShaderMainRef );
        fragmentShaderCode.append( "out vec4 saida[" ).append( numSaidas ).append( "];\n" ).append( fragmentCabMainRef );

        if ( ! ( numCompCor > 0 || numCompTex > 0 ) ) {
            fragmentShaderCode.append( "vec4 corFrag = vec4( 1.0, 1.0, 1.0, 1.0 );\n" );
            fragmentShaderCode.append( corFragRef );
        }
        else {
            if (numCompCor > 0) {
                vertexShaderCode.append( "corFrag = cor;\n" );

                if ( ! ( numCompTex > 0 ) )
                    fragmentShaderCode.append( corFragRef );
            }
            if (numCompTex > 0) {
                vertexShaderCode.append( "texFrag = tex;\n" );
                fragmentShaderCode.append(
                """
                ivec2 tamanho[2] = ivec2[2]( textureSize( imagem[0], 0 ), textureSize( imagem[1], 0 ) );

                vec2 dist[2] = vec2[2]( vec2( 1.0 / float( tamanho[0].x ), 1.0 / float( tamanho[0].y ) ),
                                        vec2( 1.0 / float( tamanho[1].x ), 1.0 / float( tamanho[1].y ) ) );

                float gx[3][3] = float[3][3]( float[3]( -1.0,  0.0,  1.0 ), float[3]( -2.0,  0.0,  2.0 ), float[3]( -1.0,  0.0,  1.0 ) );
                float gy[3][3] = float[3][3]( float[3](  1.0,  2.0,  1.0 ), float[3](  0.0,  0.0,  0.0 ), float[3]( -1.0, -2.0, -1.0 ) );

                vec4 corTex[2];
                vec4 janela[2][3][3];

                vec4 dx[2] = vec4[2]( vec4( 0.0 ), vec4( 0.0 ) );
                vec4 dy[2] = vec4[2]( vec4( 0.0 ), vec4( 0.0 ) );

                for ( int y = -1; y <= 1; y++ )
                    for ( int x = -1; x <= 1; x++ ) {
                        int i = y + 1, j = x + 1;
                        corTex[0] = texture( imagem[0], vec2( texFrag.x + float(x) * dist[0].x, texFrag.y + float(y) * dist[0].y ) );
                        corTex[1] = texture( imagem[1], vec2( texFrag.x + float(x) * dist[1].x, texFrag.y + float(y) * dist[1].y ) );
                """
                );

                if ( pb > 0 ) {
                    fragmentShaderCode.append(
                    """
                        corTex[0].b = corTex[0].g = corTex[0].r;
                        corTex[1].b = corTex[1].g = corTex[1].r;
                    """
                    );
                }

                if (numCompCor > 0) {
                        fragmentShaderCode.append(
                        """
                        janela[0][i][j] = corTex[0] = 0.5 * corFrag + 0.5 * corTex[0];
                        janela[1][i][j] = corTex[1] = 0.5 * corFrag + 0.5 * corTex[1];
                        """
                        );
                }
                else {
                    fragmentShaderCode.append(
                    """
                        janela[0][i][j] = corTex[0];
                        janela[1][i][j] = corTex[1];
                    """
                    );
                }
                
                fragmentShaderCode.append(
                """
                    dx[0] += gx[i][j] * corTex[0];
                    dy[0] += gy[i][j] * corTex[0];

                    dx[1] += gx[i][j] * corTex[1];
                    dy[1] += gy[i][j] * corTex[1];
                }

                vec4 sobel[2] = vec4[2]( sqrt( dx[0] * dx[0] + dy[0] * dy[0] ), sqrt( dx[1] * dx[1] + dy[1] * dy[1] ) );

                saida[0] = janela[0][1][1];
                saida[1] = sobel[0];
                saida[2] = ( sobel[0].g > 1.0 ) ? vec4( 100.0 ) : vec4( 0.0 );
                saida[3] = janela[1][1][1];
                saida[4] = sobel[1];
                saida[5] = ( sobel[1].g > 1.0 ) ? vec4( 100.0 ) : vec4( 0.0 );
                """
                );
            }
        }

        vertexShaderCode.append( "}" );
        fragmentShaderCode.append( "}" );

        programas[cor][tex][pb] = gerarPrograma( vertexShaderCode.toString(), fragmentShaderCode.toString() );
        return programas[cor][tex][pb];
    }

    @Override
    public void dispose( GLAutoDrawable drawable ) {
        detectorOlhoVirtual.close();
        detectorSmartphone.close();

        for ( int[][] matProg : programas )
            for ( int[] vetProg : matProg )
                for ( int programa: vetProg )
                    gl4.glDeleteProgram( programa );

        gl4.glDeleteTextures( texturas.length, texturas, 0 );
        gl4.glDeleteRenderbuffers( rbo.length, rbo, 0 );
        gl4.glDeleteFramebuffers( fbo.length, fbo, 0 );

        olhoVirtual.desligar();

        App.close();
    }
}
