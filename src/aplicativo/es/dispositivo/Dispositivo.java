package aplicativo.es.dispositivo;

import aplicativo.es.camera.Camera;
import aplicativo.opengl.DetectorPontos;
import aplicativo.opengl.Objeto;
import aplicativo.opengl.Textura;
import aplicativo.opengl.framebuffer.FrameBufferObject;

public class Dispositivo implements AutoCloseable {
    private String id;
    
    private Camera camera;
    private Textura textura;
    private FrameBufferObject frameBufferObject;
    private Objeto objeto;
    
    private DetectorPontos detectorPontos;
    
    public Dispositivo(
        String id,
        Camera camera, Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorPontos detectorPontos
    ) {
        setId( id );
        setCamera( camera );
        setTextura( textura );
        setFrameBufferObject( frameBufferObject );
        setObjeto( objeto );
        setDetectorPontos( detectorPontos );
    }
    
    public Dispositivo(
        Camera camera, Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorPontos detectorPontos
    ) {
        this( null, camera, textura, frameBufferObject, objeto, detectorPontos );
    }
    
    public Dispositivo(
        String id,
        Camera camera, Textura textura, FrameBufferObject frameBufferObject, Objeto objeto
    ) {
        this( id, camera, textura, frameBufferObject, objeto, null );
        
        setDetectorPontos();
    }
    
    public Dispositivo(
        Camera camera, Textura textura, FrameBufferObject frameBufferObject, Objeto objeto
    ) {
        this( null, camera, textura, frameBufferObject, objeto );
    }
    
    public Dispositivo(
        String id,
        Camera camera, Textura textura, FrameBufferObject frameBufferObject
    ) {
        this( id, camera, textura, frameBufferObject, null, null );
        
        setObjeto();
        setDetectorPontos();
    }
    
    public Dispositivo(
        Camera camera, Textura textura, FrameBufferObject frameBufferObject
    ) {
        this( null, camera, textura, frameBufferObject );
    }
    
    public Dispositivo(
        String id,
        Camera camera, Textura textura
    ) {
        this( id, camera, textura, null, null, null );
        
        setFrameBufferObject();
        setObjeto();
        setDetectorPontos();
    }
    
    public Dispositivo(
        Camera camera, Textura textura
    ) {
        this( null, camera, textura );
    }
    
    public Dispositivo(
        String id,
        Camera camera
    ) {
        this( id, camera, null, null, null, null );
        
        setTextura();
        setFrameBufferObject();
        setObjeto();
        setDetectorPontos();
    }
    
    public Dispositivo(
        Camera camera
    ) {
        this( null, camera );
    }
    
    public void setId( String id ) {
        if ( id == null ) {
            this.id = "Dispositivo";
            
            return;
        }
        
        this.id = id;
    }
    
    public void setCamera( Camera camera ) {
        this.camera = camera;
    }
    
    public void setTextura( Textura textura ) {
        this.textura = textura;
    }
    
    private void setTextura() {
        if ( camera == null )
            return;
            
        Textura inicializadorTextura = new Textura( camera.getLargImg(), camera.getAltImg(), true );
        
        if ( camera.getNumCompCor() > 1 )
            inicializadorTextura.setMonocromatica( false );
        
        setTextura( inicializadorTextura );
    }
    
    public void setFrameBufferObject( FrameBufferObject frameBufferObject ) {
        this.frameBufferObject = frameBufferObject;
    }
    
    private void setFrameBufferObject() {
        setFrameBufferObject( new FrameBufferObject( 3, 640, 480 ) );
    }
    
    public void setObjeto( Objeto objeto ) {
        this.objeto = objeto;
    }
    
    private void setObjeto() {
        if ( textura == null )
            return;
        
        setObjeto(
            new Objeto(
                2, 2, Objeto.getRefQuad(), Objeto.getRefElementos(), textura
            ) 
        );
    }
    
    public void setDetectorPontos( DetectorPontos detectorPontos ) {
        this.detectorPontos = detectorPontos;
    }
    
    private void setDetectorPontos() {
        if ( frameBufferObject == null )
            return;
        
        DetectorPontos inicializadorDetectorPontos = new DetectorPontos(
            frameBufferObject.getNumPix(), 1
        );
        
        if ( textura == null ) {
            setDetectorPontos( inicializadorDetectorPontos );
            
            return;
        }
        
        if ( !textura.getMonocromatica() ) {
            inicializadorDetectorPontos.setTamanhoImagem( frameBufferObject.getNumBytes() );
            inicializadorDetectorPontos.setNumeroComponentesCor(
                FrameBufferObject.numeroComponentesCor
            );
        }
        
        setDetectorPontos( inicializadorDetectorPontos );
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
    
    public Objeto getObjeto() {
        return objeto;
    }
    
    public DetectorPontos getDetectorPontos() {
        return detectorPontos;
    }
    
    public boolean getLigado() {
        if( camera == null )
            return false;
        
        return camera.getLigada();
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
    
    public void alocar() {
        if ( textura != null )
            textura.alocar();
        
        if ( frameBufferObject != null )
            frameBufferObject.alocar();
        
        if ( detectorPontos != null )
            detectorPontos.alocar();
    }
    
    public void atualizarTextura() {
        if ( textura == null || camera == null )
            return;
        
        textura.carregarImagem( camera.getImagem() );
    }
    
    public void draw() {
        if ( frameBufferObject == null || objeto == null )
            return;
        
        frameBufferObject.clear();
        frameBufferObject.draw( objeto );
    }
    
    public void atualizarImagemDetector( int numeroRenderBuffer ) {
        if ( detectorPontos == null || frameBufferObject == null )
            return;
        
        if ( detectorPontos.pronto() ) {
            // System.out.println( "Saída [" + id + "]:\t" + detectorPontos.getSaida() );
            frameBufferObject.lerRenderBuffer(
                numeroRenderBuffer,
                detectorPontos.getNumeroComponentesCor(), detectorPontos.getImagem()
            );
            detectorPontos.executar();
        }
    }
    
    public void atualizarImagemDetector() {
        atualizarImagemDetector( 1 );
    }
    
    @Override
    public void close() {
        if ( detectorPontos != null )
            detectorPontos.close();
        
        if ( frameBufferObject != null )
            frameBufferObject.close();
        
        if ( textura != null )
            textura.close();
        
        if ( camera != null )
            camera.close();
    }
}