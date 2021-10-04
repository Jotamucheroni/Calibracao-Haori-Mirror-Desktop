package aplicativo.pontos;

public class Ponto2D {
    protected float x, y;
    
    public Ponto2D( float x, float y ) {
        setCoordenadas( x, y );
    }
    
    public Ponto2D( float[] coordenadas ) {
        if ( coordenadas == null )
            setCoordenadas();
        else
            setCoordenadas( coordenadas );
    }
    
    public Ponto2D( float valor ) {
        setCoordenadas( valor );
    }
    
    public Ponto2D() {
        setCoordenadas();
    }
    
    public void setCoordenadas() {
        x = 0.0f;
        y = 0.0f;
    }
    
    public void setCoordenadas( float valor ) {
        x = valor;
        y = valor;
    }
    
    public void setCoordenadas( float x, float y ) {
        this.x = x;
        this.y = y;
    }
    
    public void setX( float x ) {
        this.x = x;
    }
    
    public void setY( float y ) {
        this.y = y;
    }
    
    public void setCoordenadas( float[] xy ) {
        this.x = 0.0f;
        this.y = 0.0f;
        
        if ( xy == null )
            return;
        
        this.x = xy[0];    
        
        if( xy.length < 2 )
            return;
        
        this.y = xy[1];
    }
    
    public float getX() {
        return x;
    }
    
    public float getY() {
        return y;
    }
    
    public float[] getCoordenadas() {
        return new float[]{ x, y };
    }
    
    public float getDistanciaX( Ponto2D ponto ) {
        return Math.abs( x - ponto.x );
    }
    
    public float getDistanciaY( Ponto2D ponto ) {
        return Math.abs( y - ponto.y );
    }
    
    public float getDistanciaEuclidiana( Ponto2D ponto ) {
        float
            diferencaX = x - ponto.x,
            diferencaY = y - ponto.y;
        
        return (float) Math.sqrt( diferencaX * diferencaX + diferencaY * diferencaY );
    }
    
    public float getDistanciaTabuleiro( Ponto2D ponto ) {
        float
            diferencaX = Math.abs( x - ponto.x ),
            diferencaY = Math.abs( y - ponto.y );
        
        return diferencaX >= diferencaY ? diferencaX : diferencaY ;
    }
    
    public void copiar( Ponto2D ponto ) {
        x = ponto.x;
        y = ponto.y;
    }
    
    public void soma( Ponto2D ponto ) {
        x += ponto.x;
        y += ponto.y;
    }
    
    public void multiplicacaoEscalar( float escalar ) {
        x *= escalar;
        y *= escalar;
    }
    
    @Override
    public String toString() {
        return "( " + (int) x + "; " + (int) y + " )";
    }
    
    @Override
    public Ponto2D clone() {
        return new Ponto2D( x, y );
    }
}