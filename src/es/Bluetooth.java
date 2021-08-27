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
    private boolean pesquisando, conectado, ligado;
    private Object travaPesquisa, travaConexao, travaLigado;
    private TreeSet<RemoteDevice> remoteDevicesPesquisa, remoteDevicesConexao;
    
    public Bluetooth() {
        try {
            localDevice = LocalDevice.getLocalDevice();
            localDevice.setDiscoverable( DiscoveryAgent.GIAC );
            
            discoveryAgent = localDevice.getDiscoveryAgent();
            
            remoteDevicesPesquisa = new TreeSet<RemoteDevice>( 
                ( o1, o2 ) -> o1.getBluetoothAddress().compareTo( o2.getBluetoothAddress() ) 
            );
            
            remoteDevicesConexao = new TreeSet<RemoteDevice>( remoteDevicesPesquisa );
            
            pesquisando = false;    travaPesquisa = new Object();
            conectado = false;      travaConexao = new Object();
            ligado = true;          travaLigado = new Object();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }
    
    public static void printDevice( RemoteDevice btDevice ) {            
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
            synchronized( travaLigado ) {
                if ( !ligado )
                    return;
            }
            
            synchronized( travaPesquisa ) {
                if ( !pesquisando )
                    return;
                
                if ( remoteDevicesPesquisa.add( btDevice ) )
                    printDevice( btDevice );
            }
        }
        
        @Override
        public void servicesDiscovered( int transID, ServiceRecord[] servRecord ) {}
        @Override
        public void serviceSearchCompleted( int transID, int respCode ) {}
        
        @Override
        public void inquiryCompleted( int discType ) {
            synchronized( travaLigado ) {
                if ( !ligado )
                    return;
            }
            
            synchronized ( travaPesquisa ) {
                if ( !pesquisando )
                    return;
                
                try {
                    discoveryAgent.startInquiry( DiscoveryAgent.GIAC, discoveryListener );
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
    };
    
    public void pesquisarDispositivos() {
        synchronized( travaLigado ) {
            if ( !ligado )
                return;
        }
        
        synchronized ( travaPesquisa ) {
            if ( pesquisando )
                return;
            
            System.out.println( "Pesquisa de dispositivos iniciada:" );
            
            remoteDevicesPesquisa.clear();
            pesquisando = true;
            
            try {
                discoveryAgent.startInquiry( DiscoveryAgent.GIAC, discoveryListener );
            } catch ( IOException e ) { 
                e.printStackTrace();
            }
        }
    }
    
    public void encerrarPesquisa() {
        synchronized ( travaPesquisa ) {
            if ( !pesquisando )
                return;
            
            pesquisando = false;
            discoveryAgent.cancelInquiry( discoveryListener );
        
            System.out.println( "Pesquisa de dispositivos encerrada." );
        }
    }
    
    private String enderecoDispositivo;
    private StreamConnection conexao;
    
    DiscoveryListener connectionListener = new DiscoveryListener() {
        @Override
        public void deviceDiscovered( RemoteDevice btDevice, DeviceClass cod ) {
            synchronized( travaLigado ) {
                if ( !ligado )
                    return;
            }
            
            synchronized ( travaConexao ) {
                remoteDevicesConexao.add( btDevice );
            }
        }
        
        @Override
        public void inquiryCompleted( int discType ) {
            synchronized( travaLigado ) {
                if ( !ligado )
                    return;
            }
            
            synchronized ( travaConexao ) {
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
        }
        
        @Override
        public void servicesDiscovered( int transID, ServiceRecord[] servRecord ) {
            synchronized( travaLigado ) {
                if ( !ligado )
                    return;
            }
            
            synchronized( travaConexao ) {
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
                    +   ( ( serviceName == null ) ? "Nome indisponível" : serviceName )
                    +   "\" encontrado. URL: "
                    +   ( ( url == null ) ? "URL indisponível" : "<" + url + ">" )
                );
                
                System.out.println( "Conectando a " + url );
                try {
                    conexao = (StreamConnection) Connector.open( url );
                    
                    if ( conexao != null ) {
                        System.out.println( "Conexão bem-sucedida!" );
                        conectado = true;
                    }
                } catch ( IOException ignored ) {}
            }
        }
        
        @Override
        public void serviceSearchCompleted( int transID, int respCode ) {}
    };
    
    public void conectarDispositivo( String enderecoDispositivo ) {
        synchronized( travaLigado ) {
            if ( !ligado )
                return;
        }
        
        synchronized ( travaConexao ) {
            if ( conectado || enderecoDispositivo == null )
                return;
                
            encerrarPesquisa();
            remoteDevicesConexao.clear();
            this.enderecoDispositivo = enderecoDispositivo;
            System.out.println( "Procurando smartphone..." );
            try {
                discoveryAgent.startInquiry( DiscoveryAgent.GIAC, connectionListener );
            } catch ( IOException ignored ) {}
        }
    }
    
    public void desconectarDispositivo() {
        synchronized ( travaConexao ) {
            if ( !conectado )
                return;
            
            conectado = false;
            try {
                conexao.close();
            } catch ( IOException ignored ) {}
        }
    }
    
    public void reconectarDispositivo( String enderecoDispositivo ) {
        synchronized ( travaConexao ) {
            System.out.println( "Reconectando..." );
            
            desconectarDispositivo();
            conectarDispositivo( enderecoDispositivo );
        }
    }
    
    public void reconectarDispositivo() {
        reconectarDispositivo( enderecoDispositivo );
    }
    
    public boolean getConectado() {
        synchronized ( travaConexao ) {
            return conectado;
        }
    }
    
    public StreamConnection getConexao() {
        synchronized ( travaConexao ) {
            return conexao;
        }
    }
    
    @Override
    public void close() {
        synchronized ( travaLigado ) {
            if ( !ligado )
                return;
            
            ligado = false;
        }
        encerrarPesquisa();
        desconectarDispositivo();
    }
}