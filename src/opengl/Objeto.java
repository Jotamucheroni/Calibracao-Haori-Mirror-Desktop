package opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL4;

public class Objeto extends OpenGL {
    private static final float[] 
        refQuad = {
            // Coordenadas  Textura
            -1.0f,  1.0f,   0.0f, 0.0f,
            -1.0f, -1.0f,   0.0f, 1.0f,
            1.0f, -1.0f,    1.0f, 1.0f,
            1.0f,  1.0f,    1.0f, 0.0f
        };
    
    private static final int[] refElementos = { 0, 1, 2, 2, 3, 0 };
    
    public static float[] getRefQuad(){
        return refQuad.clone();
    }
    
    public static int[] getRefElementos(){
        return refElementos.clone();
    }
    
    private final Textura textura;
    private final int numElementos;
    private final int[] vao = new int[1];
    private final int program;
    
    private int
        pontMatrizEscala,
        pontMatrizRotX, pontMatrizRotY, pontMatrizRotZ,
        pontMatrizTrans;
    
    public Objeto(
        int numCompPos, int numCompCor, int numCompTex,
        float[] vertices, int[] elementos, Textura textura
    ) {
        this.textura = textura;
        
        int numCompTotal = numCompPos + numCompCor + numCompTex;
        int tamVertice = numCompTotal * Float.BYTES;
        int tamVertices = vertices.length * Float.BYTES;
        numElementos = elementos.length;
        int tamElementos = numElementos * Integer.BYTES;
        
        final int[] vbo = new int[1];
        gl4.glGenBuffers( vbo.length, vbo, 0 );
        gl4.glBindBuffer( GL4.GL_ARRAY_BUFFER, vbo[0] );
        
        ByteBuffer bb = ByteBuffer.allocateDirect( tamVertices );
        bb.order( ByteOrder.nativeOrder() );
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put( vertices );
        fb.position( 0 );
        gl4.glBufferData( GL4.GL_ARRAY_BUFFER, tamVertices, fb, GL4.GL_STATIC_DRAW );
        
        program = Programa.gerarPrograma(
            numCompCor > 0, numCompTex > 0, textura != null && textura.getMonocromatica()
        );
        
        pontMatrizEscala = gl4.glGetUniformLocation( program, "escala" );
        pontMatrizRotX = gl4.glGetUniformLocation( program, "rotX" );
        pontMatrizRotY = gl4.glGetUniformLocation( program, "rotY" );
        pontMatrizRotZ = gl4.glGetUniformLocation( program, "rotZ" );
        pontMatrizTrans = gl4.glGetUniformLocation( program, "trans" );
        int pontPos = gl4.glGetAttribLocation( program, "pos" );
        int pontCor = gl4.glGetAttribLocation( program, "cor" );
        int pontTex = gl4.glGetAttribLocation( program, "tex" );
        
        gl4.glGenVertexArrays( vao.length, vao, 0 );
        gl4.glBindVertexArray( vao[0] );
        
        gl4.glBindBuffer( GL4.GL_ARRAY_BUFFER, vbo[0] );
        
        int desl = 0;
        
        gl4.glVertexAttribPointer( pontPos, numCompPos, GL4.GL_FLOAT, false, tamVertice, 0 );
        desl += numCompPos * Float.BYTES;
        
        gl4.glVertexAttribPointer( pontCor, numCompCor, GL4.GL_FLOAT, false, tamVertice, desl );
        desl += numCompCor * Float.BYTES;
        
        gl4.glVertexAttribPointer( pontTex, numCompTex, GL4.GL_FLOAT, false, tamVertice, desl );
        
        gl4.glEnableVertexAttribArray( pontPos );
        gl4.glEnableVertexAttribArray( pontCor );
        gl4.glEnableVertexAttribArray( pontTex );
        
        final int[] ebo = new int[1];
        gl4.glGenBuffers( ebo.length, ebo, 0 );
        gl4.glBindBuffer( GL4.GL_ELEMENT_ARRAY_BUFFER, ebo[0] );
        bb = ByteBuffer.allocateDirect( tamElementos );
        bb.order( ByteOrder.nativeOrder() );
        IntBuffer ib = bb.asIntBuffer();
        ib.put( elementos );
        ib.position( 0 );
        gl4.glBufferData( GL4.GL_ELEMENT_ARRAY_BUFFER, tamElementos, ib, GL4.GL_STATIC_DRAW );
        
        gl4.glBindVertexArray( 0 );
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
    
    public Objeto(
        int numCompPos, int numCompCor, int numCompTex,
        float[] vertices, Textura textura
    ) {
        this(
            numCompPos, numCompCor, numCompTex,
            vertices, getElementos( numCompPos, numCompCor, numCompTex, vertices ), textura
        );
    }
    
    public Objeto(
        int numCompPos, int numCompCor,
        float[] vertices, int[] elementos
    ) {
        this(
            numCompPos, numCompCor, 0,
            vertices, elementos, null
        );
    }
    
    public Objeto(
        int numCompPos, int numCompCor,
        float[] vertices
    ) {
        this(
            numCompPos, numCompCor, 0,
            vertices, getElementos( numCompPos, numCompCor, 0, vertices ), null
        );
    }
    
    public Objeto(
        int numCompPos, int numCompTex,
        float[] vertices, int[] elementos, Textura textura
    ) {
        this(
            numCompPos, 0, numCompTex,
            vertices, elementos, textura
        );
    }
    
    public Objeto(
        int numCompPos, int numCompTex,
        float[] vertices, Textura textura
    ) {
        this(
            numCompPos, 0, numCompTex,
            vertices, getElementos( numCompPos, 0, numCompTex, vertices ), textura
        );
    }
    
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
        matrizEscala[0] = x;    //                0                    0           0
        /*                0*/   matrizEscala[5] = y;    //             0           0
        /*                0                       0*/   matrizEscala[10] = z;   // 0
        //                0                       0                        0       1
    }
    
    public void setRot( float x, float y, float z ) {
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
    
    public void setTrans( float x, float y, float z ) {
        /*                    1                    0                    0*/   matrizTrans[3] =  x;
        /*                    0                    1                    0*/   matrizTrans[7] =  y;
        /*                    0                    0                    1*/   matrizTrans[11] = z;
        //                    0                    0                    0                       1
    }
    
    public void draw() {
        if ( textura != null )
            textura.bind();
        
        gl4.glUseProgram( program );
        
        gl4.glUniformMatrix4fv( pontMatrizEscala, 1, true, matrizEscala, 0 );
        gl4.glUniformMatrix4fv( pontMatrizRotX, 1, true, matrizRotX, 0 );
        gl4.glUniformMatrix4fv( pontMatrizRotY, 1, true, matrizRotY, 0 );
        gl4.glUniformMatrix4fv( pontMatrizRotZ, 1, true, matrizRotZ, 0 );
        gl4.glUniformMatrix4fv( pontMatrizTrans, 1, true, matrizTrans, 0 );
        
        gl4.glBindVertexArray( vao[0] );
        gl4.glDrawElements( GL4.GL_TRIANGLES, numElementos, GL4.GL_UNSIGNED_INT, 0 );
        gl4.glBindVertexArray( 0 );
        
        gl4.glUseProgram( 0 );
        
        gl4.glBindTexture( GL4.GL_TEXTURE_2D, 0 );
    }
}