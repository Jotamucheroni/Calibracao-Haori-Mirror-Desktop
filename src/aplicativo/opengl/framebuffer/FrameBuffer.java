package aplicativo.opengl.framebuffer;

import com.jogamp.opengl.GL4;

import aplicativo.opengl.Desenho;
import aplicativo.opengl.OpenGL;

public abstract class FrameBuffer extends OpenGL {
    private int
        id,
        largura, altura;
    
    protected void setId( int id ) {
        if ( id < 0 )
            id = 0;
        
        this.id = id;
    }
    
    protected void setLargura( int largura ) {
        if ( largura < 1 )
            largura = 1;
        
        this.largura = largura;
    }
    
    protected void setAltura( int altura ) {
        if ( altura < 1 )
            altura = 1;
        
        this.altura = altura;
    }
    
    public int getId() {
        return id;
    }
    
    public int getLargura() {
        return largura;
    }
    
    public int getAltura() {
        return altura;
    }
    
    public int getNumeroPixeis() {
        return largura * altura;
    }
    
    public void bindDraw() {
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, id );
    }
    
    public void bindRead() {
        gl4.glBindFramebuffer( GL4.GL_READ_FRAMEBUFFER, id );
    }
    
    public void bind() {
        gl4.glBindFramebuffer( GL4.GL_FRAMEBUFFER, id );
    }
    
    public void unbindDraw() {
        gl4.glBindFramebuffer( GL4.GL_DRAW_FRAMEBUFFER, 0 );
    }
    
    public void unbindRead() {
        gl4.glBindFramebuffer( GL4.GL_READ_FRAMEBUFFER, 0 );
    }
    
    public void unbind() {
        gl4.glBindFramebuffer( GL4.GL_FRAMEBUFFER, 0 );
    }
    
    public void clear() {
        bindDraw();
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
        unbindDraw();
    }
    
    public void draw( int x, int y, int largura, int altura, Desenho desenho ) {
        if ( desenho == null )
            return;
        
        bindDraw();
        gl4.glViewport( x, y, largura, altura );
        desenho.draw();
        unbindDraw();
    }
    
    public void draw( int largura, int altura, Desenho desenho ) {
        draw( 0, 0, largura, altura, desenho );
    }
    
    public void draw( Desenho desenho ) {
        draw( 0, 0, this.largura, this.altura, desenho );
    }
    
    public void draw( int x, int y, int largura, int altura, Desenho[] desenho ) {
        if ( desenho == null )
            return;
        
        bindDraw();
        gl4.glViewport( x, y, largura, altura );
        
        for( Desenho des : desenho )
            des.draw();
        unbindDraw();
    }
    
    public void draw( int largura, int altura, Desenho[] desenho ) {
        draw( 0, 0, largura, altura, desenho );
    }
    
    public void draw( Desenho[] desenho ) {
        draw( 0, 0, this.largura, this.altura, desenho );
    }
}