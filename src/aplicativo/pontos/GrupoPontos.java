package aplicativo.pontos;

import java.util.ArrayDeque;
import java.util.Iterator;

public class GrupoPontos {
    public enum Resultado {
        ADICIONADO, REJEITADO, ENCERRADO;
    }
    
    public enum Estado {
        VAZIO, VALIDO, INVALIDO
    }
    
    private int raioAgrupamento;
    private int maximoPontos;
    
    private ArrayDeque<Ponto2D> pilha = new ArrayDeque<Ponto2D>();
    private int iteracaoUltimaAdicao = -1;
    private Ponto2D pontoMaximo, pontoMinimo;
    
    public GrupoPontos( int raioAgrupamento, int maximoPontos ) {
        if ( raioAgrupamento < 0 )
            raioAgrupamento = 0;
        this.raioAgrupamento = raioAgrupamento;
        
        if ( maximoPontos < 1 )
            maximoPontos = 1;
        this.maximoPontos = maximoPontos;
    }
    
    public GrupoPontos() {
        this.raioAgrupamento = 3;
        this.maximoPontos = 9;
    }
    
    public int getRaioAgrupamento() {
        return raioAgrupamento;
    }
    
    public Resultado adicionar( Ponto2D ponto ) {
        if ( pilha.size() == 0 ) {
            pilha.add( ponto );
            pontoMaximo = ponto.clone();
            pontoMinimo = ponto.clone();
            
            return Resultado.ADICIONADO;
        }
        
        if (
            ( pontoMinimo.x - ponto.x ) > raioAgrupamento ||
            ( ponto.x - pontoMaximo.x ) > raioAgrupamento ||
            ( pontoMinimo.y - ponto.y ) > raioAgrupamento ||
            ( ponto.y - pontoMaximo.y ) > raioAgrupamento
        )
            return Resultado.REJEITADO;
        
        for ( Iterator<Ponto2D> iterator = pilha.descendingIterator(); iterator.hasNext(); ) {
            Ponto2D pontoPilha = iterator.next();
            
            if ( ponto.getDistanciaTabuleiro( pontoPilha ) <= raioAgrupamento ) {
                pilha.add( ponto );
                
                if ( ponto.x < pontoMinimo.x )
                    pontoMinimo.x = ponto.x;
                else if ( ponto.x > pontoMaximo.x )
                    pontoMaximo.x = ponto.x;
                
                if ( ponto.y < pontoMinimo.y )
                    pontoMinimo.y = ponto.y;
                else if ( ponto.y > pontoMaximo.y )
                    pontoMaximo.y = ponto.y;    
                
                return Resultado.ADICIONADO;
            }
        }
        
        return Resultado.REJEITADO;
    }
    
    public Resultado adicionar( Ponto2D ponto, int iteracao ) {
        if( iteracaoUltimaAdicao >= 0 && ( iteracao - iteracaoUltimaAdicao ) > 3  )
            return Resultado.ENCERRADO;
        
        Resultado resultado = adicionar( ponto );
        
        if ( resultado == Resultado.ADICIONADO )
            iteracaoUltimaAdicao = iteracao;
        
        return resultado;
    }
    
    public Ponto2D getPixelCentral() {
        Ponto2D pontoCentral =  new Ponto2D( 0, 0 );
        
        if ( pilha.size() == 0 )
            return pontoCentral;
        
        for( Ponto2D ponto : pilha )
            pontoCentral.soma( ponto );
        
        pontoCentral.multiplicacaoEscalar( 1.0f / pilha.size() );
        
        return pontoCentral;
    }
    
    public Estado getEstado() {
        if ( pilha.size() == 0 )
            return Estado.VAZIO;
        
        if ( pilha.size() <= maximoPontos )
            return Estado.VALIDO;
        
        return Estado.INVALIDO;
    }
}