package aplicativo.opengl;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Imagem extends Textura {
    public Imagem( BufferedImage bufferedImage ) {
        super(
            bufferedImage.getWidth(), bufferedImage.getHeight(),
            bufferedImage.getColorModel().getNumComponents()
        );
        
        carregar( bufferedImage );
    }
    
    public Imagem( String caminhoArquivo ) {
        this( getBufferedImage( caminhoArquivo ) );
    }
    
    private static BufferedImage getBufferedImage( String caminhoArquivo ) {
        try {
            return ImageIO.read( new File( caminhoArquivo ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        
        return null;
    }
        
    public static byte[] inverterComponentes( byte[] imagemOriginal, int numeroComponentesCor ) {
        byte[] imagemComponentesInvertidos = new byte[imagemOriginal.length];
        
        for ( int i = 0; i < imagemOriginal.length; i += numeroComponentesCor )
            for ( int j = 0; j < numeroComponentesCor; j++ )
                imagemComponentesInvertidos[i + j] =
                    imagemOriginal[i + numeroComponentesCor - j - 1];
        
        return imagemComponentesInvertidos;
    }
    
    public static byte[] converterARGBparaRGBA( byte[] imagemARGB ) {
        byte[] imagemRGBA = new byte[imagemARGB.length];
        
        for ( int i = 0; i < imagemARGB.length; i += 4 ) {
            imagemRGBA[i] = imagemARGB[i + 1];
            imagemRGBA[i + 1] = imagemARGB[i + 2];
            imagemRGBA[i + 2] = imagemARGB[i + 3];
            imagemRGBA[i + 3] = imagemARGB[i];
        }
        
        return imagemRGBA;
    }
    
    public void carregar( BufferedImage bufferedImage ) {
        if ( bufferedImage == null )
            return;
        
        byte[] imagem = ( (DataBufferByte) bufferedImage.getData().getDataBuffer() ).getData();
        
        if (
            bufferedImage.getWidth() != getLargura() ||
            bufferedImage.getHeight() != getAltura() ||
            bufferedImage.getColorModel().getNumComponents() != getNumeroComponentesCor()
        )
            return;
        
        int tipo = bufferedImage.getType();
        if (
            tipo == BufferedImage.TYPE_3BYTE_BGR || tipo == BufferedImage.TYPE_INT_BGR ||
            tipo == BufferedImage.TYPE_4BYTE_ABGR || tipo == BufferedImage.TYPE_4BYTE_ABGR_PRE
        )
            imagem = inverterComponentes( imagem, getNumeroComponentesCor() );
        else if ( tipo == BufferedImage.TYPE_INT_ARGB || tipo == BufferedImage.TYPE_INT_ARGB_PRE)
            imagem = converterARGBparaRGBA( imagem );
        
        ByteBuffer bb = ByteBuffer.allocateDirect( imagem.length );
        bb.order( ByteOrder.nativeOrder() );
        bb.put( imagem );
        bb.position( 0 );
        
        carregarImagem( bb );
    }
    
    public void carregar( String caminhoArquivo ) {
        carregar( getBufferedImage( caminhoArquivo ) );
    }
}