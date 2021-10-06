package aplicativo.otimizacao;

import java.util.Random;

public abstract class Otimizador {
    protected class Ponto {
        protected float[] valor;
        protected float aptidao;
        
        public Ponto( int numeroCoordenadas ) {
            valor = new float[numeroCoordenadas < 1 ? 1 : numeroCoordenadas ];
        }
        
        private Ponto( float[] valor, float aptidao ) {
            this.valor = valor.clone();
            this.aptidao = aptidao;
        }
        
        @Override
        public Ponto clone() {
            return new Ponto( valor, aptidao );
        }
    }
    
    protected FuncaoDesempenho funcaoDesempenho;
    protected Random sorteador;
    
    protected float aptidaoMinima;
    
    public Otimizador( FuncaoDesempenho funcaoDesempenho, float aptidaoMinima ) {
        this.funcaoDesempenho = funcaoDesempenho;
        this.aptidaoMinima = aptidaoMinima < 0 ? 0 : aptidaoMinima;
        sorteador = new Random( System.currentTimeMillis() );
    }
    
    public Otimizador( FuncaoDesempenho funcaoDesempenho ) {
        this( funcaoDesempenho, 0 );
    }
    
    protected void limitarVetor( float[] x, float[] xMinimo, float[] xMaximo ) {
        for ( int i = 0; i < x.length; i++ )
            if ( x[i] < xMinimo[i] )
                x[i] = xMinimo[i];
            else if ( x[i] > xMaximo[i] )
                x[i] = xMaximo[i];
    }
    
    protected void gerarVetorAleatorio( float[] x, float[] xMinimo, float[] xMaximo  ) {
        for ( int i = 0; i < x.length; i++ )
            x[i] = sorteador.nextFloat() * ( xMaximo[i] - xMinimo[i] ) + xMinimo[i];
    }
    
    protected void gerarVetorAleatorio( float[] x, float xMinimo, float xMaximo  ) {
        for ( int i = 0; i < x.length; i++ )
            x[i] = sorteador.nextFloat() * ( xMaximo - xMinimo ) + xMinimo;
    }
    
    protected void gerarVetorAleatorioGaussiano( float[] x, float[] desvioPadrao ) {
        for ( int i = 0; i < x.length; i++ )
            x[i] = (float) sorteador.nextGaussian() * desvioPadrao[i];
    }
    
    protected void somarVetor( float[] parcelaEsquerda, float[] parcelaDireita ) {
        for ( int i = 0; i < parcelaEsquerda.length; i++ )
            parcelaEsquerda[i] += parcelaDireita[i]; 
    }
    
    protected void subtrairVetor( float[] minuendo, float[] subtraendo ) {
        for ( int i = 0; i < minuendo.length; i++ )
            minuendo[i] -= subtraendo[i]; 
    }
    
    protected void multiplicarPontualVetor( float[] multiplicando, float[] multiplicador ) {
        for ( int i = 0; i < multiplicando.length; i++ )
            multiplicando[i] *= multiplicador[i]; 
    }
    
    protected void multiplicarEscalarlVetor( float escalar, float[] vetor ) {
        for ( int i = 0; i < vetor.length; i++ )
            vetor[i] *= escalar; 
    }
    
    public abstract float[] otimizar( float[] xMinimo, float[] xMaximo );
}