/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
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
import org.shoal.adapter.store.ReplicatedBackingStoreFactory;
import org.shoal.adapter.store.commands.*;
import org.shoal.ha.cache.api.*;
import org.shoal.ha.cache.impl.command.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Mahesh Kannan
 */
public class DataStoreShell {

    BackingStore<String, Serializable> ds;

    int counter = 0;

    public static void main(String[] args)
        throws Exception {
        DataStoreKeyHelper<String> keyHelper = new StringKeyHelper();
        DefaultKeyMapper keyMapper = new DefaultKeyMapper(args[1], args[2]);

        BackingStoreConfiguration<String, Serializable> conf = new BackingStoreConfiguration<String, Serializable>();
        conf.setStoreName(args[0])
                .setInstanceName(args[1])
                .setClusterName(args[2])
                .setKeyClazz(String.class)
                .setValueClazz(Serializable.class)
                .setClassLoader(ClassLoader.getSystemClassLoader());
        Map<String, Object> map = conf.getVendorSpecificSettings();
        map.put("start.gms", true);
        map.put("local.caching", true);
        map.put("class.loader", ClassLoader.getSystemClassLoader());
        BackingStore<String, Serializable> ds =
                (new ReplicatedBackingStoreFactory()).createBackingStore(conf);

        DataStoreShell main = new DataStoreShell();
        main.runShell(ds);
    }

    private void runShell(BackingStore<String, Serializable> ds) {
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
            ds.save(params[0], params[1], true);
        } else if ("get".equalsIgnoreCase(command)) {
            System.out.println("get(" + params[0] + ") => " + ds.load(params[0], null));
        } else if ("remove".equalsIgnoreCase(command)) {
            ds.remove(params[0]);
        }
    }
}
