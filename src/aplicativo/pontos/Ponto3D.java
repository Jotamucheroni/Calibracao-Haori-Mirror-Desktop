package aplicativo.pontos;

public class Ponto3D extends Ponto2D {
    protected float z;
    
    public Ponto3D( float x, float y, float z ) {
        setCoordenadas( x, y, z );
    }
    
    public Ponto3D( float[] coordenadas ) {
        if ( coordenadas == null )
            setCoordenadas();
        else
            setCoordenadas( coordenadas );
    }
    
    public Ponto3D( float valor ) {
        setCoordenadas( valor );
    }
    
    public Ponto3D() {
        setCoordenadas();
    }
    
    @Override
    public void setCoordenadas() {
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }
    
    @Override
    public void setCoordenadas( float valor ) {
        x = valor;
        y = valor;
        z = valor;
    }
    
    public void setCoordenadas( float x, float y, float z ) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void setZ( float z ) {
        this.z = z;
    }
    
    @Override
    public void setCoordenadas( float[] xyz ) {
        this.x = 0.0f;
        this.y = 0.0f;
        this.z = 0.0f;
        
        if ( xyz == null )
            return;
        
        this.x = xyz[0];    
        
        if( xyz.length < 2 )
            return;
        
        this.y = xyz[1];
        
        if( xyz.length < 3 )
            return;
        
        this.z = xyz[2];
    }
    
    public float getZ() {
        return z;
    }
    
    @Override
    public float[] getCoordenadas() {
        return new float[]{ x, y, z };
    }
    
    public float getDistanciaZ( Ponto3D ponto ) {
        return Math.abs( z - ponto.z );
    }
    
    public float getDistancia( Ponto3D ponto ) {
        float
            diferencaX = x - ponto.x,
            diferencaY = y - ponto.y,
            diferencaZ = z - ponto.z;
        
        return (float) Math.sqrt(
            diferencaX * diferencaX + diferencaY * diferencaY + diferencaZ * diferencaZ
        );
    }
    
    public void copiar( Ponto3D ponto ) {
        x = ponto.x;
        y = ponto.y;
        z = ponto.z;
    }
    
    public void soma( Ponto3D ponto ) {
        x += ponto.x;
        y += ponto.y;
        z += ponto.z;
    }
    
    @Override
    public void multiplicacaoEscalar( float escalar ) {
        x *= escalar;
        y *= escalar;
        z *= escalar;
    }
    
    @Override
    public String toString() {
        return "( " + x + ", " + y + ", " + z + " )";
    }
    
    @Override
    public Ponto3D clone() {
        return new Ponto3D( x, y, z );
    }
}
