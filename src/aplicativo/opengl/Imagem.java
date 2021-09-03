package aplicativo.opengl;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Imagem extends Textura {
    private String caminhoArquivo;
    private BufferedImage bufferImagem;
    
    public Imagem( String caminhoArquivo, boolean monocromatica ) {
        super();
        
        setCaminhoArquivo( caminhoArquivo );
        setMonocromatica( monocromatica );
    }
    
    public Imagem( String caminhoArquivo ) {
        this( caminhoArquivo, false );
    }
    
    public void setCaminhoArquivo( String caminhoArquivo ) {
        this.caminhoArquivo = caminhoArquivo;
        
        bufferImagem = null;
        
        if ( caminhoArquivo == null )
            return;
        
        try {
            bufferImagem = ImageIO.read( new File( caminhoArquivo ) );
            setLargura( bufferImagem.getWidth() );
            setAltura( bufferImagem.getHeight() );
            alocar();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }
    
    private byte[] ABGRparaBGR( byte[] imagemABGR ) {
        byte[] imagemBGR = new byte[imagemABGR.length - imagemABGR.length / 4];
        
        for ( int i = 1, j = 0; i < imagemABGR.length; i += 4, j += 3 ) {
            imagemBGR[j] = imagemABGR[i];
            imagemBGR[j + 1] = imagemABGR[i + 1];
            imagemBGR[j + 2] = imagemABGR[i + 2]; 
        }
        
        return imagemBGR;
    }
    
    public void carregar() {
        if ( bufferImagem == null )
            return;
        
        byte[] imagem = ABGRparaBGR(
            ( (DataBufferByte) bufferImagem.getData().getDataBuffer() ).getData()
        );
        
        ByteBuffer bb = ByteBuffer.allocateDirect( imagem.length );
        bb.order( ByteOrder.nativeOrder() );
        bb.put( imagem );
        bb.position( 0 );
        
        carregarImagem( bb );
    }
}