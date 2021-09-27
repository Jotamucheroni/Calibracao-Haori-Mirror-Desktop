package aplicativo.pontos;

public class PontoMarcador {
    private Ponto2D pontoImagem;
    private Ponto3D pontoMundo;
    
    public PontoMarcador( Ponto2D pontoImagem, Ponto3D pontoMundo ) {
        this.pontoImagem = pontoImagem.clone();
        this.pontoMundo = pontoMundo.clone();
    }
    
    public PontoMarcador() {
        this.pontoImagem = new Ponto2D();
        this.pontoMundo = new Ponto3D();
    }
    
    public Ponto2D getPontoImagem() {
        return pontoImagem;
    }
    
    public Ponto2D getPontoMundo() {
        return pontoMundo;
    }
}