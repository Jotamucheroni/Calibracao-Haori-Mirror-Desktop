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
    
    public String gerarCodigoVertexShader( boolean cor, boolean textura ) {
        StringBuilder codigo = new StringBuilder(
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
        );
        
        if ( cor )
            codigo.append( "out vec4 corFrag;\n" );
        
        if ( textura )
            codigo.append( "out vec2 texFrag;\n" );
        
        codigo.append(
            """
            
            void main() {
                gl_Position = trans * rotZ * rotY * rotX * escala * pos;
                
            """
        );
        
        if ( cor )
            codigo.append( "    corFrag = cor;\n" );
        
        if ( textura )
            codigo.append( "    texFrag = tex;\n" );
        
        codigo.append( "}" );
        
        return codigo.toString();
    }
    
    private static final int numSaidas = 3;
    
    public String gerarCodigoFragmentShader(
        boolean cor, boolean textura, boolean texturaMonocromatica
    ) {
        StringBuilder codigo = new StringBuilder(
            """    
            #version 460
            
            """
        );
        
        if ( cor )
            codigo.append( "in vec4 corFrag;\n" );
        
        if ( textura )
            codigo.append(
                """
                in vec2 texFrag;
                
                layout(binding = 0) uniform sampler2D imagem;
                
                """
            );
        
        codigo
            .append( "out vec4 saida[" ).append( numSaidas ).append( "];\n" )
            .append( "\n" )
            .append( "void main() {\n" );
        
        if ( !textura ) {
            codigo.append( "\t" );
            
            for( int i = numSaidas; i > 0; i-- )
                codigo.append( "saida[" ).append( i - 1 ).append( "] = " );
            
            if ( cor )
                codigo.append( "corFrag" );
            else
                codigo.append( "vec4( 1.0, 1.0, 1.0, 1.0 )" );
            
            codigo.append( ";\n}" );
            
            return codigo.toString();
        }
        
        codigo.append(
            "    vec4 pixelCentral = texture( imagem, vec2( texFrag.x, texFrag.y ) );\n"
        );
        
        if ( texturaMonocromatica )
            codigo.append( "    pixelCentral.b = pixelCentral.g = pixelCentral.r;\n" );
        
        if ( cor )
            codigo.append( "    pixelCentral = 0.5 * corFrag + 0.5 * pixelCentral;\n" );
        
        codigo.append(
            """
                
                ivec2 tamanho = textureSize( imagem, 0 );
                vec2 dist = vec2( 1.0 / float( tamanho.x ), 1.0 / float( tamanho.y ) );
                
                float gx[3][3] = float[3][3](
                    float[3]( -1.0,  0.0,  1.0 ),
                    float[3]( -2.0,  0.0,  2.0 ),
                    float[3]( -1.0,  0.0,  1.0 )
                );
                float gy[3][3] = float[3][3](
                    float[3](  1.0,  2.0,  1.0 ),
                    float[3](  0.0,  0.0,  0.0 ),
                    float[3]( -1.0, -2.0, -1.0 )
                );
                
                vec4 corTex;
                
                vec4 dx = vec4( 0.0 );
                vec4 dy = vec4( 0.0 );
                
                int i, j;
                for ( int y = -1; y <= 1; y++ )
                    for ( int x = -1; x <= 1; x++ ) {
                        i = y + 1;
                        j = x + 1;
                        
                        corTex = texture(
                            imagem,
                            vec2( texFrag.x + float(x) * dist.x, texFrag.y + float(y) * dist.y )
                        );
            """
        );
        
        if ( texturaMonocromatica )
            codigo.append( "            corTex.b = corTex.g = corTex.r;\n" );
        
        if ( cor )
            codigo.append( "            corTex = 0.5 * corFrag + 0.5 * corTex;\n" );
        
        codigo.append(
            """
                        
                        dx += gx[i][j] * corTex;
                        dy += gy[i][j] * corTex;
                    }
                
                vec4 sobel = sqrt( dx * dx + dy * dy );
                
                saida[0] = pixelCentral;
                saida[1] = sobel;
                saida[2] = ( sobel.g > 1.0 ) ? vec4( 1.0 ) : vec4( 0.0 );
            }
            """
        );
            
        return codigo.toString();
    }
    
    public int gerarPrograma( boolean cor, boolean textura, boolean texturaMonocromatica ) {
        return gerarPrograma(
            gerarCodigoVertexShader( cor, textura ),
            gerarCodigoFragmentShader( cor, textura, texturaMonocromatica )
        );
    }
}