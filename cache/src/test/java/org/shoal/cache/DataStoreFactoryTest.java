package org.shoal.cache;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.shoal.ha.cache.api.DataStore;
import org.shoal.ha.cache.api.DataStoreFactory;

/**
 * Unit test for simple App.
 */
public class DataStoreFactoryTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public DataStoreFactoryTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( DataStoreFactoryTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        DataStore ds = DataStoreFactory.createDataStore("cache1", "instance1", "group1");
        assertTrue( true );
    }
}
