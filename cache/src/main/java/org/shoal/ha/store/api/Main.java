package org.shoal.ha.store.api;

/**
 * Created by IntelliJ IDEA.
 * User: mk
 * Date: Mar 24, 2010
 * Time: 9:39:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    public static void main(String[] args) {
        DataStore ds = DataStoreFactory.createDataStore(args[0], args[1], args[2]);
    }
}
