package org.shoal.test.maptest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.shoal.ha.cache.impl.util.DefaultKeyMapper;

/**
 * Unit test for simple App.
 */
public class KeyMapperTest
        extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public KeyMapperTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(KeyMapperTest.class);
    }


    private static void mapStringKeyTest(DefaultKeyMapper km) {
        String[] keys = new String[]{"Key0", "Key1", "Key2"};

        System.out.print("{");
        String delim = "";
        for (String key : keys) {
            System.out.print(delim + key + " => " + km.getMappedInstance("g1", key));
            delim = ", ";
        }

        System.out.println("}");
    }

    public void testRegisterOnly() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        assert (true);
    }

    public void testUnregisterOnly() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        km.removeInstance("n0");
        km.removeInstance("n1");
        assert (true);
    }

    public void testRegisterAndUnregister() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");

        km.removeInstance("n0");
        assert (true);
    }

    public void testEmptyMapTest() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");
        String mappedInstance = km.getMappedInstance("g1", "Key1");
        String replicaInstance = km.findReplicaInstance("g1", "Key1");

        System.out.println("test[testEmptyMapTest] => " + mappedInstance + " : " +
                replicaInstance);
        assert (mappedInstance != null);
        assert (replicaInstance != null);
        assert (mappedInstance.equals(replicaInstance));
    }

    public void testRegisterAndTest() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        mapStringKeyTest(km);

        km.registerInstance("inst0");
        km.registerInstance("inst1");
        km.registerInstance("instancen0");
        km.registerInstance("instancen1");
        mapStringKeyTest(km);

        assert (true);
    }

    public void testMappedToMyself() {
        DefaultKeyMapper km = new DefaultKeyMapper("n0", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        km.registerInstance("n2");
        km.registerInstance("n3");
        km.registerInstance("n4");
        km.registerInstance("n5");

        Integer[] keys = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        String[] expectedReplica = new String[]{
                "n1", "n1", "n2", "n3", "n4", "n5",
                "n2", "n1", "n2", "n3", "n4", "n5",
                "n3", "n1", "n2"};

        boolean result = true;
        for (int i = 0; i < keys.length; i++) {
            if (!km.getMappedInstance("g1", keys[i]).equals(expectedReplica[i])) {
                result = false;
                System.err.println("For key: " + keys[i] + " exptected Replica was: " + expectedReplica[i] +
                        " but got mapped to: " + km.getMappedInstance("g1", keys[i]));
            }
        }
        System.out.println("* Test[testMappedToMyself] => " + result);
        assert (result);
    }

    public void testReplicaUponFailure() {
        DefaultKeyMapper km = new DefaultKeyMapper("n2", "g1");

        km.registerInstance("n0");
        km.registerInstance("n1");
        km.registerInstance("n2");
        km.registerInstance("n3");
        km.registerInstance("n4");
        km.registerInstance("n5");

        Integer[] keys = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        String[] expectedReplica = new String[]{
                "n1", "n1", "n2", "n3", "n4", "n5",
                "n2", "n1", "n2", "n3", "n4", "n5",
                "n3", "n1", "n2"};

        km.removeInstance("n0");
        boolean result = true;
        for (int i = 0; i < keys.length; i++) {
            if (!km.findReplicaInstance("g1", keys[i]).equals(expectedReplica[i])) {
                result = false;
                System.err.println("For key: " + keys[i] + " exptected Replica was: " + expectedReplica[i] +
                        " but got mapped to: " + km.findReplicaInstance("g1", keys[i]));
            }
        }
        System.out.println("* Test[testReplicaUponFailure] => " + result);
        assert (result);
    }

    public void testReplicaUponFailureFromMultipleNodes() {
        DefaultKeyMapper km2 = new DefaultKeyMapper("n2", "g1");

        km2.registerInstance("n0");
        km2.registerInstance("n1");
        km2.registerInstance("n2");
        km2.registerInstance("n3");
        km2.registerInstance("n4");
        km2.registerInstance("n5");

        DefaultKeyMapper km5 = new DefaultKeyMapper("n5", "g1");

        km5.registerInstance("n0");
        km5.registerInstance("n1");
        km5.registerInstance("n2");
        km5.registerInstance("n3");
        km5.registerInstance("n4");
        km5.registerInstance("n5");

        Integer[] keys = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        String[] expectedReplica = new String[]{
                "n0", "n1", "n2", "n4", "n4", "n5",
                "n0", "n1", "n2", "n5", "n4", "n5",
                "n0", "n1", "n2"};

        km2.removeInstance("n3");
        km5.removeInstance("n3");
        boolean result = true;
        for (int i = 0; i < keys.length; i++) {
            if (!km2.findReplicaInstance("g1", keys[i]).equals(expectedReplica[i])) {
                result = false;
                System.err.println("For key: " + keys[i] + " exptected Replica was: " + expectedReplica[i] +
                        " but got mapped to: " + km2.findReplicaInstance("g1", keys[i]));
            }
        }
        for (int i = 0; i < keys.length; i++) {
            if (!km5.findReplicaInstance("g1", keys[i]).equals(expectedReplica[i])) {
                result = false;
                System.err.println("For key: " + keys[i] + " exptected Replica was: " + expectedReplica[i] +
                        " but got mapped to: " + km5.findReplicaInstance("g1", keys[i]));
            }
        }
        for (int i = 0; i < keys.length; i++) {
            if (!km2.findReplicaInstance("g1", keys[i]).equals(km5.findReplicaInstance("g1", keys[i]))) {
                result = false;
                System.err.println("For key: " + keys[i] + " km2 : " + expectedReplica[i] +
                        " but km2 replied: " + km2.findReplicaInstance("g1", keys[i]) +
                        " AND km5 replied: " + km5.findReplicaInstance("g1", keys[i]));
            }
        }

        System.out.println("* Test[testReplicaUponFailureFromMultipleNodes] => " + result);
        assert (result);
    }

    public void testReplicaUponFailureFromAllOtherNodes() {
        String[] survivingNodes = new String[]{"n0", "n1", "n2", "n4", "n5"};
        DefaultKeyMapper[] mappers = new DefaultKeyMapper[5];
        for (int i = 0; i < mappers.length; i++) {
            mappers[i] = new DefaultKeyMapper(survivingNodes[i], "g1");
        }

        for (int i = 0; i < mappers.length; i++) {
            DefaultKeyMapper mapper = mappers[i];

            mapper.registerInstance("n0");
            mapper.registerInstance("n1");
            mapper.registerInstance("n2");
            mapper.registerInstance("n3");
            mapper.registerInstance("n4");
            mapper.registerInstance("n5");

            mapper.removeInstance("n3");
        }


        Integer[] keys = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        String[] expectedReplica = new String[]{
                "n0", "n1", "n2", "n4", "n4", "n5",
                "n0", "n1", "n2", "n5", "n4", "n5",
                "n0", "n1", "n2"};

        boolean result = true;
        for (int j = 0; j < mappers.length; j++) {
            DefaultKeyMapper mapper = mappers[j];

            for (int i = 0; i < keys.length; i++) {
                if (!mapper.findReplicaInstance("g1", keys[i]).equals(expectedReplica[i])) {
                    result = false;
                    System.err.println("Mapper["+j+"] For key: " + keys[i] + " exptected Replica was: " + expectedReplica[i] +
                            " but got mapped to: " + mapper.findReplicaInstance("g1", keys[i]));
                }
            }
        }

        System.out.println("* Test[testReplicaUponFailureFromAllOtherNodes] => " + result);
        assert (result);
    }

    public void testReplicaUponRestart() {
        Integer[] keys = new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14};
        String[] expectedReplica = new String[]{
                "n0", "n1", "n2", "n4", "n4", "n5",
                "n0", "n1", "n2", "n5", "n4", "n5",
                "n0", "n1", "n2"};

        boolean result = true;




        DefaultKeyMapper mapper = new DefaultKeyMapper("n3", "g1");
        mapper.registerInstance("n0");
        mapper.registerInstance("n1");
        mapper.registerInstance("n2");
        mapper.registerInstance("n4");
        mapper.registerInstance("n5");
        mapper.removeInstance("n3");
        mapper.registerInstance("n3");
        mapper.setDebug(true);
        for (int i = 0; i < keys.length; i++) {
            if (!mapper.findReplicaInstance("g1", keys[i]).equals(expectedReplica[i])) {
                result = false;
                System.err.println("testReplicaUponRestart:: For key: " + keys[i] + " exptected Replica was: " + expectedReplica[i] +
                        " but got mapped to: " + mapper.findReplicaInstance("g1", keys[i]));
            }
        }


        System.out.println("* Test[testReplicaUponRestart] => " + result);
        assert (result);
    }

}