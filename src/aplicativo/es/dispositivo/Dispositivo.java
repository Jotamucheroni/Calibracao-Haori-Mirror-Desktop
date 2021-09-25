package aplicativo.es.dispositivo;

import aplicativo.es.camera.Camera;
import aplicativo.opengl.DetectorPontos;
import aplicativo.opengl.Programa;
import aplicativo.opengl.Desenho;
import aplicativo.opengl.Textura;
import aplicativo.opengl.framebuffer.FrameBufferObject;

public class Dispositivo implements AutoCloseable {
    private static int instancia = 0;
    
    private final String id;
    private final Camera camera;
    private final Textura textura;
    private final FrameBufferObject frameBufferObject;
    private final Desenho desenho;
    private final DetectorPontos detectorPontos;
    
    public Dispositivo( String id, Camera camera ) {
        if ( id == null )
            id = "Dispositivo " + instancia;
        this.id = id;
        instancia++;
        
        this.camera = camera;
        
        if ( camera != null )
            textura = new Textura(
                camera.getLarguraImagem(), camera.getAlturaImagem(),
                camera.getNumeroComponentesCorImagem()
            );
        else
            textura = null;
        
        frameBufferObject = new FrameBufferObject( Programa.MAXIMO_SAIDAS, 640, 480 );
        
        if ( textura != null )
            desenho = new Desenho( 2, 2, Desenho.getRefQuad(), Desenho.getRefElementos(), textura );
        else
            desenho = null;
        
        detectorPontos = new DetectorPontos(
            frameBufferObject.getLargura(),
            frameBufferObject.getAltura(),
            textura == null ? 4 : textura.getNumeroComponentesCor()
        );
    }
    
    public Dispositivo(
        Camera camera
    ) {
        this( null, camera );
    }
    
    public String getId() {
        return id;
    }
    
    public Camera getCamera() {
        return camera;
    }
    
    public Textura getTextura() {
        return textura;
    }
    
    public FrameBufferObject getFrameBufferObject() {
        return frameBufferObject;
    }
    
    public Desenho getDesenho() {
        return desenho;
    }
    
    public DetectorPontos getDetectorPontos() {
        return detectorPontos;
    }
    
    public void ligar() {
        if ( camera == null )
            return;
        
        camera.ligar();
    }
    
    public void desligar() {
        if ( camera == null )
            return;
        
        camera.desligar();
    }
    
    public boolean getLigado() {
        if( camera == null )
            return false;
        
        return camera.getLigada();
    }
    
    public void atualizarTextura() {
        if ( textura == null || camera == null )
            return;
        
        textura.carregarImagem( camera.getImagem() );
    }
    
    public void draw() {
        if ( frameBufferObject == null )
            return;
        
        frameBufferObject.clear();
        
        if ( desenho == null )
            return;
        
        frameBufferObject.draw( desenho );
    }
    
    public void atualizarImagemDetector( int numeroRenderBuffer ) {
        if ( detectorPontos == null )
            return;
        
        if ( detectorPontos.ocupado() || frameBufferObject == null )
            return;
        
        frameBufferObject.lerRenderBuffer(
            numeroRenderBuffer,
            detectorPontos.getNumeroComponentesCorImagem(), detectorPontos.getImagem()
        );
        detectorPontos.executar();
    }
    
    public void atualizarImagemDetector() {
        atualizarImagemDetector( 1 );
    }
    
    @Override
    public void close() {
        if ( detectorPontos != null )
            detectorPontos.close();
        
        if ( desenho != null )
            desenho.close();
        
        if ( frameBufferObject != null )
            frameBufferObject.close();
        
        if ( textura != null )
            textura.close();
        
        if ( camera != null )
            camera.close();
    }
}