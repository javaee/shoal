package org.shoal.test.maptest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.shoal.ha.cache.api.DataStore;
import org.shoal.ha.cache.api.DataStoreFactory;
import org.shoal.ha.cache.impl.util.StringKeyMapper;

/**
 * Unit test for simple App.
 */
public class KeyMapperTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public KeyMapperTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( KeyMapperTest.class );
    }



    private static void mapTest(StringKeyMapper km) {
        String[] keys = new String[] {"Key0", "Key1", "Key2"};

        System.out.print("{");
        String delim = "";
        for (String key : keys) {
            System.out.print(delim + key + " => " + km.getMappedInstance("g1", key));
            delim = ", ";
        }

        System.out.println("}");
    }

    /**
     * Rigourous Test :-)
     */
    public void testRegisterOnly()
    {
        StringKeyMapper km = new StringKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        km.printMemberStates();
        assert(true);
    }

    /**
     * Rigourous Test :-)
     */
    public void testRegister()
    {
        StringKeyMapper km = new StringKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        mapTest(km);
        mapTest(km);

        km.printMemberStates();
        assert(true);
    }

    /**
     * Rigourous Test :-)
     */
    public void testRegisterAndTest()
    {
        StringKeyMapper km = new StringKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        mapTest(km);

        km.registerInstance("inst0");
        km.registerInstance("inst1");
        km.registerInstance("instancen0");
        km.registerInstance("instancen1");
        mapTest(km);

        km.printMemberStates();
        assert(true);
    }
}