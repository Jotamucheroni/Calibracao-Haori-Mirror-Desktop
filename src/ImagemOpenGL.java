import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ImagemOpenGL extends TexturaOpenGL {
    private String caminhoArquivo;
    private BufferedImage bufferImagem;
    
    public ImagemOpenGL( String caminhoArquivo, boolean monocromatica ) {
        super();
        
        setCaminhoArquivo( caminhoArquivo );
        setMonocromatica( monocromatica );
    }
    
    ImagemOpenGL( String caminhoArquivo ) {
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
    
    private byte[] toBGR( byte[] imagemABGR ) {
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
        
        byte[] imagem = toBGR(
            ( (DataBufferByte) bufferImagem.getData().getDataBuffer() ).getData()
        );
        
        ByteBuffer bb = ByteBuffer.allocateDirect( imagem.length );
        bb.order( ByteOrder.nativeOrder() );
        bb.put( imagem );
        bb.position( 0 );
        
        carregarImagem( bb );
        /* gl4.glBindTexture( GL4.GL_TEXTURE_2D, textura );
        gl4.glTexImage2D( 
            GL4.GL_TEXTURE_2D, 0, GL4.GL_RGBA8,
            bufferImagem.getWidth(), bufferImagem.getHeight(), 0,
            GL4.GL_BGR, GL4.GL_UNSIGNED_BYTE, bb
        ); */
    }
}