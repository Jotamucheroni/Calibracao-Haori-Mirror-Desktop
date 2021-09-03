package aplicativo.es.dispositivo;

import java.io.DataInputStream;

import javax.microedition.io.StreamConnection;

import aplicativo.es.Bluetooth;
import aplicativo.es.camera.CameraRemota;
import aplicativo.opengl.DetectorPontos;
import aplicativo.opengl.Objeto;
import aplicativo.opengl.Textura;
import aplicativo.opengl.framebuffer.FrameBufferObject;

public class DispositivoRemoto extends Dispositivo {
    public DispositivoRemoto(
        String id,
        CameraRemota cameraRemota,
        Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorPontos detectorBorda
    ) {
        super( id, cameraRemota, textura, frameBufferObject, objeto, detectorBorda );
    }
    
    public DispositivoRemoto(
        CameraRemota cameraRemota,
        Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorPontos detectorBorda
    ) {
        super( null, cameraRemota, textura, frameBufferObject, objeto, detectorBorda );
    }
    
    public DispositivoRemoto(
        String id,
        CameraRemota cameraRemota,
        Textura textura, FrameBufferObject frameBufferObject, Objeto objeto
    ) {
        super( id, cameraRemota, textura, frameBufferObject, objeto );
    }
    
    public DispositivoRemoto(
        CameraRemota cameraRemota,
        Textura textura, FrameBufferObject frameBufferObject, Objeto objeto
    ) {
        super( null, cameraRemota, textura, frameBufferObject, objeto );
    }
    
    public DispositivoRemoto(
        String id,
        CameraRemota cameraRemota,
        Textura textura, FrameBufferObject frameBufferObject
    ) {
        super( id, cameraRemota, textura, frameBufferObject );
    }
    
    public DispositivoRemoto(
        CameraRemota cameraRemota, Textura textura, FrameBufferObject frameBufferObject
    ) {
        super( null, cameraRemota, textura, frameBufferObject );
    }
    
    public DispositivoRemoto(
        String id,
        CameraRemota cameraRemota, Textura textura
    ) {
        super( id, cameraRemota, textura );
    }
    
    public DispositivoRemoto(
        CameraRemota cameraRemota, Textura textura
    ) {
        super( null, cameraRemota, textura );
    }
    
    public DispositivoRemoto(
        String id,
        CameraRemota cameraRemota
    ) {
        super( id, cameraRemota );
    }
    
    public DispositivoRemoto(
        CameraRemota cameraRemota
    ) {
        super( null, cameraRemota );
    }
    
    public CameraRemota getCameraRemota() {
        return (CameraRemota) getCamera();
    }
    
    public void setEntradaRemota( DataInputStream entradaRemota ) {
        getCameraRemota().setEntradaRemota( entradaRemota );
    }
    
    public void setEntradaRemota( StreamConnection entradaRemota ) {
        getCameraRemota().setEntradaRemota( entradaRemota );
    }
    
    public void esperarEntradaRemota( Bluetooth bluetooth ) {
        getCameraRemota().esperarEntradaRemota( bluetooth );
    }
}
