package aplicativo.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.jogamp.opengl.GL4;

public class Desenho extends OpenGL implements AutoCloseable {
    public static final int
        PONTOS = GL4.GL_POINTS,
        LINHAS = GL4.GL_LINES,
        TRIANGULOS = GL4.GL_TRIANGLES;
    
    private static final int[] vetorModoDesenho = new int[]{
        PONTOS, LINHAS, TRIANGULOS
    };
    
    public static final int
        PADRAO = TRIANGULOS;
    
    private static final float[] 
        refQuad = {
        //  Posição            Textura
           -1.0f,   1.0f,      0.0f,   0.0f,
           -1.0f,  -1.0f,      0.0f,   1.0f,
            1.0f,  -1.0f,      1.0f,   1.0f,
            1.0f,   1.0f,      1.0f,   0.0f
        };
    
    private static final int[] refElementos = { 0, 1, 2, 2, 3, 0 };
    
    public static float[] getRefQuad(){
        return refQuad.clone();
    }
    
    public static int[] getRefElementos(){
        return refElementos.clone();
    }
    
    private final int 
        vertexBufferObject, elementBufferObject,
        vertexArrayObject,
        numElementos;
        
    private final Programa programa;
    
    private final int
        ponteiroMatrizEscala,
        ponteiroMatrizRotX, ponteiroMatrizRotY, ponteiroMatrizRotZ,
        ponteiroMatrizTrans;
    
    private final Textura textura;
    private final int ponteiroParametroTextura;
    
    private final int modoDesenho;
    
    public Desenho(
        int numCompPos, int numCompCor, int numCompTex,
        float[] vertices, int[] elementos, Textura textura,
        int modoDesenho
    ) {
        {
            final int[] leitorId = new int[2];
            
            gl4.glGenBuffers( 2, leitorId, 0 );
            vertexBufferObject = leitorId[0];
            elementBufferObject = leitorId[1];
            
            gl4.glGenVertexArrays( 1, leitorId, 0 );
            vertexArrayObject = leitorId[0];
        }
        
        programa = new Programa(
            numCompCor > 0,
            numCompTex > 0,
            textura != null && textura.getNumeroComponentesCor() == 1
        );
        
        gl4.glBindVertexArray( vertexArrayObject );
            
            gl4.glBindBuffer( GL4.GL_ARRAY_BUFFER, vertexBufferObject );
            
            {
                final int tamanhoTotalVertices = vertices.length * Float.BYTES;
                
                gl4.glBufferData(
                    GL4.GL_ARRAY_BUFFER, tamanhoTotalVertices,
                    ByteBuffer
                        .allocateDirect( tamanhoTotalVertices )
                        .order( ByteOrder.nativeOrder() )
                        .asFloatBuffer()
                        .put( vertices )
                        .position( 0 ),
                    GL4.GL_STATIC_DRAW
                );
            }
            
            {
                final int[] ponteiroVertice = new int[]{
                    programa.getAttribLocation( "pos" ),
                    programa.getAttribLocation( "cor" ),
                    programa.getAttribLocation( "tex" )
                };
                final int[] numComp = new int[] { numCompPos, numCompCor, numCompTex };
                final int tamanhoVertice = ( numCompPos + numCompCor + numCompTex ) * Float.BYTES;
                int deslocamento = 0;
                
                for ( int i = 0; i < ponteiroVertice.length; i++ ) {
                    gl4.glEnableVertexAttribArray( ponteiroVertice[i] );
                    gl4.glVertexAttribPointer(
                        ponteiroVertice[i], numComp[i], GL4.GL_FLOAT, false,
                        tamanhoVertice, deslocamento
                    );
                    deslocamento += numComp[i] * Float.BYTES;
                }
            }
            
            numElementos = elementos.length;
            gl4.glBindBuffer( GL4.GL_ELEMENT_ARRAY_BUFFER, elementBufferObject );
            
            {
                final int tamanhoElementos = numElementos * Integer.BYTES;
                
                gl4.glBufferData(
                    GL4.GL_ELEMENT_ARRAY_BUFFER, tamanhoElementos,
                    ByteBuffer
                        .allocateDirect( tamanhoElementos )
                        .order( ByteOrder.nativeOrder() )
                        .asIntBuffer()
                        .put( elementos )
                        .position( 0 ),
                    GL4.GL_STATIC_DRAW
                );
            }
        
        gl4.glBindVertexArray( 0 );
        gl4.glBindBuffer( GL4.GL_ELEMENT_ARRAY_BUFFER, 0 );
        gl4.glBindBuffer( GL4.GL_ARRAY_BUFFER, 0 );
        
        ponteiroMatrizEscala = programa.getUniformLocation( "escala" );
        ponteiroMatrizRotX = programa.getUniformLocation( "rotX" );
        ponteiroMatrizRotY = programa.getUniformLocation( "rotY" );
        ponteiroMatrizRotZ = programa.getUniformLocation( "rotZ" );
        ponteiroMatrizTrans = programa.getUniformLocation( "trans" );
        
        this.textura = textura;
        ponteiroParametroTextura = programa.getUniformLocation( "parametroTextura" );
        
        boolean modoDesenhoValido = false;
        
        for( int modo : vetorModoDesenho )
            if( modo == modoDesenho ) {
                modoDesenhoValido = true;
                break;
            }
        
        if ( modoDesenhoValido )
            this.modoDesenho = modoDesenho;
        else
            this.modoDesenho = PADRAO;
    }
    
    public Desenho(
        int numCompPos, int numCompCor, int numCompTex,
        float[] vertices, int[] elementos, Textura textura
    ) {
        this(
            numCompPos, numCompCor, numCompTex,
            vertices, elementos, textura,
            PADRAO
        );
    }
    
    public Desenho(
        int numCompPos, int numCompCor,
        float[] vertices, int[] elementos,
        int modoDesenho
    ) {
        this(
            numCompPos, numCompCor, 0,
            vertices, elementos, null,
            modoDesenho
        );
    }
    
    public Desenho(
        int numCompPos, int numCompCor,
        float[] vertices, int[] elementos
    ) {
        this(
            numCompPos, numCompCor, 0,
            vertices, elementos, null,
            PADRAO
        );
    }
    
    public Desenho(
        int numCompPos, int numCompCor, int numCompTex,
        float[] vertices, Textura textura,
        int modoDesenho
    ) {
        this(
            numCompPos, numCompCor, numCompTex,
            vertices, getElementos( numCompPos, numCompCor, numCompTex, vertices ), textura,
            modoDesenho
        );
    }
    
    public Desenho(
        int numCompPos, int numCompCor, int numCompTex,
        float[] vertices, Textura textura
    ) {
        this(
            numCompPos, numCompCor, numCompTex,
            vertices, getElementos( numCompPos, numCompCor, numCompTex, vertices ), textura,
            PADRAO
        );
    }
    
    public Desenho(
        int numCompPos, int numCompCor,
        float[] vertices,
        int modoDesenho
    ) {
        this(
            numCompPos, numCompCor, 0,
            vertices, getElementos( numCompPos, numCompCor, 0, vertices ), null,
            modoDesenho
        );
    }
    
    public Desenho(
        int numCompPos, int numCompCor,
        float[] vertices
    ) {
        this(
            numCompPos, numCompCor, 0,
            vertices, getElementos( numCompPos, numCompCor, 0, vertices ), null,
            PADRAO
        );
    }
    
    public Desenho(
        int numCompPos, int numCompTex,
        float[] vertices, int[] elementos, Textura textura,
        int modoDesenho
    ) {
        this(
            numCompPos, 0, numCompTex,
            vertices, elementos, textura,
            modoDesenho
        );
    }
    
    public Desenho(
        int numCompPos, int numCompTex,
        float[] vertices, int[] elementos, Textura textura
    ) {
        this(
            numCompPos, 0, numCompTex,
            vertices, elementos, textura,
            PADRAO
        );
    }
    
    public Desenho(
        int numCompPos,
        float[] vertices, int[] elementos,
        int modoDesenho
    ) {
        this(
            numCompPos, 0, 0,
            vertices, elementos, null,
            modoDesenho
        );
    }
    
    public Desenho(
        int numCompPos,
        float[] vertices, int[] elementos
    ) {
        this(
            numCompPos, 0, 0,
            vertices, elementos, null,
            PADRAO
        );
    }
    
    public Desenho(
        int numCompPos, int numCompTex,
        float[] vertices, Textura textura,
        int modoDesenho
    ) {
        this(
            numCompPos, 0, numCompTex,
            vertices, getElementos( numCompPos, 0, numCompTex, vertices ), textura,
            modoDesenho
        );
    }
    
    public Desenho(
        int numCompPos, int numCompTex,
        float[] vertices, Textura textura
    ) {
        this(
            numCompPos, 0, numCompTex,
            vertices, getElementos( numCompPos, 0, numCompTex, vertices ), textura,
            PADRAO
        );
    }
    
    public Desenho(
        int numCompPos,
        float[] vertices,
        int modoDesenho
    ) {
        this(
            numCompPos, 0, 0,
            vertices, getElementos( numCompPos, 0, 0, vertices ), null,
            modoDesenho
        );
    }
    
    public Desenho(
        int numCompPos,
        float[] vertices
    ) {
        this(
            numCompPos, 0, 0,
            vertices, getElementos( numCompPos, 0, 0, vertices ), null,
            PADRAO
        );
    }
    
    private static int[] getElementos(
        int numCompPos, int numCompCor, int numCompTex, float[] vertices
    ) {
        int numVertices = vertices.length / ( numCompPos + numCompCor + numCompTex );
        int[] elementos = new int[numVertices];
        
        for ( int i = 0; i < numVertices; i++ )
            elementos[i] = i;
        
        return elementos;
    }
    
    private static final int NUMERO_COORDENDAS = 3;
    
    public static final int
        X = 0,
        Y = 1,
        Z = 2;
    
    private static float[] getXYZ( float[] xyz ) {
        final float[] xyzFinal = new float[]{ 0.0f, 0.0f, 0.0f };
        
        if ( xyz == null )
            return xyzFinal;
        
        for ( int i = 0; i < xyz.length && i < xyzFinal.length; i++ )
            xyzFinal[i] = xyz[i];
        
        return xyzFinal;
    }
    
    private int getCoordenadaValida( int coord ) {
        if ( coord < X )
            coord = X;
        else if ( coord > Z )
            coord = Z;
        
        return coord;
    }
    
    private final float[]
        vetorEscala = new float[NUMERO_COORDENDAS],
        vetorRotacao = new float[NUMERO_COORDENDAS],
        vetorTranslacao = new float[NUMERO_COORDENDAS];
    
    private static final float[] matrizId = {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    };
    
    private final float[] 
        matrizEscala = matrizId.clone(),
        matrizRotX = matrizId.clone(),
        matrizRotY = matrizId.clone(),
        matrizRotZ = matrizId.clone(),
        matrizTrans = matrizId.clone();
    
    public void setEscala( float x, float y, float z ) {
        vetorEscala[0] = x;
        vetorEscala[1] = y;
        vetorEscala[2] = z;
        
        matrizEscala[0] = x;    //                0                    0           0
        /*                0*/   matrizEscala[5] = y;    //             0           0
        /*                0                       0*/   matrizEscala[10] = z;   // 0
        //                0                       0                        0       1
    }
    
    public void setEscala( float[] xyz ) {
        if ( xyz == null )
            return;
        
        xyz = getXYZ( xyz );
        setEscala( xyz[0], xyz[1], xyz[2] );
    }
    
    public void setRotacao( float x, float y, float z ) {
        vetorRotacao[0] = x;
        vetorRotacao[1] = y;
        vetorRotacao[2] = z;
        
        double
            sinX = Math.sin( x ), cosX = Math.cos( x ),
            sinY = Math.sin( y ), cosY = Math.cos( y ),
            sinZ = Math.sin( z ), cosZ = Math.cos( z );
        
        // 1                                0                                  0        0
        /* 0*/  matrizRotX[5] = (float)  cosX;    matrizRotX[6] = (float)  -sinX;    // 0
        /* 0*/  matrizRotX[9] = (float)  sinX;    matrizRotX[10] = (float)  cosX;    // 0
        // 0                                0                                  0        1
        
        matrizRotY[0] = (float)  cosY;    /* 0*/  matrizRotY[2] = (float)   sinY;    // 0
        //                          0        1                                 0        0
        matrizRotY[8] = (float) -sinY;    /* 0*/  matrizRotY[10] = (float)  cosY;    // 0
        //                          0        0                                 0        1
        
        matrizRotZ[0] = (float)  cosZ;    matrizRotZ[1] = (float) -sinZ;    // 0    0
        matrizRotZ[4] = (float)  sinZ;    matrizRotZ[5] = (float)  cosZ;    // 0    0
        //                          0                                 0        1    0
        //                          0                                 0        0    1
    }
    
    public void setRotacao( float[] xyz ) {
        if ( xyz == null )
            return;
        
        xyz = getXYZ( xyz );
        setRotacao( xyz[0], xyz[1], xyz[2] );
    }
    
    public void setTranslacao( float x, float y, float z ) {
        vetorTranslacao[0] = x;
        vetorTranslacao[1] = y;
        vetorTranslacao[2] = z;
        
        /*                    1                    0                    0*/   matrizTrans[3] =  x;
        /*                    0                    1                    0*/   matrizTrans[7] =  y;
        /*                    0                    0                    1*/   matrizTrans[11] = z;
        //                    0                    0                    0                       1
    }
    
    public void setTranslacao( float[] xyz ) {
        if ( xyz == null )
            return;
        
        xyz = getXYZ( xyz );
        setTranslacao( xyz[0], xyz[1], xyz[2] );
    }
    
    public float getEscala( int coord ) {
        return vetorEscala[getCoordenadaValida( coord )];
    }
    
    public float getRotacao( int coord ) {
        return vetorRotacao[getCoordenadaValida( coord )];
    }
    
    public float getTranslacao( int coord ) {
        return vetorTranslacao[getCoordenadaValida( coord )];
    }
    
    public float[] getVetorEscala() {
        return vetorEscala.clone();
    }
    
    public float[] getVetorRotacao() {
        return vetorRotacao.clone();
    }
    
    public float[] getVetorTranslacao() {
        return vetorTranslacao.clone();
    }
    
    public float[] getMatrizEscala() {
        return matrizEscala.clone();
    }
    
    public float[] getMatrizRotacaoX() {
        return matrizRotX.clone();
    }
    
    public float[] getMatrizRotacaoY() {
        return matrizRotY.clone();
    }
    
    public float[] getMatrizRotacaoZ() {
        return matrizRotZ.clone();
    }
    
    public float[] getMatrizTranslacao() {
        return matrizTrans.clone();
    }
    
    private final float[] parametroTextura = new float[Programa.MAXIMO_PARAMETROS_TEXTURA]; 
    
    private int getIndiceParametroValido( int indiceOriginal ) {
        if ( indiceOriginal < 0 )
            return 0;
        
        if ( indiceOriginal >= parametroTextura.length )
            return parametroTextura.length - 1;
        
        return indiceOriginal;
    }
    
    public void setParametroTextura( int indiceParametro, float valor ) {
        if ( indiceParametro < 0 || indiceParametro >= parametroTextura.length )
            return;
        
        parametroTextura[indiceParametro] = valor;
    }
    
    public float getParametroTextura( int indiceParametro ) {
        return parametroTextura[getIndiceParametroValido( indiceParametro )];
    }
    
    public void draw() {
        if ( textura != null )
            textura.bind();
        
        programa.ativar();
        
        gl4.glUniformMatrix4fv( ponteiroMatrizEscala, 1, true, matrizEscala, 0 );
        gl4.glUniformMatrix4fv( ponteiroMatrizRotX, 1, true, matrizRotX, 0 );
        gl4.glUniformMatrix4fv( ponteiroMatrizRotY, 1, true, matrizRotY, 0 );
        gl4.glUniformMatrix4fv( ponteiroMatrizRotZ, 1, true, matrizRotZ, 0 );
        gl4.glUniformMatrix4fv( ponteiroMatrizTrans, 1, true, matrizTrans, 0 );
        
        gl4.glUniform1fv(
            ponteiroParametroTextura, parametroTextura.length, parametroTextura, 0
        );
        
        gl4.glBindVertexArray( vertexArrayObject );
        gl4.glDrawElements( modoDesenho, numElementos, GL4.GL_UNSIGNED_INT, 0 );
        gl4.glBindVertexArray( 0 );
        
        programa.desativar();
        
        if ( textura != null )
            textura.unbind();
    }
    
    @Override
    public void close() {
        programa.close();
        gl4.glDeleteVertexArrays( 1, new int[]{ vertexArrayObject }, 0 );
        gl4.glDeleteBuffers( 2, new int[]{ elementBufferObject, vertexBufferObject }, 0 );
    }
}