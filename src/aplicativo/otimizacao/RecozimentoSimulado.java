package aplicativo.otimizacao;

public class RecozimentoSimulado extends Otimizador {
    public RecozimentoSimulado( FuncaoDesempenho funcaoDesempenho ) {
        super( funcaoDesempenho );
    }
    
    public RecozimentoSimulado( FuncaoDesempenho funcaoDesempenho, float aptidaoMinima ) {
        super( funcaoDesempenho, aptidaoMinima );
    }
    
    public static final int NUMERO_PARTICULAS = 50;
    
    @Override
    public float[] otimizar( float[] xMinimo, float[] xMaximo ) {
        Ponto[] particula = new Ponto[NUMERO_PARTICULAS];
        float[] desvioPadrao = new float[xMinimo.length];
        Ponto otimo;
        
        for ( int i = 0; i < xMinimo.length; i++ )
            desvioPadrao[i] = 0.0103f * ( xMaximo[i] - xMinimo[i] ); 
        
        particula[0] = new Ponto( xMinimo.length );
        gerarVetorAleatorio( particula[0].valor, xMinimo, xMaximo );
        particula[0].aptidao = funcaoDesempenho.f( particula[0].valor );
        otimo = particula[0].clone();
        
        for ( int i = 1; i < particula.length; i++ ) {
            particula[i] = new Ponto( xMinimo.length );
            gerarVetorAleatorio( particula[i].valor, xMinimo, xMaximo );
            particula[i].aptidao = funcaoDesempenho.f( particula[i].valor );
            
            if ( particula[i].aptidao < otimo.aptidao )
                otimo = particula[i].clone();
        }
        
        float t0 = 0.000000009f;
        float[] vetorPerturbacao = new float[xMinimo.length];
        
        lacoPrincipal:
        for ( int i = 0; i < 20; i++ ) {
            for ( int j = 0; j < 100; j++ ) {
                for ( int p = 0; p < particula.length; p++ ) {
                    Ponto pontoAntigo = particula[p].clone();
                    
                    gerarVetorAleatorioGaussiano( vetorPerturbacao, desvioPadrao );
                    somarVetor( particula[p].valor, vetorPerturbacao );
                    limitarVetor( particula[p].valor, xMinimo, xMaximo);
                    particula[p].aptidao = funcaoDesempenho.f( particula[p].valor );
                    
                    if ( particula[p].aptidao < pontoAntigo.aptidao ) {
                        if ( particula[p].aptidao < otimo.aptidao ) {
                            otimo = particula[p].clone();
                        
                            if ( otimo.aptidao < aptidaoMinima )
                                break lacoPrincipal;
                        }
                    }
                    else if (
                        ! (
                            sorteador.nextFloat() <
                            (float) Math.exp( ( pontoAntigo.aptidao - particula[p].aptidao ) / t0 )
                        )
                    )
                        particula[p] = pontoAntigo;
                }
            }
            
            t0 *= 0.753f;
        }
        
        return otimo.valor.clone();
    }
}