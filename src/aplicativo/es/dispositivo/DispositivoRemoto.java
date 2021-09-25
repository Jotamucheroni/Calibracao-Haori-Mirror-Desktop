package aplicativo.es.dispositivo;

import java.io.DataInputStream;

import javax.microedition.io.StreamConnection;

import aplicativo.es.Bluetooth;
import aplicativo.es.camera.CameraRemota;

public class DispositivoRemoto extends Dispositivo {
    public DispositivoRemoto( String id, CameraRemota cameraRemota ) {
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
