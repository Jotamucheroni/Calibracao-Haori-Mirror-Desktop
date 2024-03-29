package aplicativo.otimizacao;

import aplicativo.pontos.Ponto2D;
import aplicativo.pontos.Ponto3D;
import aplicativo.pontos.PontoMarcador;

public class FuncaoParametrosIntrinsecos implements FuncaoDesempenho {
    private PontoMarcador[] pontoMarcador;
    
    public FuncaoParametrosIntrinsecos( PontoMarcador[] pontoMarcador ) {
        this.pontoMarcador = pontoMarcador;
    }
    
    public FuncaoParametrosIntrinsecos() {}
    
    public void setPontoMarcador( PontoMarcador[] pontoMarcador ) {
        this.pontoMarcador = pontoMarcador;
    }
    
    public PontoMarcador[] getPontoMarcador() {
        return pontoMarcador;
    }
    
    @Override
    public float f( float[] x ) {
        if ( pontoMarcador == null || x == null || x.length < 2 )
            return Float.MAX_VALUE;
        
        Ponto3D pontoMundo;
        Ponto2D pontoEstimado = new Ponto2D();
        float
            z,
            distancia,
            somaQuadrados = 0;
        
        for ( PontoMarcador ponto : pontoMarcador ) {
            pontoMundo = ponto.getPontoMundo();
            
            z = pontoMundo.getZ();
            pontoEstimado.setCoordenadas( pontoMundo.getX() * x[0] / z, pontoMundo.getY() * x[1] / z );
            distancia = pontoEstimado.getDistanciaTabuleiro( ponto.getPontoImagem() );
            
            somaQuadrados += distancia * distancia;
        }
        
        return pontoMarcador.length > 0 ? somaQuadrados / pontoMarcador.length : Float.MAX_VALUE;
    }
}