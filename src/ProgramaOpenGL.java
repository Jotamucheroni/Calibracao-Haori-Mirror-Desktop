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
        
        layout(binding = 0) uniform sampler2D imagem[2];
        """
        /* +
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
            .append( "out vec4 saida[" ).append( numSaidas ).append( "];\n" )
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
                
                if ( numCompCor > 0 ) {
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
        
        programas[cor][tex][pb] = gerarPrograma(
            vertexShaderCode.toString(), fragmentShaderCode.toString()
        );
        return programas[cor][tex][pb];
    }
    
    public void liberarRecursos() {
        for ( int[][] matProg : programas )
            for ( int[] vetProg : matProg )
                for ( int programa : vetProg )
                    gl4.glDeleteProgram( programa );
    }
}