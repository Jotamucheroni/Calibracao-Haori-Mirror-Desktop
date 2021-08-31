package es.dispositivo;

import java.io.DataInputStream;

import javax.microedition.io.StreamConnection;

import es.camera.CameraRemota;
import opengl.Textura;
import opengl.framebuffer.FrameBufferObject;
import opengl.Objeto;
import opengl.DetectorBorda;

public class DispositivoRemoto extends Dispositivo {
    public DispositivoRemoto(
        String id,
        CameraRemota cameraRemota,
        Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorBorda detectorBorda
    ) {
        super( id, cameraRemota, textura, frameBufferObject, objeto, detectorBorda );
    }
    
    public DispositivoRemoto(
        CameraRemota cameraRemota,
        Textura textura, FrameBufferObject frameBufferObject, Objeto objeto,
        DetectorBorda detectorBorda
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
}
