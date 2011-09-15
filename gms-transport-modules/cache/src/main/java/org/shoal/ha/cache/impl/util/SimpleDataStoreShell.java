/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

import org.glassfish.ha.store.api.Storeable;
import org.shoal.ha.cache.api.*;
import org.shoal.ha.mapper.DefaultKeyMapper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Mahesh Kannan
 */
public class SimpleDataStoreShell {

    DataStore<String, String> ds;

    ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<String, String>();

    int counter = 0;

    public static void main(String[] args)
        throws Exception {
        DefaultKeyMapper keyMapper = new DefaultKeyMapper(args[1], args[2]);

        DataStoreContext<String, String> conf = new DataStoreContext<String, String>();
        conf.setStoreName(args[0])
                .setInstanceName(args[1])
                .setGroupName(args[2])
                .setKeyClazz(String.class)
                .setValueClazz(String.class)
                .setClassLoader(ClassLoader.getSystemClassLoader())
                .setStartGMS(true)
                .setDoAddCommands();
        DataStore<String, String> rds = (new DataStoreFactory()).createDataStore(conf);

        SimpleDataStoreShell main = new SimpleDataStoreShell();
        main.runShell(rds);
    }

    private void runShell(DataStore<String, String> ds) {
        this.ds = ds;
        String line = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
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
            }
        } while (!"quit".equalsIgnoreCase(line));
    }

    private void prompt() {
        System.out.print("" + counter + ">");
        System.out.flush();
    }

    private void execute(String command, String[] params) {

        try {
        if ("put".equalsIgnoreCase(command)) {
            String key = params[0];
            ds.put(key, params[1]);
                System.out.println("PUT " + key);
                ds.put(key, params[1]);
            /*
            for (int i=1; i<8; i++) {
                String key1 = params[0] + ":" + i;
                MyStoreable st1 = cache.get(key1);
                if (st1 == null) {
                    st1 = new MyStoreable();
                    cache.put(key1, st1);
                }

                if (params.length > 1) {
                    st.setStr1(params[1] + ":" + i);
                }
                if (params.length > 2) {
                    st.setStr2(params[2] + ":" + i);
                }
                st.touch();
                System.out.println("PUT " + st);
                ds.save(key1, st1, true);
            }
            */
        } else if ("get".equalsIgnoreCase(command)) {
            String value = ds.get(params[0]);
            System.out.println("get(" + params[0] + ") => " + value);
            if (value != null) {
                cache.put(params[0], value);
            }
        } else if ("remove".equalsIgnoreCase(command)) {
            ds.remove(params[0]);
        }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static class MyStoreable
        implements Storeable {

        long version;

        long accessTime;

        long maxIdleTime;

        String str1;

        String str2;

        boolean[] dirty = new boolean[2];

        public void touch() {
            version++;
            accessTime = System.currentTimeMillis();
            maxIdleTime = 15 * 1000;
        }

        @Override
        public long _storeable_getVersion() {
            return version;
        }

        public String getStr1() {
            return str1;
        }

        public void setStr1(String str1) {
            boolean same = str1 != null
                            ? str1.equals(this.str1)
                            : this.str1 == null;
            if (! same) {
                this.str1 = str1;
                dirty[0] = true;
            }
        }

        public String getStr2() {
            return str2;
        }

        public void setStr2(String str2) {
            boolean same = str2 != null
                            ? str2.equals(this.str2)
                            : this.str2 == null;
            if (! same) {
                this.str2 = str2;
                dirty[1] = true;
            }
        }

        @Override
        public void _storeable_setVersion(long version) {
            this.version = version;
        }

        @Override
        public long _storeable_getLastAccessTime() {
            return maxIdleTime;
        }

        @Override
        public void _storeable_setLastAccessTime(long accessTime) {
            this.accessTime = accessTime;
        }

        @Override
        public long _storeable_getMaxIdleTime() {
            return maxIdleTime;
        }

        @Override
        public void _storeable_setMaxIdleTime(long maxIdleTime) {
            this.maxIdleTime = maxIdleTime;
        }

        @Override
        public String[] _storeable_getAttributeNames() {
            return new String[] {"str1", "str2"};
        }

        @Override
        public boolean[] _storeable_getDirtyStatus() {
            return dirty;
        }

        @Override
        public void _storeable_writeState(OutputStream os) throws IOException {
            DataOutputStream dos = null;
            try {
                dos = new DataOutputStream(os);
                dos.writeBoolean(dirty[0]);
                if (dirty[0]) {
                    dos.writeInt(str1 == null ? 0 : str1.length());
                    if (str1 != null) {
                        dos.write(str1.getBytes());
                    }
                }
                dos.writeBoolean(dirty[1]);
                if (dirty[1]) {
                    dos.writeInt(str2 == null ? 0 : str2.length());
                    if (str2 != null) {
                        dos.write(str2.getBytes());
                    }
                }
            } finally {
                try {dos.flush(); dos.close(); } catch (IOException ex) {}
            }
        }

        @Override
        public void _storeable_readState(InputStream is) throws IOException {
            DataInputStream dis = null;
            try {
                dis = new DataInputStream(is);
                dirty[0] = dis.readBoolean();
                if (dirty[0]) {
                    int strLen = dis.readInt();
                    if (strLen > 0) {
                        byte[] strBytes = new byte[strLen];
                        dis.read(strBytes);
                        str1 = new String(strBytes);
                    }
                }

                dirty[1] = dis.readBoolean();
                if (dirty[1]) {
                    int strLen = dis.readInt();
                    if (strLen > 0) {
                        byte[] strBytes = new byte[strLen];
                        dis.read(strBytes);
                        str2 = new String(strBytes);
                    }
                }
            } finally {
                try {dis.close(); } catch (IOException ex) {}
            }
        }

        @Override
        public String toString() {
            return "MyStoreable{" +
                    "version=" + version +
                    ", accessTime=" + accessTime +
                    ", maxIdleTime=" + maxIdleTime +
                    ", str1='" + str1 + '\'' +
                    ", str2='" + str2 + '\'' +
                    ", dirty=" + dirty +
                    '}';
        }
    }
}
