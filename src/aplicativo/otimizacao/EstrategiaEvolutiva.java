package aplicativo.otimizacao;

public class EstrategiaEvolutiva extends Otimizador {
    public EstrategiaEvolutiva( FuncaoDesempenho funcaoDesempenho ) {
        super( funcaoDesempenho );
    }
    
    public EstrategiaEvolutiva( FuncaoDesempenho funcaoDesempenho, float aptidaoMinima ) {
        super( funcaoDesempenho, aptidaoMinima );
    }
    
    private void selecionar( Ponto[] individuo ) {
        Ponto[] individuoSelecionado = new Ponto[individuo.length];
        Ponto vencedorTorneio;
        int sorteio;
        
        for ( int i = 0; i < individuo.length; i++ ) {
            vencedorTorneio = individuo[sorteador.nextInt( individuo.length )];
            
            for ( int j = 1; j < 6; j++ ) {
                sorteio = sorteador.nextInt( individuo.length );
                
                if ( individuo[sorteio].aptidao < vencedorTorneio.aptidao )
                    vencedorTorneio = individuo[sorteio];
            }
            
            individuoSelecionado[i] = vencedorTorneio.clone();
        }
        
        for ( int i = 0; i < individuo.length; i++ )
            individuo[i] = individuoSelecionado[i];
    }
    
    private void recombinar( Ponto[] individuo ) {
        float[] novoValor1, novoValor2;
        
        for( int i = 0; i < individuo.length - 1; i += 2 )
            if ( sorteador.nextFloat() < 0.7 ) {
                novoValor1 = individuo[i].valor.clone();
                subtrairVetor( novoValor1, individuo[i + 1].valor );
                novoValor2 = novoValor1.clone();
                
                multiplicarEscalarlVetor( sorteador.nextFloat() * 2 - 0.5f, novoValor1 );
                multiplicarEscalarlVetor( sorteador.nextFloat() * 2 - 0.5f, novoValor2 );
                
                somarVetor( novoValor1, individuo[i + 1].valor );
                somarVetor( novoValor2, individuo[i + 1].valor );
                
                individuo[i].valor = novoValor1;
                individuo[i + 1].valor = novoValor2;
            }
    }
    
    private void mutar( Ponto[] individuo, float taxaMutacao, float[] desvioPadrao ) {
        float[] vetorMutacao = new float[desvioPadrao.length];
        
        for ( int i = 0; i < individuo.length; i++ )
            if ( sorteador.nextFloat() < taxaMutacao ) {
                gerarVetorAleatorioGaussiano( vetorMutacao, desvioPadrao );
                somarVetor( individuo[i].valor, vetorMutacao );
            }
                
    }
    
    public static final int NUMERO_INDIVIDUOS = 375;
    
    @Override
    public float[] otimizar( float[] xMinimo, float[] xMaximo ) {
        Ponto[] individuo = new Ponto[NUMERO_INDIVIDUOS];
        float[] desvioPadrao = new float[xMinimo.length];
        Ponto otimo;
        
        for ( int i = 0; i < xMinimo.length; i++ )
            desvioPadrao[i] = 0.21f * ( xMaximo[i] - xMinimo[i] ); 
        
        individuo[0] = new Ponto( xMinimo.length );
        gerarVetorAleatorio( individuo[0].valor, xMinimo, xMaximo );
        individuo[0].aptidao = funcaoDesempenho.f( individuo[0].valor );
        otimo = individuo[0].clone();
        
        for ( int i = 1; i < individuo.length; i++ ) {
            individuo[i] = new Ponto( xMinimo.length );
            gerarVetorAleatorio( individuo[i].valor, xMinimo, xMaximo );
            individuo[i].aptidao = funcaoDesempenho.f( individuo[i].valor );
            
            if ( individuo[i].aptidao < otimo.aptidao )
                otimo = individuo[i].clone();
        }
        
        float taxaMutacao = 1.0f;
        for ( int i = 0; i < 50 && otimo.aptidao >= aptidaoMinima; i++ ) {
            selecionar( individuo );
            recombinar( individuo );
            
            for ( Ponto ponto : individuo ) {
                limitarVetor( ponto.valor, xMinimo, xMaximo );
                ponto.aptidao = funcaoDesempenho.f( ponto.valor );
                
                if ( ponto.aptidao < otimo.aptidao )
                    otimo = ponto.clone();
            }
            
            mutar( individuo, taxaMutacao, desvioPadrao );
            
            for ( Ponto ponto : individuo ) {
                limitarVetor( ponto.valor, xMinimo, xMaximo );
                ponto.aptidao = funcaoDesempenho.f( ponto.valor );
                
                if ( ponto.aptidao < otimo.aptidao )
                    otimo = ponto.clone();
            }
            
            taxaMutacao *= 0.981f;
        }
        
        return otimo.valor.clone();
    }
}