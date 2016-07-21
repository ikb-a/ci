package edu.toronto.cs.se.ci;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
    
    public void testMain(){
    	File f = new File(".");
    	System.out.println(f.getAbsolutePath());
    	String [] files =(f.list());
    	for(String file: files)
    		System.out.println(file);
    	
    	System.out.println("----target---");
    	f = new File("./target");
    	files =(f.list());
    	for(String file: files)
    		System.out.println(file);
    	
    	System.out.println("-----test-classes=====");
    	f = new File("./target/test-classes");
    	files =(f.list());
    	for(String file: files)
    		System.out.println(file);
    }
}
