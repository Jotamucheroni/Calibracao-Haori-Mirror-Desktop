package aplicativo.pontos;

import java.util.ArrayList;
import java.util.List;

public class Marcador {
    private class PontoMarcadorColuna {
        private final PontoMarcador pontoMarcador;
        private int coluna;
        
        public PontoMarcadorColuna( PontoMarcador pontoMarcador, int coluna ) {
            this.pontoMarcador = pontoMarcador;
            this.coluna = coluna;
        }
    }
    
    private int linhasGrade, colunasGrade;
    private final PontoMarcador[][] gradePontos;
    
    public Marcador( int linhasGrade, int colunasGrade, List<PontoMarcador> listaPontos ) {
        if ( linhasGrade < 1 )
            linhasGrade = 1;
        
        if ( colunasGrade < 1 )
            colunasGrade = 1;
        
        this.linhasGrade = linhasGrade;
        this.colunasGrade = colunasGrade;
        gradePontos = new PontoMarcador[linhasGrade][colunasGrade];
        
        if ( listaPontos == null || listaPontos.size() == 0 )
            return;
        
        List<PontoMarcadorColuna> listaPosicionado = new ArrayList<PontoMarcadorColuna>();
        {
            PontoMarcador ponto;
            
            for ( int i = listaPontos.size() - 1, coluna = 0; i >= 0 && coluna < colunasGrade; i-- ) {
                if ( ( ponto = listaPontos.get( i ) ) != null )
                    listaPosicionado.add( new PontoMarcadorColuna( ponto, coluna ) );
                else 
                    coluna++;
            }
        }
        
        PontoMarcadorColuna pontoMarcadorPosicionado;
        List<PontoMarcadorColuna> listaLinha = new ArrayList<PontoMarcadorColuna>();
        float yLimite, y;
        
        for ( int linha = 0, coluna; listaPosicionado.size() > 0 && linha < linhasGrade; linha++ ) {
            listaLinha.clear();
            pontoMarcadorPosicionado = listaPosicionado.get( listaPosicionado.size() - 1 );
            coluna = pontoMarcadorPosicionado.coluna;
            listaLinha.add( pontoMarcadorPosicionado );
            
            for ( int i = listaPosicionado.size() - 2; i >= 0; i-- )
                if ( ( pontoMarcadorPosicionado = listaPosicionado.get( i ) ).coluna != coluna  ) {
                    coluna = pontoMarcadorPosicionado.coluna;
                    listaLinha.add( pontoMarcadorPosicionado );
                }
            
            if ( listaLinha.size() == 1 ) {
                pontoMarcadorPosicionado = listaLinha.get( 0 );
                gradePontos[linha][coluna] = pontoMarcadorPosicionado.pontoMarcador;
                listaPosicionado.remove( pontoMarcadorPosicionado );
                continue;
            }    
            
            yLimite = Math.abs(
                    listaLinha.get( 0 ).pontoMarcador.getPontoImagem().x
                -   listaLinha.get( 1 ).pontoMarcador.getPontoImagem().x
            ) / 3;
            listaLinha.sort(
                ( p1, p2 ) -> (int)
                    ( p1.pontoMarcador.getPontoImagem().y - p2.pontoMarcador.getPontoImagem().y )
            );
            yLimite =
                listaLinha.get( ( listaLinha.size() - 1 ) / 2 ).pontoMarcador.getPontoImagem().y - yLimite;
            
            for ( PontoMarcadorColuna ponto : listaLinha ) {
                y = ponto.pontoMarcador.getPontoImagem().y;
                
                if ( y >= yLimite ) {
                    gradePontos[linha][ponto.coluna] = ponto.pontoMarcador;
                    listaPosicionado.remove( ponto );
                }
            }
        }
    }
    
    public Marcador( Marcador marcador ) {
        linhasGrade = marcador.linhasGrade;
        colunasGrade = marcador.colunasGrade;
        gradePontos = new PontoMarcador[linhasGrade][colunasGrade];
        
        PontoMarcador ponto;
        for ( int i = 0; i < linhasGrade; i++ )
            for ( int j = 0; j < colunasGrade; j++ )
                if ( ( ponto = marcador.gradePontos[i][j] ) != null )
                    gradePontos[i][j] = ponto.clone();
    }
    
    public int getLinhasGrade() {
        return linhasGrade;
    }
    
    public int getColunasGrade() {
        return colunasGrade;
    }
    
    public PontoMarcador getPontoGrade( int linha, int coluna ) {
        if ( linha < 0 )
            linha = 0;
        else if ( linha >= linhasGrade )
            linha = linhasGrade - 1;
        
        if ( coluna < 0 )
            coluna = 0;
        else if ( coluna >= colunasGrade )
            coluna = colunasGrade - 1;
        
        return gradePontos[linha][coluna];
    }
        
    @Override
    public Marcador clone() {
        return new Marcador( this );
    }
}
