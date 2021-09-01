package es.dispositivo;

import es.camera.Camera;

import opengl.Textura;
import opengl.framebuffer.FrameBufferObject;
import opengl.Objeto;
import opengl.DetectorBorda;

public class Dispositivo implements AutoCloseable {
    private String id;
    
    private Camera camera;
    private Textura textura;
    private FrameBufferObject frameBufferObject;
    private Objeto objeto;
    
    private DetectorBorda detectorBorda;
    
    public Dispositivo(
        String id,
        Camera camera, Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorBorda detectorBorda
    ) {
        setId( id );
        setCamera( camera );
        setTextura( textura );
        setFrameBufferObject( frameBufferObject );
        setObjeto( objeto );
        setDetectorBorda( detectorBorda );
    }
    
    public Dispositivo(
        Camera camera, Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorBorda detectorBorda
    ) {
        this( null, camera, textura, frameBufferObject, objeto, detectorBorda );
    }
    
    public Dispositivo(
        String id,
        Camera camera, Textura textura, FrameBufferObject frameBufferObject, Objeto objeto
    ) {
        this( id, camera, textura, frameBufferObject, objeto, null );
        
        setDetectorBorda();
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
        setDetectorBorda();
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
        setDetectorBorda();
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
        setDetectorBorda();
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
    
    public void setDetectorBorda( DetectorBorda detector ) {
        this.detectorBorda = detector;
    }
    
    private void setDetectorBorda() {
        if ( frameBufferObject == null )
            return;
        
        DetectorBorda inicializadorDetectorBorda = new DetectorBorda(
            frameBufferObject.getNumPix(), 1
        );
        
        if ( textura == null ) {
            setDetectorBorda( inicializadorDetectorBorda );
            
            return;
        }
        
        if ( !textura.getMonocromatica() ) {
            inicializadorDetectorBorda.setTamanhoImagem( frameBufferObject.getNumBytes() );
            inicializadorDetectorBorda.setNumeroComponentesCor(
                FrameBufferObject.numeroComponentesCor
            );
        }
        
        setDetectorBorda( inicializadorDetectorBorda );
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
    
    public DetectorBorda getDetectorBorda() {
        return detectorBorda;
    }
    
    public boolean getLigado() {
        if( camera == null )
            return false;
            
        return camera.ligada();
    }
    
    public void ligar() {
        if ( camera == null )
            return;
            
        camera.ligar();
    }
    
    public void alocar() {
        if ( textura != null )
            textura.alocar();
        
        if ( frameBufferObject != null )
            frameBufferObject.alocar();
        
        if ( detectorBorda != null )
            detectorBorda.alocar();
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
        if ( detectorBorda == null || frameBufferObject == null )
            return;
        
        if ( detectorBorda.pronto() ) {
            System.out.println( "Saída [" + id + "]:\t" + detectorBorda.getSaida() );
            frameBufferObject.lerRenderBuffer(
                numeroRenderBuffer,
                detectorBorda.getNumeroComponentesCor(), detectorBorda.getImagem()
            );
            detectorBorda.executar();
        }
    }
    
    public void atualizarImagemDetector() {
        atualizarImagemDetector( 1 );
    }
    
    @Override
    public void close() {
        detectorBorda.close();
        frameBufferObject.close();
        textura.close();
        camera.close();
    }
}