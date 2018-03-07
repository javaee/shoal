/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.shoal.ha.cache.impl.util;

import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.HashableKey;
import org.glassfish.ha.store.util.SimpleMetadata;
import org.shoal.adapter.store.ReplicatedBackingStore;
import org.shoal.adapter.store.ReplicatedBackingStoreFactory;
import org.shoal.ha.cache.impl.command.CommandManager;
import org.shoal.ha.mapper.DefaultKeyMapper;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
public class SimpleStoreableBackingStoreShell {

    BackingStore<MyKey, SimpleMetadata> ds;

    ConcurrentHashMap<MyKey, SimpleMetadata> cache =
            new ConcurrentHashMap<MyKey, SimpleMetadata>();

    CommandManager<MyKey, SimpleMetadata> cm;

    int counter = 0;

    public static void main(String[] args)
        throws Exception {
        //DefaultKeyMapper keyMapper = new DefaultKeyMapper(args[1], args[2]);

        BackingStoreConfiguration<MyKey, SimpleMetadata> conf = new BackingStoreConfiguration<MyKey, SimpleMetadata>();
        conf.setStoreName(args[0])
                .setInstanceName(args[1])
                .setClusterName(args[2])
                .setKeyClazz(MyKey.class)
                .setValueClazz(SimpleMetadata.class)
                .setClassLoader(ClassLoader.getSystemClassLoader());
        Map<String, Object> map = conf.getVendorSpecificSettings();
        map.put("start.gms", true);
        //map.put("local.caching", true);
        map.put("class.loader", ClassLoader.getSystemClassLoader());
        map.put("async.replication", true);
        BackingStore<MyKey, SimpleMetadata> ds = (BackingStore<MyKey, SimpleMetadata>)
                (new ReplicatedBackingStoreFactory()).createBackingStore(conf);

        SimpleStoreableBackingStoreShell main = new SimpleStoreableBackingStoreShell();
        main.runShell(ds);
    }

    private void runShell(BackingStore<MyKey, SimpleMetadata> ds) {


        cm = ((ReplicatedBackingStore<MyKey, SimpleMetadata>) ds)
                .getDataStoreContext().getCommandManager();

        this.ds = ds;
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
        do {
            prompt();
            try {
                line = br.readLine();
                List<String> args = new ArrayList<String>();
                for (StringTokenizer tok = new StringTokenizer(line, "\n\r\t\f \f"); tok.hasMoreTokens();) {
                    String str = tok.nextToken();
                    args.add(str);
                }

                if (args.size() > 0) {
                    String command = args.remove(0);
                    String[] params = args.toArray(new String[0]);

                    execute(command, params);
                    counter++;
                }
            } catch (IOException  ioEx) {
                ioEx.printStackTrace();
            } catch (BackingStoreException bsEx) {
                bsEx.printStackTrace();

            }
        } while (!"quit".equalsIgnoreCase(line));
    }

    private void prompt() {
        System.out.print("" + counter + ">");
        System.out.flush();
    }

    private void execute(String command, String[] params)
        throws BackingStoreException {

        if ("put".equalsIgnoreCase(command)) {
            String key = params[0];

            for (int i=0; i<8; i++) {
                MyKey key1 = new MyKey(params[0] + ":" + i, key);
                SimpleMetadata st = cache.get(key1);
                long version = st == null ? 0 : st._storeable_getVersion() + 1;

                st = new SimpleMetadata(version,
                            System.currentTimeMillis(), 600000,
                            ("Value:" + i + ":" + version).getBytes(Charset.defaultCharset()));
                cache.put(key1, st);
                String rs = ds.save(key1, st, true);
                System.out.println("PUT key = " + key1 + " : " + st + ";   TO : " + rs);
            }

            /*
            for (int i=0; i<8; i++) {
                MyKey key1 = new MyKey(params[0] + ":" + i, key);
                System.out.println("(local) GET key = " + key1 + " : "
                        + cache.get(key1));
            }
            */
        } else if ("get".equalsIgnoreCase(command)) {
            MyKey key = new MyKey(params[0], null);
            SimpleMetadata st = ds.load(key, params.length > 1 ? params[1] : null);
            if (st != null) {
                System.out.println("get(" + params[0] + ") => " + st + " ==> " + new String(st.getState(), Charset.defaultCharset()));
                cache.put(key, st);
            } else {
                System.out.println("get(" + params[0] + ") NOT FOUND ==> null");
            }
        } else if ("touch".equalsIgnoreCase(command)) {
            MyKey key = new MyKey(params[0], null);
            SimpleMetadata st = ds.load(key, params.length > 1 ? params[1] : null);
            /*
            st.touch();
            String result = null;
            ds.updateTimestamp(params[0], st._storeable_getLastAccessTime());
            System.out.println("Result of touch: " + result);
            */
        } else if ("remove".equalsIgnoreCase(command)) {
            MyKey key = new MyKey(params[0], null);
            ds.remove(key);
        } /* else if ("list-backing-store-config".equalsIgnoreCase(command)) {
            ReplicationFramework framework = ds.getFramework();
            ListBackingStoreConfigurationCommand cmd = new ListBackingStoreConfigurationCommand();
            try {
                framework.execute(cmd);
                ArrayList<String> confs = cmd.getResult(6, TimeUnit.SECONDS);
                for (String str : confs) {
                    System.out.println(str);
                }
            } catch (DataStoreException dse) {
                System.err.println(dse);
            }
        } else if ("list-entries".equalsIgnoreCase(command)) {
            ReplicationFramework framework = ds.getFramework();
            ListReplicaStoreEntriesCommand cmd = new ListReplicaStoreEntriesCommand(params[0]);
            try {
                framework.execute(cmd);
                ArrayList<String> confs = cmd.getResult(6, TimeUnit.SECONDS);
                for (String str : confs) {
                    System.out.println(str);
                }
            } catch (DataStoreException dse) {
                System.err.println(dse);
            }
        } */
    }

    private static class MyKey
        implements Serializable, HashableKey {
        String myKey;
        String rootKey;

        MyKey(String myKey, String rootKey) {
            this.myKey = myKey;
            this.rootKey = rootKey;
        }

        @Override
        public Object getHashKey() {
            return rootKey;
        }

        public int hashCode() {
            return myKey.hashCode();
        }

        public boolean equals(Object other) {
            System.out.println("Equals(" + other + ")");
            if (other instanceof MyKey) {
                MyKey k2 = (MyKey) other;
                System.out.println(this + ".equals(" + other + ") = " + k2.myKey.equals(myKey));
                return k2.myKey.equals(myKey);
            }

            return false;
        }

        public String toString() {
            return myKey;
        }
    }
}
