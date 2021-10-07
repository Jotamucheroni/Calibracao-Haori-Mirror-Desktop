package aplicativo.otimizacao;

public class EnxameParticulas extends Otimizador {
    private class Particula {
        private float[]
            xMinimo, xMaximo,
            velocidadeMinima, velocidadeMaxima;
        private Ponto atual, melhor;
        private float[] velocidade;
        
        public Particula( float[] xMinimo, float[] xMaximo ) {
            this.xMinimo = xMinimo;
            this.xMaximo = xMaximo;
            
            velocidadeMinima = new float[xMinimo.length];
            velocidadeMaxima = new float[xMinimo.length];
            
            for ( int i = 0; i < xMinimo.length; i++ ) {
                velocidadeMaxima[i] = xMaximo[i] - xMinimo[i];
                velocidadeMinima[i] = -velocidadeMaxima[i];
            }
            
            finalizarInicializacao();
        }
        
        private Particula(
            float[] xMinimo, float[] xMaximo, float[] velocidadeMinima, float[] velocidadeMaxima
        ) {
            this.xMinimo = xMinimo;
            this.xMaximo = xMaximo;
            this.velocidadeMinima = velocidadeMinima;
            this.velocidadeMaxima = velocidadeMaxima;
            
            finalizarInicializacao();
        }
        
        private void finalizarInicializacao() {
            atual = new Ponto( xMinimo.length );
            velocidade = new float[xMinimo.length];
            
            gerarVetorAleatorio( atual.valor, xMinimo, xMaximo );
            gerarVetorAleatorio( velocidade, velocidadeMinima, velocidadeMaxima );
            
            atual.aptidao = funcaoDesempenho.f( atual.valor );
            melhor = atual.clone();
        }
        
        public void limitarVelocidade() {
            limitarVetor( velocidade, velocidadeMinima, velocidadeMaxima );
        }
        
        @Override
        public Particula clone() {
            return new Particula( xMinimo, xMaximo, velocidadeMinima, velocidadeMaxima );
        }
    }
    
    public EnxameParticulas( FuncaoDesempenho funcaoDesempenho ) {
        super( funcaoDesempenho );
    }
    
    public EnxameParticulas( FuncaoDesempenho funcaoDesempenho, float aptidaoMinima ) {
        super( funcaoDesempenho, aptidaoMinima );
    }
    
    public static final int NUMERO_PARTICULAS = 40;
    public static final float
        AC1 = 2.05f,
        AC2 = 2.05f;
    
    @Override
    public float[] otimizar( float[] xMinimo, float[] xMaximo )  {
        Particula[] particula = new Particula[NUMERO_PARTICULAS];
        final float[]
            phi1 = new float[xMinimo.length],
            phi2 = new float[xMinimo.length];
        Ponto otimo;
        
        particula[0] = new Particula( xMinimo, xMaximo );
        otimo = particula[0].melhor.clone();
        
        for ( int i = 1; i < particula.length; i++ ) {
            particula[i] = particula[0].clone();
            
            if ( particula[i].melhor.aptidao < otimo.aptidao )
                otimo = particula[i].melhor.clone();
        }
        
        float w = 1;
        for ( int i = 0; i < 50 && otimo.aptidao >= aptidaoMinima; i++ ) {
            for ( int p = 0; p < particula.length; p++ ) {
                int
                    melhorVizinho = p,
                    vizinho = ( p + 1 >= particula.length ) ? 0 : p + 1;
                
                if ( particula[vizinho].melhor.aptidao < particula[melhorVizinho].melhor.aptidao )
                    melhorVizinho = vizinho;
                
                vizinho = ( p - 1 < 0 ) ? particula.length - 1 : p - 1;
                
                if ( particula[vizinho].melhor.aptidao < particula[melhorVizinho].melhor.aptidao )
                    melhorVizinho = vizinho;
                
                gerarVetorAleatorio( phi1, 0, AC1 );
                gerarVetorAleatorio( phi2, 0, AC2 );
                
                float[]
                    compotamentoCognitivo   =   particula[p].melhor.valor.clone(),
                    compotamentoSocial      =   particula[melhorVizinho].melhor.valor.clone();
                
                subtrairVetor( compotamentoCognitivo, particula[p].atual.valor );
                subtrairVetor( compotamentoSocial, particula[p].atual.valor );
                
                multiplicarPontualVetor( compotamentoCognitivo, phi1 );
                multiplicarPontualVetor( compotamentoSocial, phi2 );
                
                multiplicarEscalarlVetor( w, particula[p].velocidade );
                
                somarVetor( particula[p].velocidade, compotamentoCognitivo );
                somarVetor( particula[p].velocidade, compotamentoSocial );
                
                multiplicarEscalarlVetor( 0.729f, particula[p].velocidade );
                particula[p].limitarVelocidade();
                
                somarVetor( particula[p].atual.valor, particula[p].velocidade );
                limitarVetor( particula[p].atual.valor, xMinimo, xMaximo );
                
                particula[p].atual.aptidao = funcaoDesempenho.f( particula[p].atual.valor );
                if ( particula[p].atual.aptidao < particula[p].melhor.aptidao ) {
                    particula[p].melhor = particula[p].atual.clone();
                    
                    if ( particula[p].melhor.aptidao < otimo.aptidao )
                        otimo = particula[p].melhor.clone();
                }
            }
            
            w *= 0.985f;
        }
        
        return otimo.valor.clone();
    }
}