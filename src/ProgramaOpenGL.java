import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import com.jogamp.opengl.GL4;

public class ProgramaOpenGL {
    private GL4 gl4;
    
    ProgramaOpenGL( GL4 gl4 ) {
        this.gl4  = gl4;
    }
    
    private static Logger log = Logger.getLogger( ProgramaOpenGL.class.getName() );
    
    public int loadShader( int type, String shaderCode ) {
        int shader = gl4.glCreateShader( type );
        
        gl4.glShaderSource(
            shader, 1, new String[]{ shaderCode }, new int[]{ shaderCode.length() }, 0
        );
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
            log.info(
                    "\nUm programa não pôde ser compilado:\n" 
                +   infoLog
                +   "\n"
                +   "Código fonte:"
            );
            int i = 1;
            for( String linha: shaderCode.split( "\n" ) ) {
                System.out.println( i + "\t" + linha );
                i++;
            }
        }
        
        return shader;
    }
    
    public int gerarPrograma( String vertexShaderCode, String fragmentShaderCode ) {
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
        
        layout(binding = 0) uniform sampler2D imagem;
        """
        ;
    
    private static final String fragmentCabMainRef =
        """
        void main() {
        """
        ;
    
    private static final int numSaidas = 3;
    
    private static final String corFragRef =
        """
        saida[2] = saida[1] = saida[0] = corFrag;
        """
        ;
    
    private final int[][][] programas = new int[][][] {
        { { 0, 0 }, { 0, 0 } }, { { 0, 0 }, { 0, 0 } }
    };
    
    public int gerarPrograma( int numCompCor, int numCompTex, boolean texPb ) {
        int cor = ( numCompCor > 0 ) ? 1 : 0, tex = ( numCompTex > 0 ) ? 1 : 0, pb = texPb ? 1 : 0;
        
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
        fragmentShaderCode
            .append( "out vec4 saida[" )
            .append( numSaidas )
            .append( "];\n" )
            .append( fragmentCabMainRef );
        
        if ( !( numCompCor > 0 || numCompTex > 0 ) ) {
            fragmentShaderCode.append( "vec4 corFrag = vec4( 1.0, 1.0, 1.0, 1.0 );\n" );
            fragmentShaderCode.append( corFragRef );
        }
        else {
            if ( numCompCor > 0 ) {
                vertexShaderCode.append( "corFrag = cor;\n" );
                
                if ( !( numCompTex > 0 ) )
                    fragmentShaderCode.append( corFragRef );
            }
            if ( numCompTex > 0 ) {
                vertexShaderCode.append( "texFrag = tex;\n" );
                fragmentShaderCode.append(
                    """
                    ivec2 tamanho = textureSize( imagem, 0 );
                    
                    vec2 dist = vec2( 1.0 / float( tamanho.x ), 1.0 / float( tamanho.y ) );
                    
                    float gx[3][3] = float[3][3]( float[3]( -1.0,  0.0,  1.0 ), float[3]( -2.0,  0.0,  2.0 ), float[3]( -1.0,  0.0,  1.0 ) );
                    float gy[3][3] = float[3][3]( float[3](  1.0,  2.0,  1.0 ), float[3](  0.0,  0.0,  0.0 ), float[3]( -1.0, -2.0, -1.0 ) );
                    
                    vec4 corTex;
                    vec4 janela[3][3];
                    
                    vec4 dx = vec4( 0.0 );
                    vec4 dy = vec4( 0.0 );
                    
                    for ( int y = -1; y <= 1; y++ )
                        for ( int x = -1; x <= 1; x++ ) {
                            int i = y + 1, j = x + 1;
                            corTex = texture( imagem, vec2( texFrag.x + float(x) * dist.x, texFrag.y + float(y) * dist.y ) );
                    """
                );
                
                if ( pb > 0 ) {
                    fragmentShaderCode.append(
                        """
                            corTex.b = corTex.g = corTex.r;
                        """
                    );
                }
                
                if ( numCompCor > 0 ) {
                    fragmentShaderCode.append(
                        """
                            janela[i][j] = corTex = 0.5 * corFrag + 0.5 * corTex;
                        """
                    );
                }
                else {
                    fragmentShaderCode.append(
                        """
                            janela[i][j] = corTex;
                        """
                    );
                }
                
                fragmentShaderCode.append(
                    """
                        dx += gx[i][j] * corTex;
                        dy += gy[i][j] * corTex;
                    }
                    
                    vec4 sobel = sqrt( dx * dx + dy * dy );
                    
                    saida[0] = janela[1][1];
                    saida[1] = sobel;
                    saida[2] = ( sobel.g > 1.0 ) ? vec4( 1.0 ) : vec4( 0.0 );
                    """
                );
            }
        }
        
        vertexShaderCode.append( "}" );
        fragmentShaderCode.append( "}" );
        
        programas[cor][tex][pb] = gerarPrograma(
            vertexShaderCode.toString(), fragmentShaderCode.toString()
        );
        return programas[cor][tex][pb];
    }
    
    public void close() {
        for ( int[][] matProg : programas )
            for ( int[] vetProg : matProg )
                for ( int programa : vetProg )
                    gl4.glDeleteProgram( programa );
    }
}