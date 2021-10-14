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
            xEstimado, yEstimado, zEstimado,
            xEstimadoAuxiliar, yEstimadoAuxiliar, zEstimadoAuxiliar,
            sinX, cosX,
            sinY, cosY,
            sinZ, cosZ,
            distancia,
            somaQuadrados = 0;
        
        for ( int i = 0; i < ponto3D.length; i++ ) {
            pontoMundo = ponto3D[i];
            
            zMundo = pontoMundo.getZ();
            
            xEstimado = pontoMundo.getX() * x[0] / zMundo;
            yEstimado = pontoMundo.getY() * x[1] / zMundo;
            zEstimado = 0;
            
            sinX = (float) Math.sin( x[2] ); cosX = (float) Math.cos( x[2] );
            sinY = (float) Math.sin( x[3] ); cosY = (float) Math.cos( x[3] );
            sinZ = (float) Math.sin( x[4] ); cosZ = (float) Math.cos( x[4] );
            
            xEstimadoAuxiliar = xEstimado;
            yEstimadoAuxiliar = yEstimado;
            xEstimado =  xEstimadoAuxiliar * cosZ - yEstimadoAuxiliar * sinZ;
            yEstimado =  xEstimadoAuxiliar * sinZ + yEstimadoAuxiliar * cosZ;
            
            xEstimadoAuxiliar = xEstimado;
            zEstimadoAuxiliar = zEstimado;
            xEstimado =  xEstimadoAuxiliar * cosY + zEstimadoAuxiliar * sinY;
            zEstimado = -xEstimadoAuxiliar * sinY + zEstimadoAuxiliar * cosY;
            
            yEstimadoAuxiliar = yEstimado;
            zEstimadoAuxiliar = zEstimado;
            yEstimado =  yEstimadoAuxiliar * cosX - zEstimadoAuxiliar * sinX;
            zEstimado =  yEstimadoAuxiliar * sinX + zEstimadoAuxiliar * cosX;
            
            xEstimado += x[5];
            yEstimado += x[6];
            
            pontoEstimado.setCoordenadas( xEstimado, yEstimado );
            distancia = pontoEstimado.getDistanciaTabuleiro( ponto2D[i] );
            somaQuadrados += distancia * distancia;
        }
        
        return somaQuadrados / ponto3D.length;
    }
}