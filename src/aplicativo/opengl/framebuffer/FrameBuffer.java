package aplicativo.opengl.framebuffer;

import com.jogamp.opengl.GL4;

import aplicativo.opengl.Objeto;
import aplicativo.opengl.OpenGL;

public abstract class FrameBuffer extends OpenGL {
    private int largura, altura;
    
    private int id;
    
    private boolean alocado = false;
    
    public void setLargura( int largura ) {
        if ( largura < 1 )
            largura = 1;
        
        this.largura = largura;
    }
    
    public void setAltura( int altura ) {
        if ( altura < 1 )
            altura = 1;
        
        this.altura = altura;
    }
    
    protected void setId( int id ) {
        if ( id < 0 )
            id = 0;
        
        this.id = id;
    }
    
    protected void setAlocado( boolean alocado ) {
        this.alocado = alocado;
    }
    
    public int getLargura() {
        return largura;
    }
    
    public int getAltura() {
        return altura;
    }
    
    public int getId() {
        return id;
    }
    
    public boolean getAlocado() {
        return alocado;
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
    
    public void clear() {
        if ( !alocado )
            return;
        
        bindDraw();
        gl4.glClear( GL4.GL_COLOR_BUFFER_BIT );
    }
    
    public void draw( int x, int y, int largura, int altura, Objeto objeto ) {
        if ( !alocado || objeto == null )
            return;
        
        bindDraw();
        gl4.glViewport( x, y, largura, altura );
        objeto.draw();
    }
    
    public void draw( int largura, int altura, Objeto objeto ) {
        draw( 0, 0, largura, altura, objeto );
    }
    
    public void draw( Objeto objeto ) {
        draw( 0, 0, this.largura, this.altura, objeto );
    }
    
    public void draw( int x, int y, int largura, int altura, Objeto[] objeto ) {
        if ( !alocado || objeto == null )
            return;
        
        bindDraw();
        gl4.glViewport( x, y, largura, altura );
        
        for( Objeto obj : objeto )
            obj.draw();
    }
    
    public void draw( int largura, int altura, Objeto[] objeto ) {
        draw( 0, 0, largura, altura, objeto );
    }
    
    public void draw( Objeto[] objeto ) {
        draw( 0, 0, this.largura, this.altura, objeto );
    }
}