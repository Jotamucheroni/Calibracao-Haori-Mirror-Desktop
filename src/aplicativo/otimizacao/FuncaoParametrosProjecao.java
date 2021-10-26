package aplicativo.otimizacao;

import aplicativo.pontos.Ponto3D;
import aplicativo.pontos.Ponto2D;

public class FuncaoParametrosProjecao implements FuncaoDesempenho {
    private Ponto3D superiorEsquerdo3D, superiorDireito3D, inferiorDireito3D, inferiorEsquerdo3D;
    private Ponto2D superiorEsquerdo2D, superiorDireito2D, inferiorDireito2D, inferiorEsquerdo2D;
    
    public FuncaoParametrosProjecao(
        Ponto3D superiorEsquerdo3D, Ponto3D superiorDireito3D,
        Ponto3D inferiorDireito3D, Ponto3D inferiorEsquerdo3D,
        
        Ponto2D superiorEsquerdo2D, Ponto2D superiorDireito2D,
        Ponto2D inferiorDireito2D, Ponto2D inferiorEsquerdo2D
    ) {
        setPontos3D( superiorEsquerdo3D, superiorDireito3D, inferiorDireito3D, inferiorEsquerdo3D );
        setPontos2D( superiorEsquerdo2D, superiorDireito2D, inferiorDireito2D, inferiorEsquerdo2D );
    }
    
    public FuncaoParametrosProjecao() {}
    
    public void setPontos3D(
        Ponto3D superiorEsquerdo3D, Ponto3D superiorDireito3D,
        Ponto3D inferiorDireito3D, Ponto3D inferiorEsquerdo3D
    ) {
        this.superiorEsquerdo3D = superiorEsquerdo3D;
        this.superiorDireito3D = superiorDireito3D;
        this.inferiorDireito3D = inferiorDireito3D;
        this.inferiorEsquerdo3D = inferiorEsquerdo3D;
    }
    
    public void setPontos2D(
        Ponto2D superiorEsquerdo2D, Ponto2D superiorDireito2D,
        Ponto2D inferiorDireito2D, Ponto2D inferiorEsquerdo2D
    ) {
        this.superiorEsquerdo2D = superiorEsquerdo2D;
        this.superiorDireito2D = superiorDireito2D;
        this.inferiorDireito2D = inferiorDireito2D;
        this.inferiorEsquerdo2D = inferiorEsquerdo2D;
    }
    
    @Override
    public float f( float[] x ) {
        if (
            superiorEsquerdo3D == null || superiorDireito3D == null ||
            inferiorDireito3D == null || inferiorEsquerdo3D == null ||
            
            superiorEsquerdo2D == null || superiorDireito2D == null ||
            inferiorDireito2D == null || inferiorEsquerdo2D == null ||
            
            x == null || x.length < 7
        )
            return Float.MAX_VALUE;
        
        Ponto3D[] ponto3D = { superiorEsquerdo3D, superiorDireito3D, inferiorDireito3D, inferiorEsquerdo3D };
        Ponto2D[] ponto2D = { superiorEsquerdo2D, superiorDireito2D, inferiorDireito2D, inferiorEsquerdo2D };
        
        Ponto3D pontoMundo;
        Ponto2D pontoEstimado = new Ponto2D();
        float
            zMundo,
            distancia,
            somaQuadrados = 0;
        
        for ( int i = 0; i < ponto3D.length; i++ ) {
            pontoMundo = ponto3D[i];
            zMundo = pontoMundo.getZ();
            
            pontoEstimado.setCoordenadas(
                pontoMundo.getX() * x[0] / zMundo, pontoMundo.getY() * x[1] / zMundo
            );
            pontoEstimado.rotacionar( x[2], x[3], x[4] );
            pontoEstimado.transladar( x[5] / ( zMundo * zMundo ), x[6] / ( zMundo * zMundo ) );
            
            distancia = pontoEstimado.getDistanciaTabuleiro( ponto2D[i] );
            somaQuadrados += distancia * distancia;
        }
        
        return somaQuadrados / ponto3D.length;
    }
}