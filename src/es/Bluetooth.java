package es;

import java.io.IOException;
import java.util.TreeSet;

import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class Bluetooth implements AutoCloseable {
    private LocalDevice localDevice;
    private DiscoveryAgent discoveryAgent;
    private boolean pesquisando;
    private boolean conectado;
    private TreeSet<RemoteDevice> remoteDevicesPesquisa;
    private TreeSet<RemoteDevice> remoteDevicesConexao;
    
    public Bluetooth() {
        try {
            localDevice = LocalDevice.getLocalDevice();
            localDevice.setDiscoverable( DiscoveryAgent.GIAC );
            
            discoveryAgent = localDevice.getDiscoveryAgent();
            
            remoteDevicesPesquisa = new TreeSet<RemoteDevice>( 
                ( o1, o2 ) -> o1.getBluetoothAddress().compareTo( o2.getBluetoothAddress() ) 
            );
            
            remoteDevicesConexao = new TreeSet<RemoteDevice>( remoteDevicesPesquisa );
            
            pesquisando = false;
            conectado = false;
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    private void printDevice( RemoteDevice btDevice ) {
        System.out.print( btDevice.getBluetoothAddress() );
        try {
            System.out.println( ": " + btDevice.getFriendlyName( false ) );
        } catch ( IOException e ) {
            System.out.println( ": Nome indisponível" );
        }
    }
    
    DiscoveryListener discoveryListener = new DiscoveryListener() {
        @Override
        public void deviceDiscovered( RemoteDevice btDevice, DeviceClass cod ) {
            if ( remoteDevicesPesquisa.add( btDevice ) )
                printDevice( btDevice );
        }
        
        @Override
        public void servicesDiscovered( int transID, ServiceRecord[] servRecord ) {}
        @Override
        public void serviceSearchCompleted( int transID, int respCode ) {}
        
        @Override
        public void inquiryCompleted( int discType ) {
            if ( pesquisando )
                try {
                    discoveryAgent.startInquiry( DiscoveryAgent.GIAC, discoveryListener );
                } catch ( IOException e ) { e.printStackTrace(); }
        }
    };
    
    public void pesquisarDispositivos() {
        remoteDevicesPesquisa.clear();
        System.out.println( "Pesquisa de dispositivos iniciada:" );
        pesquisando = true;
        try {
            discoveryAgent.startInquiry( DiscoveryAgent.GIAC, discoveryListener );
        } catch ( IOException e ) { 
            e.printStackTrace();
        }
    }
    
    public void encerrarPesquisa() {
        if ( !pesquisando )
            return;
        
        pesquisando = false;
        discoveryAgent.cancelInquiry( discoveryListener );
        System.out.println( "Pesquisa de dispositivos encerrada." );
    }
    
    private String enderecoDispositivo;
    private StreamConnection conexao;
    
    DiscoveryListener connectionListener = new DiscoveryListener() {
        @Override
        public void deviceDiscovered( RemoteDevice btDevice, DeviceClass cod ) {
            remoteDevicesConexao.add( btDevice );
        }
        
        @Override
        public void servicesDiscovered( int transID, ServiceRecord[] servRecord ) {
            String mensagem;
            if ( servRecord.length > 1 )
                mensagem = " serviços encontrados!";
            else
                mensagem = " serviço encontrado!";
            System.out.println( servRecord.length + mensagem );

            String url = servRecord[0].getConnectionURL(
                ServiceRecord.NOAUTHENTICATE_NOENCRYPT,
                false
            );
            Object serviceName = servRecord[0].getAttributeValue( 0x0100 ).getValue();
            
            System.out.println(
                "Serviço \"" 
              + ( ( serviceName == null ) ? "Nome indisponível" : serviceName )
              + "\" encontrado. URL: "
              + ( ( url == null ) ? "URL indisponível" : "<" + url + ">" )
            );
            
            System.out.println( "Conectando a " + url );
            try {
                conexao = (StreamConnection) Connector.open( url );
                
                if ( conexao != null ) {
                    System.out.println( "Conexão bem-sucedida!" );
                    conectado = true;
                    synchronized( this ) {
                        notify();
                    }
                }
            } catch ( IOException ignored ) {}
        }
        
        @Override
        public void serviceSearchCompleted( int transID, int respCode ) {}
        
        @Override
        public void inquiryCompleted( int discType ) {
            try {
                for ( RemoteDevice btDevice : remoteDevicesConexao )
                    if ( btDevice.getBluetoothAddress().equals( enderecoDispositivo ) ) {
                        System.out.println( "Encontrado!" );
                        printDevice( btDevice );
                        System.out.println( "Procurando serviço..." );
                        discoveryAgent.searchServices(
                            new int[]{ 0x0100 },
                            new UUID[]{ new UUID( "7427f3add28e4267a5b5f358165eac26", false ) },
                            btDevice,
                            connectionListener
                        );
                        return;
                    }
                discoveryAgent.startInquiry( DiscoveryAgent.GIAC, connectionListener );        
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    };
    
    public StreamConnection conectarDispositivo( String enderecoDispositivo ) {
        if ( conectado || enderecoDispositivo == null )
            return null;
        
        if ( pesquisando )
            encerrarPesquisa();
        
        remoteDevicesConexao.clear();
        this.enderecoDispositivo = enderecoDispositivo;
        System.out.println( "Procurando smartphone..." );
        try {
            discoveryAgent.startInquiry( DiscoveryAgent.GIAC, connectionListener );
            synchronized( connectionListener ) {
                connectionListener.wait();
            }
            
            return conexao;
        } catch ( IOException | InterruptedException ignored ) {}
        
        return null;
    }
    
    public void desconectarDispositivo() {
        if ( !conectado )
            return;
        
        try {
            conexao.close();
        } catch ( IOException ignored ) {}
        conectado = false;
    }
    
    public StreamConnection reconectarDispositivo( String enderecoDispositivo ) {
        System.out.println( "Reconectando..." );
        
        desconectarDispositivo();
        return conectarDispositivo( enderecoDispositivo );
    }
    
    public StreamConnection reconectarDispositivo() {
        return reconectarDispositivo( enderecoDispositivo );
    }

    @Override
    public void close() {
        encerrarPesquisa();
        desconectarDispositivo();
    }
}