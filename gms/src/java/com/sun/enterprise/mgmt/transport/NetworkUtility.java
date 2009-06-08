/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
 * @author Bongjae Chang
 */
public class NetworkUtility {

    private static final Logger LOG = GMSLogDomain.getLogger( GMSLogDomain.GMS_LOGGER );

    public static final String IPV4ANYADDRESS = "0.0.0.0";
    public static final String IPV6ANYADDRESS = "::";
    public static final String IPV4LOOPBACK = "127.0.0.1";
    public static final String IPV6LOOPBACK = "::1";

    /**
     * Constant which works as the IP "Any Address" value
     */
    public static final InetAddress ANYADDRESS;
    public static final InetAddress ANYADDRESSV4;
    public static final InetAddress ANYADDRESSV6;

    /**
     * Constant which works as the IP "Local Loopback" value;
     */
    public static final InetAddress LOOPBACK;
    public static final InetAddress LOOPBACKV4;
    public static final InetAddress LOOPBACKV6;

    public volatile static List<InetAddress> allLocalAddresses;
    public volatile static NetworkInterface firstNetworkInterface;

    static {
        InetAddress GET_ADDRESS = null;
        try {
            GET_ADDRESS = InetAddress.getByName( IPV4ANYADDRESS );
        } catch( Exception ignored ) {
            if( LOG.isLoggable( Level.FINE ) )
                LOG.log( Level.FINE, "failed to intialize ANYADDRESSV4. Not fatal", ignored );
        }
        ANYADDRESSV4 = GET_ADDRESS;

        GET_ADDRESS = null;
        try {
            GET_ADDRESS = InetAddress.getByName( IPV6ANYADDRESS );
        } catch( Exception ignored ) {
            if( LOG.isLoggable( Level.FINE ) )
                LOG.log( Level.FINE, "failed to intialize IPV6ANYADDRESS. Not fatal", ignored );
        }
        ANYADDRESSV6 = GET_ADDRESS;

        ANYADDRESS = ( ANYADDRESSV4 == null ) ? ANYADDRESSV6 : ANYADDRESSV4;

        GET_ADDRESS = null;
        try {
            GET_ADDRESS = InetAddress.getByName( IPV4LOOPBACK );
        } catch( Exception ignored ) {
            if( LOG.isLoggable( Level.FINE ) )
                LOG.log( Level.FINE, "failed to intialize IPV4LOOPBACK. Not fatal", ignored );
        }
        LOOPBACKV4 = GET_ADDRESS;

        GET_ADDRESS = null;
        try {
            GET_ADDRESS = InetAddress.getByName( IPV6LOOPBACK );
        } catch( Exception ignored ) {
            if( LOG.isLoggable( Level.FINE ) )
                LOG.log( Level.FINE, "failed to intialize ANYADDRESSV4. Not fatal", ignored );
        }
        LOOPBACKV6 = GET_ADDRESS;

        LOOPBACK = ( LOOPBACKV4 == null ) ? LOOPBACKV6 : LOOPBACKV4;

        if( LOOPBACK == null || ANYADDRESS == null )
            throw new IllegalStateException( "failure initializing statics. Neither IPV4 nor IPV6 seem to work" );
    }

    public static List<InetAddress> getAllLocalAddresses() {
        if( allLocalAddresses != null )
            return allLocalAddresses;
        List<InetAddress> allAddr = new ArrayList<InetAddress>();
        Enumeration<NetworkInterface> allInterfaces = null;
        try {
            allInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch( SocketException t ) {
            if( LOG.isLoggable( Level.INFO ) )
                LOG.log( Level.INFO, "Could not get local interfaces' list", t );
        }

        if( allInterfaces == null )
            allInterfaces = Collections.enumeration( Collections.<NetworkInterface>emptyList() );

        while( allInterfaces.hasMoreElements() ) {
            NetworkInterface anInterface = allInterfaces.nextElement();
            try {
                Enumeration<InetAddress> allIntfAddr = anInterface.getInetAddresses();
                while( allIntfAddr.hasMoreElements() ) {
                    InetAddress anAddr = allIntfAddr.nextElement();
                    if( anAddr.isLoopbackAddress() || anAddr.isAnyLocalAddress() )
                        continue;
                    if( !allAddr.contains( anAddr ) ) {
                        allAddr.add( anAddr );
                    }
                }
            } catch( Throwable t ) {
                if( LOG.isLoggable( Level.INFO ) )
                    LOG.log( Level.INFO, "Could not get addresses for " + anInterface, t );
            }
        }

        if( allAddr.isEmpty() ) {
            if( LOOPBACKV4 != null )
                allAddr.add( LOOPBACKV4 );
            if( LOOPBACKV6 != null )
                allAddr.add( LOOPBACKV6 );
        }
        allLocalAddresses = allAddr;
        return allLocalAddresses;
    }

    public static NetworkInterface getFirstNetworkInterface() throws IOException {
        if( firstNetworkInterface != null )
            return firstNetworkInterface;
        NetworkInterface loopback = null;
        NetworkInterface firstInterface = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while( interfaces != null && interfaces.hasMoreElements() ) {
            NetworkInterface anInterface = interfaces.nextElement();
            if( anInterface.isLoopback() ) {
                loopback = anInterface;
                continue;
            }
            if( anInterface.isUp() && anInterface.supportsMulticast() ) {
                firstInterface = anInterface;
                break;
            }
        }
        if( firstInterface == null )
            firstInterface = loopback;
        if( firstInterface == null ) {
            throw new IOException( "failed to find an network interface" );
        } else {
            firstNetworkInterface = firstInterface;
            return firstNetworkInterface;
        }
    }

    public static void writeIntToByteArray( final byte[] bytes, final int offset, final int value ) throws IllegalArgumentException {
        if( bytes == null )
            return;
        if( bytes.length < offset + 4 )
            throw new IllegalArgumentException( "bytes' length is too small" );
        bytes[offset + 0] = (byte)( ( value >> 24 ) & 0xFF );
        bytes[offset + 1] = (byte)( ( value >> 16 ) & 0xFF );
        bytes[offset + 2] = (byte)( ( value >> 8 ) & 0xFF );
        bytes[offset + 3] = (byte)( value & 0xFF );
    }

    public static int getIntFromByteArray( final byte[] bytes, final int offset ) throws IllegalArgumentException {
        if( bytes == null )
            return 0;
        if( bytes.length < offset + 4 )
            throw new IllegalArgumentException( "bytes' length is too small" );
        int ch1 = bytes[offset] & 0xff;
        int ch2 = bytes[offset + 1] & 0xff;
        int ch3 = bytes[offset + 2] & 0xff;
        int ch4 = bytes[offset + 3] & 0xff;
        return (int)( ( ch1 << 24 ) + ( ch2 << 16 ) + ( ch3 << 8 ) + ch4 );
    }

    public static int serialize( final ByteArrayOutputStream baos, final Map<String, Serializable> messages ) throws MessageIOException {
        return serialize( baos, messages, true );
    }

    public static int serialize( final ByteArrayOutputStream baos, final Map<String, Serializable> messages, final boolean debug ) throws MessageIOException {
        int count = 0;
        if( baos == null || messages == null )
            return count;
        String name = null;
        ObjectOutputStream oos = null;
        try {
            if( debug )
                oos = new DebuggingObjectOutputStream( baos );
            else
                oos = new ObjectOutputStream( baos );
            for( Map.Entry<String, Serializable> entry : messages.entrySet() ) {
                name = entry.getKey();
                Serializable obj = entry.getValue();
                count++;
                oos.writeObject( name );
                synchronized( obj ) {
                    oos.writeObject( obj );
                }
            }
            oos.flush();
        } catch( Throwable t ) {
            throw new MessageIOException( "failed to serialize a message : name = " + name + "." +
                                          ( debug ? " path to bad object: " + ( (DebuggingObjectOutputStream)oos ).getStack() : "" ),
                                          t );
        } finally {
            if( oos != null ) {
                try {
                    oos.close();
                } catch( IOException e ) {
                }
            }
        }
        return count;
    }

    public static void deserialize( final ByteArrayInputStream bais, final int count, final Map<String, Serializable> messages ) throws MessageIOException {
        if( bais == null || count <= 0 || messages == null )
            return;
        String name = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream( bais );
            Object obj = null;
            for( int i = 0; i < count; i++ ) {
                name = (String)ois.readObject();
                obj = ois.readObject();
                if( obj instanceof Serializable )
                    messages.put( name, (Serializable)obj );
            }
        } catch( Throwable t ) {
            throw new MessageIOException( "failed to deserialize a message : name = " + name, t );
        } finally {
            if( ois != null ) {
                try {
                    ois.close();
                } catch( IOException e ) {
                }
            }
        }
    }

    private static final Field DEPTH_FIELD;

    static {
        try {
            DEPTH_FIELD = ObjectOutputStream.class.getDeclaredField( "depth" );
            DEPTH_FIELD.setAccessible( true );
        } catch( NoSuchFieldException e ) {
            throw new AssertionError( e );
        }
    }

    private static class DebuggingObjectOutputStream extends ObjectOutputStream {

        final List<Object> stack = new ArrayList<Object>();
        boolean broken = false;

        public DebuggingObjectOutputStream( OutputStream out ) throws IOException {
            super( out );
            enableReplaceObject( true );
        }

        protected Object replaceObject( Object o ) {
            int currentDepth = currentDepth();
            if( o instanceof IOException && currentDepth == 0 )
                broken = true;
            if( !broken ) {
                truncate( currentDepth );
                stack.add( o );
            }
            return o;
        }

        private void truncate( int depth ) {
            while( stack.size() > depth ) {
                pop();
            }
        }

        private Object pop() {
            return stack.remove( stack.size() - 1 );
        }

        private int currentDepth() {
            try {
                Integer oneBased = (Integer)DEPTH_FIELD.get( this );
                return oneBased - 1;
            } catch( IllegalAccessException e ) {
                throw new AssertionError( e );
            }
        }

        public List<Object> getStack() {
            return stack;
        }
    }

    public static void main( String[] args ) {
        System.out.println( getAllLocalAddresses() );
    }
}
