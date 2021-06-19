package es;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
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

public class Bluetooth {
    private LocalDevice localDevice;
    private DiscoveryAgent discoveryAgent;
    private boolean pesquisando;
    private TreeSet<RemoteDevice> remoteDevicesPesquisa;
    private TreeSet<RemoteDevice> remoteDevicesConexao;

    private int tamBufferEntrada;
    private ByteBuffer bufferEntrada;
    public ByteBuffer visBufferEntrada;
    
    public Bluetooth( int tamBufferEntrada ) {
        try {
            localDevice = LocalDevice.getLocalDevice();
            localDevice.setDiscoverable( DiscoveryAgent.GIAC );

            discoveryAgent = localDevice.getDiscoveryAgent();

            remoteDevicesPesquisa = new TreeSet<RemoteDevice>( 
                ( o1, o2 ) -> 
                    o1.getBluetoothAddress().compareTo( o2.getBluetoothAddress() ) 
            );

            remoteDevicesConexao = new TreeSet<RemoteDevice>( remoteDevicesPesquisa );

            this.tamBufferEntrada = tamBufferEntrada;
            bufferEntrada = ByteBuffer.allocateDirect( tamBufferEntrada );
            visBufferEntrada = bufferEntrada.asReadOnlyBuffer();

            pesquisando = false;
        } catch ( IOException e ) { e.printStackTrace(); }
    }

    public Bluetooth() {
        this( 1 );
    }

    private void printDevice( RemoteDevice btDevice ) {
        System.out.print( btDevice.getBluetoothAddress() );
        try {
            System.out.println( ": " + btDevice.getFriendlyName( false ) );
        } catch ( IOException e ) { System.out.println( ": Nome indisponível" ); }
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
        } catch ( IOException e ) { e.printStackTrace(); }
    }

    public void encerrarPesquisa() {
        pesquisando = false;
        discoveryAgent.cancelInquiry( discoveryListener );
        System.out.println( "Pesquisa de dispositivos encerrada." );
    }

    private Thread receberDados( StreamConnection conexao ) {
        Thread thread = new Thread( () ->
            {
                try {
                    DataInputStream input = conexao.openDataInputStream();

                    byte[] b = new byte[tamBufferEntrada];
                    while( true ) {
                        input.readFully( b );
                        bufferEntrada.rewind();
                        bufferEntrada.put( b );
                    }   
                } 
                catch ( EOFException ignored ) {}
                catch ( IOException e ) { e.printStackTrace(); }
            }
        );
        thread.start();

        return thread;
    }
    
/*     private final int numElem = 10;
    private final int numBytes = numElem * Integer.BYTES; */

    /* private Thread enviarDados( StreamConnection conexao ) {
        Thread thread = new Thread( () ->
            {
                try {
                    DataOutputStream output = conexao.openDataOutputStream();

                    ByteBuffer bb = ByteBuffer.allocateDirect( numBytes );
                    byte[] b = new byte[numBytes];
                    for ( int num = 100; true; num += 100 ) {
                        bb.rewind();
                        for ( int i = 0; i < numElem; i++ )
                            bb.putInt( num + ( i * 10 ) );
                        bb.rewind();
                        bb.get( b );
                        output.write( b );
                        Thread.sleep( 1000 );
                    } 
                } catch ( IOException | InterruptedException ignored ) {}
            }
        );
        thread.start();

        return thread;
    } */

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
                StreamConnection conexao = (StreamConnection) Connector.open( url );
                
                if ( conexao != null ) {
                    System.out.println( "Conexão bem-sucedida!" );

                    Thread 
                        tReceber = receberDados( conexao );
                    
                    tReceber.join();
                    conexao.close();
                }

            } catch ( IOException | InterruptedException e ) { e.printStackTrace(); }
        }

        @Override
        public void serviceSearchCompleted( int transID, int respCode ) {}

        @Override
        public void inquiryCompleted( int discType ) {
            try {
                for ( RemoteDevice btDevice: remoteDevicesConexao )
                    if ( btDevice.getBluetoothAddress().equals( "304B0745112F" ) ) {
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
            } catch ( IOException e ) { e.printStackTrace(); }
        }
        
    };

    public void conectarSmartphone() {
        if ( pesquisando )
            encerrarPesquisa();

        remoteDevicesConexao.clear();    
        System.out.println( "Procurando smartphone..." );
        try {
            discoveryAgent.startInquiry( DiscoveryAgent.GIAC, connectionListener );
        } catch ( IOException e ) { e.printStackTrace(); }
    }
}