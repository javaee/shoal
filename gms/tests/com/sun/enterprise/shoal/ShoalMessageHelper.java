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

package com.sun.enterprise.shoal;


import java.io.*;
import java.util.zip.GZIPOutputStream;

public class ShoalMessageHelper {

    public static final byte[] serializeObject(Object obj)
            throws java.io.NotSerializableException, java.io.IOException {
        byte[] data = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            data = bos.toByteArray();
        } catch (java.io.NotSerializableException notSerEx) {
            throw notSerEx;
        } catch (Exception th) {
            IOException ioEx = new IOException(th.toString());
            ioEx.initCause(th);
            throw ioEx;
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Exception ex) {
                }
            }
            try {
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception ex) {
            }
        }

        return data;
    }

    public static final Object deserializeObject(byte[] data)
            throws Exception {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try {
            bis = new ByteArrayInputStream(data);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
        } catch (Exception ex) {
            throw ex;
        } finally {
            try {
                if (ois != null) {
                    ois.close();
                }
            } catch (Exception ex) {
            }
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (Exception ex) {
            }
        }
        return obj;
    }

    /**
     * Create serialized byte[] for <code>obj</code>.
     *
     * @param obj - serialize obj
     * @return byte[] containing serialized data stream for obj
     */
    /*    Keep around for when we may want to try out compressing messages.
    static public byte[] getByteArray(Serializable obj)
            throws IOException {
        return getByteArray(obj, false);
    }

    static public byte[] getByteArray(Serializable obj, boolean compress)
            throws IOException {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        byte[] obs;
        try {
            bos = new ByteArrayOutputStream();
            if (compress) {
                oos = new ObjectOutputStream(new GZIPOutputStream(bos));
            } else {
                oos = new ObjectOutputStream(bos);
            }
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            oos = null;
            obs = bos.toByteArray();
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Exception e) {}
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (Exception e) {}
            }
        }

        return obs;
    }
    */
}
