package aplicativo;

import java.io.PrintStream;
import java.util.Scanner;

public class Parametros {
    private static int instancia = 0;
    
    private String id;
    private String[] nome;
    private float[] valor;
    
    public Parametros( String id, String[] nome, float[] valor ) {
        if ( id == null )
            id = "Grupo " + instancia;
        
        instancia++;
        this.id = id;
        
        if ( valor == null )
            valor = new float[1];
        
        this.valor = valor.clone();
        
        if ( nome == null )
            nome = new String[valor.length];
        
        this.nome = new String[valor.length];
        
        for( int i = 0; i < nome.length && i < this.nome.length; i++ )
            if ( nome[i] == null )
                this.nome[i] = "Parâmetro " + i;
            else
                this.nome[i] = nome[i];
        
        for( int i = nome.length; i < this.nome.length; i++ )
            this.nome[i] = "Parâmetro " + i;
    }
    
    public Parametros( String[] nome, float[] valor ) {
        this( null, nome, valor );
    }
    
    public Parametros( String id, float[] valor ) {
        this( id, null, valor );
    }
    
    public Parametros( float[] valor ) {
        this( null, null, valor );
    }
    
    public Parametros( String id, String[] nome, int numeroParametros ) {
        this( id, nome, ( numeroParametros > 0 ) ? new float[numeroParametros] : null );
    }
    
    public Parametros( String[] nome, int numeroParametros ) {
        this( null, nome, ( numeroParametros > 0 ) ? new float[numeroParametros] : null );
    }
    
    public Parametros( String id, int numeroParametros ) {
        this( id, null, ( numeroParametros > 0 ) ? new float[numeroParametros] : null );
    }
    
    public Parametros( int numeroParametros ) {
        this( null, null, ( numeroParametros > 0 ) ? new float[numeroParametros] : null );
    }
    
    public Parametros( String id, String nome, float valor ) {
        this( id, new String[]{ nome }, new float[]{ valor } );
    }
    
    public Parametros( String id, float valor ) {
        this( id, null, new float[]{ valor } );
    }
    
    public Parametros( float valor ) {
        this( null, null, new float[]{ valor } );
    }
    
    public Parametros( String id ) {
        this( id, null, null );
    }
    
    public Parametros() {
        this( null, null, null );
    }
    
    private int getIndiceValido( int indiceOriginal ) {
        if ( indiceOriginal < 0 )
            return 0;
        
        if ( indiceOriginal >= valor.length )
            return valor.length - 1;
        
        return indiceOriginal;
    }
    
    private boolean atualizado = false;
    
    protected void setValor( int indice, float valor ) {
        indice = getIndiceValido( indice );
        
        synchronized( this ) {   
            this.valor[indice] = valor;
            atualizado = true;
        }
    }
    
    public boolean getAtualizado() {
        synchronized( this ) {
            boolean atualizado = this.atualizado;
            this.atualizado = false;
            
            return atualizado;
        }
    }
    
    public String getId() {
        return id;
    }
    
    public String getNome( int indice ) {
        indice = getIndiceValido( indice );
        
        return nome[indice];
    }
    
    public float getValor( int indice ) {
        indice = getIndiceValido( indice );
        
        synchronized( this ) {
            return valor[indice];
        }
    }
    
    public int getNumeroParametros() {
        return valor.length;
    }
    
    public void lerValor( Scanner entrada, PrintStream saida, int indice ) {
        if ( indice < 0 )
            indice = 0;
        else if ( indice >= getNumeroParametros() )
            indice = getNumeroParametros() - 1;
        
        Thread linhaAtual = Thread.currentThread();  
        float valor;
        String prefixo = 
            "\u001B[96m[" + getId() + "]\u001B[95m[" + getNome( indice ) + "]\u001B[0m ";
        
        do {
            saida.print( prefixo + "Valor: " );
            
            valor = entrada.nextFloat();
            
            if ( valor < 0.0f )
                break;
            
            setValor( indice, valor );
        } while ( !linhaAtual.isInterrupted() );
    }
    
    public void lerValor( Scanner entrada, int indice ) {
        lerValor( entrada, System.out, indice );
    }
    
    public void lerValor( PrintStream saida, int indice ) {
        lerValor( new Scanner( System.in ), saida, indice );
    }
    
    public void lerValor( int indice ) {
        lerValor( new Scanner( System.in ), System.out, indice );
    }
    
    public void ler( Scanner entrada, PrintStream saida ) {        
        Thread linhaAtual = Thread.currentThread();
        
        if ( valor.length == 1 ) {
            lerValor( entrada, saida, 0 );
            
            return;
        }
        
        int indice;
        String prefixo = "\u001B[96m[" + getId() + "]\u001B[0m ";
        
        do {
            saida.println( prefixo + "Parâmetros:" );
            
            for ( int i = 0; i < valor.length; i++ )
                saida.println( prefixo + "  " + i + " - "  + getNome( i ) );
            
            saida.print( prefixo + "Escolha o parâmetro pelo índice (-1 para voltar): " );
            
            indice = entrada.nextInt();
            
            if ( indice < 0 )
                break;
            
            lerValor( entrada, saida, indice );
        } while ( !linhaAtual.isInterrupted() );
    }
    
    public void ler( Scanner entrada ) {
        ler( entrada, System.out );
    }
    
    public void ler( PrintStream saida ) {
        ler( new Scanner( System.in ), saida );
    }
    
    public void ler() {
        ler( new Scanner( System.in ), System.out );
    }
    
    public Parametros clone() {
        return new Parametros( nome, valor );
    }
    
    public Parametros clone( String novoId ) {
        return new Parametros( novoId, nome, valor );
    }
}