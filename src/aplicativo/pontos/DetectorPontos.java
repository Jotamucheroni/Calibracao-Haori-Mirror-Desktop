package aplicativo.pontos;

import java.nio.ByteBuffer;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class DetectorPontos implements AutoCloseable {
    private final int
        larguraImagem, alturaImagem,
        numeroComponentesCorImagem;
    
    private final ByteBuffer imagem, visImagem;
    
    private final Object travaDetector = new Object();
    private final Thread detector;
    
    private final List<Ponto2D> listaPontosAuxiliar = new ArrayList<Ponto2D>();
    private final List<GrupoPontos> listaGrupoPontosAuxiliar = new ArrayList<GrupoPontos>();
    private final List<Ponto2D> listaPontosAgrupadosAuxiliar = new ArrayList<Ponto2D>();
    
    private final Object travaSaida = new Object();
    private List<Ponto2D> listaPontos = new ArrayList<Ponto2D>();
    private List<Ponto2D> listaPontosAgrupados = new ArrayList<Ponto2D>();
    
    public DetectorPontos( int larguraImagem, int alturaImagem, int numeroComponentesCorImagem ) {
        if ( larguraImagem < 1 )
            larguraImagem = 1;
        this.larguraImagem = larguraImagem;
        
        if ( alturaImagem < 1 )
            alturaImagem = 1;
        this.alturaImagem = alturaImagem;
        
        if ( numeroComponentesCorImagem < 1 )
            numeroComponentesCorImagem = 1;
        else if ( numeroComponentesCorImagem > 4 )
            numeroComponentesCorImagem = 4;
        this.numeroComponentesCorImagem = numeroComponentesCorImagem;
        
        imagem = ByteBuffer.allocateDirect( getNumeroBytesImagem() );
        visImagem = imagem.asReadOnlyBuffer();
        
        detector = new Thread(
            () ->
            {
                final int
                    parLargura = 1 - this.larguraImagem % 2,
                    parAltura = 1 - this.alturaImagem % 2,
                    raioMaximoLargura = ( this.larguraImagem - 1 - parLargura ) / 2,
                    raioMaximoAltura = ( this.alturaImagem - 1 - parAltura ) / 2;
                
                final Ponto2D
                    centroImagem = new Ponto2D(
                        ( this.larguraImagem - 1 - parLargura ) / 2,
                        ( this.alturaImagem - 1 - parAltura ) / 2
                    ),
                    centroRealImagem        =   centroImagem.clone(),
                    origem                  =   new Ponto2D( 0, 0 ),
                    cantoSuperiorEsquerdo   =   new Ponto2D(),
                    cantoSuperiorDireito    =   new Ponto2D(),
                    cantoInferiorEsquerdo   =   new Ponto2D(),
                    cantoInferiorDireito    =   new Ponto2D();
                
                if ( parLargura == 1 )
                    centroRealImagem.x += 0.5;
                if ( parAltura == 1 )
                    centroRealImagem.y += 0.5;
                
                float x, y;
                
                do {
                    // long t = System.currentTimeMillis();
                    
                    listaPontosAuxiliar.clear();
                    listaGrupoPontosAuxiliar.clear();
                    listaPontosAgrupadosAuxiliar.clear();
                    
                    adicionarPonto( 0, centroImagem.x, centroImagem.y );
                    if ( parLargura == 1 )
                        adicionarPonto( 0, centroImagem.x + 1, centroImagem.y );
                    if ( parLargura == 1 && parAltura == 1 )
                        adicionarPonto( 0, centroImagem.x + 1, centroImagem.y + 1 );
                    if ( parAltura == 1 )
                        adicionarPonto( 0, centroImagem.x, centroImagem.y + 1 );
                    
                    int raio;
                    for(
                        raio = 1;
                        raio <= raioMaximoLargura && raio <= raioMaximoAltura;
                        raio += 1
                    ) {
                        cantoSuperiorEsquerdo.setCoordenadas(
                            centroImagem.x - raio, centroImagem.y - raio
                        );
                        cantoSuperiorDireito.setCoordenadas(
                            centroImagem.x + parLargura + raio, centroImagem.y - raio
                        );
                        cantoInferiorDireito.setCoordenadas(
                            centroImagem.x + parLargura + raio, centroImagem.y + parAltura + raio
                        );
                        cantoInferiorEsquerdo.setCoordenadas(
                            centroImagem.x - raio, centroImagem.y + parAltura + raio
                        );
                        
                        for(
                            x = cantoSuperiorEsquerdo.x,
                            y = cantoSuperiorEsquerdo.y;
                            
                            x < cantoSuperiorDireito.x;
                            
                            x++
                        )
                            adicionarPonto( raio, x, y );
                        
                        for( ; y < cantoInferiorDireito.y; y++ )
                            adicionarPonto( raio, x, y );
                        
                        for( ; x > cantoInferiorEsquerdo.x; x-- ) 
                            adicionarPonto( raio, x, y );
                        
                        for( ; y > cantoSuperiorEsquerdo.y; y-- ) 
                            adicionarPonto( raio, x, y );
                    }
                    
                    for ( ; raio <= raioMaximoAltura; raio++ ) {
                        for( int l = 0; l < this.larguraImagem; l++ )
                            adicionarPonto( raio, l, centroImagem.y - raio );
                        
                        for( int l = this.larguraImagem - 1; l >= 0 ; l-- )
                            adicionarPonto( raio, l, centroImagem.y + parAltura + raio );
                    }
                    
                    for ( ; raio <= raioMaximoLargura; raio++ ) {
                        for( int a = 0; a < this.alturaImagem; a++ )
                            adicionarPonto( raio, centroImagem.x + parLargura + raio, a );
                        
                        for( int a = this.alturaImagem - 1; a >= 0; a-- )
                            adicionarPonto( raio, centroImagem.x - raio, a );
                    }
                    
                    for ( GrupoPontos grupo : listaGrupoPontosAuxiliar )
                        if ( grupo.getEstado() == GrupoPontos.Estado.VALIDO ) 
                            listaPontosAgrupadosAuxiliar.add( grupo.getPixelCentral() );
                    
                    deslocarCentro( listaPontosAgrupadosAuxiliar, centroRealImagem );
                    limparEOrdenar( listaPontosAgrupadosAuxiliar, origem );
                    
                    synchronized( travaSaida ) {
                        this.listaPontos =
                            new ArrayList<Ponto2D>( listaPontosAuxiliar );
                        this.listaPontosAgrupados =
                            new ArrayList<Ponto2D>( listaPontosAgrupadosAuxiliar );
                    }
                    
                    /* System.out.println( 
                        "Quadros/s: " + 1 / ( (float) ( System.currentTimeMillis() - t ) / 1000 )
                    ); */
                    
                    synchronized( travaDetector ) {
                        try {
                            travaDetector.wait();
                        } catch ( InterruptedException ignorada ) {
                            return;
                        }
                    }
                } while ( !Thread.currentThread().isInterrupted() );
            }
        );
    }
    
    public DetectorPontos( int larguraImagem, int alturaImagem ) {
        this( larguraImagem, alturaImagem, 4 );
    }
    
    private final ArrayList<GrupoPontos> listaGruposExclusao = new ArrayList<GrupoPontos>();
    
    private void adicionarPonto( int iteracao, float x, float y ) {
        visImagem.position( (int) ( y * this.larguraImagem + x ) );
        if ( Byte.toUnsignedInt( visImagem.get() ) == 255 ) {
            Ponto2D ponto = new Ponto2D( x, y );
            
            listaPontosAuxiliar.add( ponto );
            
            listaGruposExclusao.clear();
            boolean adicionado = false;
            for ( GrupoPontos grupo : listaGrupoPontosAuxiliar ) {
                GrupoPontos.Resultado resultado = grupo.adicionar( ponto, iteracao );
                
                if ( resultado == GrupoPontos.Resultado.ADICIONADO ) {
                    adicionado = true;
                    break;
                }
                else if ( resultado == GrupoPontos.Resultado.ENCERRADO )
                    listaGruposExclusao.add( grupo );
            }
            
            if ( !adicionado ) {
                GrupoPontos grupo = new GrupoPontos();
                grupo.adicionar( ponto );
                
                listaGrupoPontosAuxiliar.add( grupo );
            }
            
            for ( GrupoPontos grupo : listaGruposExclusao ) {
                listaGrupoPontosAuxiliar.remove( grupo );
                if ( grupo.getEstado() == GrupoPontos.Estado.VALIDO )
                    listaPontosAgrupadosAuxiliar.add( grupo.getPixelCentral() );
            }
        }
    }
    
    private void deslocarCentro( List<Ponto2D> listaPontos, Ponto2D novoCentro ) {
        Ponto2D deslocamento = novoCentro.clone();
        deslocamento.multiplicacaoEscalar( -1 );
        
        for ( Ponto2D ponto : listaPontos )
            ponto.soma( deslocamento );
    }
    
    private int
        maximoColunasAEsquerda = 5,
        maximoColunasADireita = 5,
        maximoLinhasAcima = 3,
        maximoLinhasAbaixo = 3;
    
    public void setMaximoColunasAEsquerda( int numeroColunas ) {
        if ( numeroColunas < 0 )
            numeroColunas = 0;
        
        maximoColunasAEsquerda = numeroColunas;
    }
    
    public void setMaximoColunasADireita( int numeroColunas ) {
        if ( numeroColunas < 0 )
            numeroColunas = 0;
        
        maximoColunasADireita = numeroColunas;
    }
    
    public void setMaximoLinhasAcima( int numeroLinhas ) {
        if ( numeroLinhas < 0 )
            numeroLinhas = 0;
        
        maximoLinhasAcima = numeroLinhas;
    }
    
    public void setMaximoLinhasAbaixo( int numeroLinhas ) {
        if ( numeroLinhas < 0 )
            numeroLinhas = 0;
        
        maximoLinhasAbaixo = numeroLinhas;
    }
    
    public int getMaximoColunasAEsquerda() {
        return maximoColunasAEsquerda;
    }
    
    public int getMaximoColunasADireita() {
        return maximoColunasADireita;
    }
    
    public int getMaximoLinhasAcima() {
        return maximoLinhasAcima;
    }
    
    public int getMaximoLinhasAbaixo() {
        return maximoLinhasAbaixo;
    }
    
    private int minimoPontosColuna = maximoLinhasAcima;
    
    public void setMinimoPontosColuna( int numeroPontos ) {
        if ( numeroPontos < 0 )
            numeroPontos = 0;
        
        minimoPontosColuna = numeroPontos;
    }
    
    public int getMinimoPontosColuna() {
        return minimoPontosColuna;
    }
    
    private void removerNulos( List<Ponto2D> listaPontos ) {
        for ( int i = 1; i < listaPontos.size(); i++ )
            if ( listaPontos.get( i ) == null && listaPontos.get( i - 1 ) == null ) {
                listaPontos.remove( i );
                i--;
            }
        
        if ( listaPontos.size() > 0 && listaPontos.get( listaPontos.size() - 1 ) == null )
            listaPontos.remove( listaPontos.size() - 1 );
        
        if ( listaPontos.size() > 0 && listaPontos.get( 0 ) == null )
            listaPontos.remove( 0 );
    }
    
    private void limparEOrdenar( List<Ponto2D> listaPontos, Ponto2D referencia ) {
        if ( listaPontos == null || listaPontos.size() < 4 )
            return;
        
        Ponto2D
            cantoSuperiorEsquerdo = null,
            cantoSuperiorDireito = null,
            cantoInferiorDireito = null,
            cantoInferiorEsquerdo = null;
        
        for ( Ponto2D ponto : listaPontos ) {
            if (
                ponto.x <= referencia.x    &&
                ponto.y >= referencia.y    &&
                
                (
                    cantoSuperiorEsquerdo == null ||
                    
                    referencia.getDistanciaTabuleiro( ponto ) <
                    referencia.getDistanciaTabuleiro( cantoSuperiorEsquerdo )
                )
            )
                cantoSuperiorEsquerdo = ponto;
            
            else if (
                ponto.x > referencia.x     &&
                ponto.y >= referencia.y    &&
                
                (
                    cantoSuperiorDireito == null ||
                    
                    referencia.getDistanciaTabuleiro( ponto ) <
                    referencia.getDistanciaTabuleiro( cantoSuperiorDireito )
                )
            )
                cantoSuperiorDireito = ponto;
            
            else if (
                ponto.x > referencia.x     &&
                ponto.y < referencia.y     &&
                
                (
                    cantoInferiorDireito == null ||
                    
                    referencia.getDistanciaTabuleiro( ponto ) <
                    referencia.getDistanciaTabuleiro( cantoInferiorDireito )
                )
            )
                cantoInferiorDireito = ponto;
            
            else if (
                ponto.x <= referencia.x    &&
                ponto.y < referencia.y     &&
                
                (
                    cantoInferiorEsquerdo == null ||
                    
                    referencia.getDistanciaTabuleiro( ponto ) <
                    referencia.getDistanciaTabuleiro( cantoInferiorEsquerdo )
                )
            )
                cantoInferiorEsquerdo = ponto;
        }
        
        if (
            cantoSuperiorEsquerdo   ==  null    ||
            cantoSuperiorDireito    ==  null    ||
            cantoInferiorDireito    ==  null    ||
            cantoInferiorEsquerdo   ==  null
        )
            return;
        
        float
            ladoQuadradoAux = cantoSuperiorDireito.x - cantoSuperiorEsquerdo.x,
            ladoQuadrado = ladoQuadradoAux;
        
        if (
            ( ladoQuadradoAux = cantoSuperiorDireito.y - cantoInferiorDireito.y ) > ladoQuadrado
        )
            ladoQuadrado = ladoQuadradoAux;
        if (
            ( ladoQuadradoAux = cantoInferiorDireito.x - cantoInferiorEsquerdo.x ) > ladoQuadrado
        )
            ladoQuadrado = ladoQuadradoAux;
        if (
            ( ladoQuadradoAux = cantoSuperiorEsquerdo.y - cantoInferiorEsquerdo.y ) > ladoQuadrado
        )
            ladoQuadrado = ladoQuadradoAux;
        
        final float 
            diferencaMaxima = ladoQuadrado / 3,
            limiteEsquerdo = (
                cantoSuperiorEsquerdo.x < cantoInferiorEsquerdo.x ?
                    cantoSuperiorEsquerdo.x :
                    cantoInferiorEsquerdo.x
            ) - ( maximoColunasAEsquerda - 1 ) * ladoQuadrado - diferencaMaxima,
            limiteDireito = (
                cantoSuperiorDireito.x > cantoInferiorDireito.x ?
                    cantoSuperiorDireito.x :
                    cantoInferiorDireito.x
            ) + ( maximoColunasADireita - 1 ) * ladoQuadrado + diferencaMaxima,
            limiteSuperior = (
                cantoSuperiorEsquerdo.y > cantoSuperiorDireito.y ?
                    cantoSuperiorEsquerdo.y :
                    cantoSuperiorDireito.y
            ) + ( maximoLinhasAcima - 1 ) * ladoQuadrado + diferencaMaxima,
            limiteInferior = (
                cantoInferiorEsquerdo.y < cantoInferiorDireito.y ?
                    cantoInferiorEsquerdo.y :
                    cantoInferiorDireito.y
            ) - ( maximoLinhasAbaixo - 1 ) * ladoQuadrado - diferencaMaxima;
        
        listaPontos.removeIf(
            ponto -> 
                ponto.x < limiteEsquerdo    ||
                ponto.x > limiteDireito     ||
                ponto.y > limiteSuperior    ||
                ponto.y < limiteInferior
        );
        
        if ( listaPontos.size() < 4 )
            return;
        
        listaPontos.sort(
            ( p1, p2 ) -> 
            {
                if ( Math.abs( p1.x - p2.x ) < diferencaMaxima )
                    return (int) ( p2.y - p1.y );
                
                return (int) ( p1.x - p2.x );
            }
        );
        
        Ponto2D pontoAtual, pontoAnterior;
        List<Float>
            listaDistancia = new ArrayList<Float>(),
            listaPrimeiraLinha = new ArrayList<Float>(),
            listaUltimaLinha = new ArrayList<Float>();
        
        listaPrimeiraLinha.add( listaPontos.get( 0 ).y );
        for( int i = 1; i < listaPontos.size(); i++ ) {
            pontoAtual = listaPontos.get( i );
            pontoAnterior = listaPontos.get( i - 1 );
            
            if (
                Math.abs( pontoAtual.x - pontoAnterior.x ) > diferencaMaxima
            ) {
                listaPrimeiraLinha.add( pontoAtual.y );
                listaUltimaLinha.add( pontoAnterior.y );
                listaPontos.add( i, null );
                i++;
            }
            else
                listaDistancia.add( pontoAnterior.y - pontoAtual.y );
        }
        listaUltimaLinha.add( listaPontos.get( listaPontos.size() - 1 ).y );
        
        listaDistancia.sort( ( d1, d2 ) -> (int) ( d1 - d2 ) );
        listaPrimeiraLinha.sort( ( d1, d2 ) -> (int) ( d1 - d2 ) );
        listaUltimaLinha.sort( ( d1, d2 ) -> (int) ( d1 - d2 ) );
        
        if (
            listaDistancia.size() == 0      || 
            listaPrimeiraLinha.size() == 0  ||
            listaUltimaLinha.size() == 0
        )
            return;
        
        final float
            diferencaMaximaFinal = listaDistancia.get( ( listaDistancia.size() - 1 ) / 2 ) / 3,
            limiteSuperiorFinal = listaPrimeiraLinha.get( ( listaPrimeiraLinha.size() - 1 ) / 2 )
                + diferencaMaximaFinal,
            limiteInferiorFinal = listaUltimaLinha.get( ( listaUltimaLinha.size() - 1 ) / 2 )
                - diferencaMaximaFinal;
        
        listaPontos.removeIf(
            ponto -> 
                ponto != null &&
                (
                    ponto.y > limiteSuperiorFinal    ||
                    ponto.y < limiteInferiorFinal
                )
        );
        
        removerNulos( listaPontos );
        
        if ( listaPontos.size() < 4 )
            return;
        
        Ponto2D pontoLista, pontoColuna, pontoMinimo;
        
        List<Ponto2D> coluna = new ArrayList<Ponto2D>();
        List<Ponto2D> listaRemocao = new ArrayList<Ponto2D>();
        
        float distancia, distanciaMinima;
        
        for ( int i = 0; i < listaPontos.size(); i++ ) {
            pontoLista = listaPontos.get( i );
            
            if ( pontoLista == null || i == listaPontos.size() - 1 ) {
                if ( i == listaPontos.size() - 1 )
                    coluna.add( pontoLista );
                
                while ( coluna.size() > 4 ) {
                    distanciaMinima = -1;
                    pontoMinimo = null;
                    
                    for ( int j = 1; j < coluna.size() - 1; j++ ) {
                        pontoColuna = coluna.get( j );
                        
                        distancia = 
                                ( coluna.get( j - 1 ).y - pontoColuna.y )
                            +   ( pontoColuna.y -  coluna.get( j + 1 ).y );
                        
                        if ( distancia < distanciaMinima || pontoMinimo == null ) {
                            distanciaMinima = distancia;
                            pontoMinimo = pontoColuna;
                        }
                    }
                    
                    coluna.remove( pontoMinimo );
                    listaRemocao.add( pontoMinimo );
                }
                
                if ( coluna.size() < minimoPontosColuna )
                    listaRemocao.addAll( coluna );
                
                coluna.clear();
                
                continue;
            }
            
            coluna.add( pontoLista );
        }
        
        if ( coluna.size() < minimoPontosColuna )
            listaRemocao.addAll( coluna );
        
        listaPontos.removeAll( listaRemocao );
        
        removerNulos( listaPontos );
    }
    
    public int getLarguraImagem() {
        return larguraImagem;
    }
    
    public int getAlturaImagem() {
        return alturaImagem;
    }
    
    public int getNumeroComponentesCorImagem() {
        return numeroComponentesCorImagem;
    }
    
    public int getNumeroPixeisImagem() {
        return larguraImagem * alturaImagem;
    }
    
    public int getNumeroBytesImagem() {
        return getNumeroPixeisImagem() * numeroComponentesCorImagem;
    }
    
    public ByteBuffer getImagem() {
        if ( imagem == null )
            return null;
        
        imagem.rewind();
        
        return imagem;
    }
    
    public int getNumeroPontos() {
        synchronized ( travaSaida ) {
            return listaPontos.size();
        }
    }
    
    public List<Ponto2D> getListaPontos() {
        synchronized ( travaSaida ) {
            return Collections.unmodifiableList( listaPontos );
        }
    }
    
    public List<Ponto2D> getListaPontosAgrupados() {
        synchronized ( travaSaida ) {
            return Collections.unmodifiableList( listaPontosAgrupados );
        }
    }
    
    public boolean ocupado() {
        return detector.isAlive() && detector.getState() != Thread.State.WAITING;
    }
    
    public void executar() {
        if ( ocupado() )
            return;
        
        if ( !detector.isAlive() )
            detector.start();
        else
            synchronized( travaDetector ) {
                travaDetector.notify();
            }
    }
    
    @Override
    public void close() {
        detector.interrupt();
    }
}