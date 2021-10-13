package aplicativo.otimizacao;

import aplicativo.pontos.Marcador;
import aplicativo.pontos.PontoMarcador;
import aplicativo.pontos.Ponto3D;

public class FuncaoParametrosExtrinsecos implements FuncaoDesempenho {
    private Marcador marcador, marcadorReferencia;
    
    public FuncaoParametrosExtrinsecos(
        Marcador marcador, Marcador marcadorReferencia
    ) {
        setMarcador( marcador );
        setMarcadorReferencia( marcadorReferencia );
    }
    
    public FuncaoParametrosExtrinsecos() {}
    
    public void setMarcador( Marcador marcador ) {
        this.marcador = marcador;
    }
    
    public void setMarcadorReferencia( Marcador marcadorReferencia ) {
        this.marcadorReferencia = marcadorReferencia;
    }
    
    public Marcador getMarcador() {
        return marcador;
    }
    
    public Marcador getMarcadorReferencia() {
        return marcadorReferencia;
    }
    
    @Override
    public float f( float[] x ) {
        if ( marcador == null || marcadorReferencia == null || x == null || x.length < 6 )
            return Float.MAX_VALUE;
        
        float somaQuadrados = 0;
        int numeroLinhas, numeroColunas;
        {
        int
            numeroLinhasMarcador = marcador.getLinhasGrade(),
            numeroLinhasReferencia = marcadorReferencia.getLinhasGrade();
            numeroLinhas = numeroLinhasMarcador <= numeroLinhasReferencia ?
                numeroLinhasMarcador :
                numeroLinhasReferencia;
        int 
            numeroColunasMarcador = marcador.getColunasGrade(),
            numeroColunasReferencia = marcadorReferencia.getColunasGrade();
            numeroColunas = numeroColunasMarcador <= numeroColunasReferencia ?
                numeroColunasMarcador :
                numeroColunasReferencia;
        }
        PontoMarcador pontoMarcador, pontoMarcadorReferencia;
        Ponto3D
            pontoMundo, pontoMundoReferencia,
            pontoEstimado = new Ponto3D();
        float
            xReferencia, yReferencia, zReferencia,
            xEstimado, yEstimado, zEstimado,
            yEstimadoAuxiliar,
            sinX, cosX,
            sinY, cosY,
            sinZ, cosZ;
        int numeroCorrespodencias = 0;
        
        for ( int i = 0; i < numeroLinhas; i++ )
            for ( int j = 0; j < numeroColunas; j++ )
                if (
                    ( pontoMarcador = marcador.getPontoGrade( i, j ) ) != null &&
                    ( pontoMarcadorReferencia = marcadorReferencia.getPontoGrade( i, j ) ) != null
                ) {
                    pontoMundo = pontoMarcador.getPontoMundo();
                    pontoMundoReferencia = pontoMarcadorReferencia.getPontoMundo();
                    
                    xReferencia = pontoMundoReferencia.getX();
                    yReferencia = pontoMundoReferencia.getY();
                    zReferencia = pontoMundoReferencia.getZ();
                    
                    sinX = (float) Math.sin( x[3] ); cosX = (float) Math.cos( x[3] );
                    sinY = (float) Math.sin( x[4] ); cosY = (float) Math.cos( x[4] );
                    sinZ = (float) Math.sin( x[5] ); cosZ = (float) Math.cos( x[5] );
                    
                    xEstimado =  xReferencia * cosZ - yReferencia * sinZ;
                    yEstimado =  xReferencia * sinZ + yReferencia * cosZ;
                    
                    zEstimado = -xEstimado * sinY + zReferencia * cosY;
                    xEstimado =  xEstimado * cosY + zReferencia * sinY;
                    
                    yEstimadoAuxiliar = yEstimado;
                    yEstimado =  yEstimadoAuxiliar * cosX - zEstimado * sinX;
                    zEstimado =  yEstimadoAuxiliar * sinX + zEstimado * cosX;
                    
                    xEstimado += x[0];
                    yEstimado += x[1];
                    zEstimado += x[2];
                    
                    pontoEstimado.setCoordenadas( xEstimado, yEstimado, zEstimado );
                    somaQuadrados += pontoEstimado.getDistancia( pontoMundo );
                    
                    numeroCorrespodencias++;
                }
        
        return numeroCorrespodencias > 0 ? somaQuadrados / numeroCorrespodencias : Float.MAX_VALUE;
    }
}
