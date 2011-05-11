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

package com.sun.enterprise.mgmt.transport;

import com.sun.enterprise.ee.cms.logging.GMSLogDomain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class that can be used by any calling code to do common routines about Network I/O
 *
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
    public volatile static InetAddress firstInetAddressV4;
    public volatile static InetAddress firstInetAddressV6;

    private static final boolean IS_AIX_JDK;

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

    private static Method isLoopbackMethod = null;
    private static Method isUpMethod = null;
    private static Method supportsMulticastMethod = null;

    static {
        // JDK 1.6
        try {
            isLoopbackMethod = NetworkInterface.class.getMethod( "isLoopback" );
        } catch( Throwable t ) {
            isLoopbackMethod = null;
        }
        try {
            isUpMethod = NetworkInterface.class.getMethod( "isUp" );
        } catch( Throwable t ) {
            isUpMethod = null;
        }
        try {
            supportsMulticastMethod = NetworkInterface.class.getMethod( "supportsMulticast" );
        } catch( Throwable t ) {
            supportsMulticastMethod = null;
        }
        String vendor = System.getProperty("java.vendor");
        IS_AIX_JDK = vendor == null ? false  : vendor.startsWith("IBM");
    }

    /**
     * Returns all local addresses except for lookback and any local address
     * But, if any addresses were not found locally, the lookback is added to the list.
     *
     * @return List which contains available addresses locally
     */
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
                if (!isUp(anInterface)) {
                    continue;
                }
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

    /**
     * Return a first network interface except for the lookback
     * But, if any network interfaces were not found locally, the lookback interface is returned.
     *
     * @return a first network interface
     * @throws IOException if an I/O error occurs or a network interface was not found
     */
    public static NetworkInterface getFirstNetworkInterface() throws IOException {
        if( firstNetworkInterface != null )
            return firstNetworkInterface;
        NetworkInterface loopback = null;
        NetworkInterface firstInterface = null;
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while( interfaces != null && interfaces.hasMoreElements() ) {
            NetworkInterface anInterface = interfaces.nextElement();
            if( isLoopbackNetworkInterface( anInterface ) ) {
                loopback = anInterface;
                continue;
            }
            if( supportsMulticast( anInterface ) &&
                ( getFirstInetAddress( anInterface, false ) != null ||
                  getFirstInetAddress( anInterface, true ) != null ) ) {
                firstInterface = anInterface;
                break;
            }
        }
        if( firstInterface == null )
            firstInterface = loopback;
        if( firstInterface == null ) {
            throw new IOException( "failed to find a network interface" );
        } else {
            firstNetworkInterface = firstInterface;
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("getFirstNetworkInterface  result: interface name" + firstNetworkInterface.getName() + " address:" + firstNetworkInterface.getInetAddresses().nextElement().toString());
            }
            return firstNetworkInterface;
        }
    }

    /**
     * Return a first <code>InetAddress</code> of the first network interface
     * But, if any network interfaces were not found locally, <code>null</code> could be returned.
     *
     * @param preferIPv6 if true, prefer IPv6 InetAddress. otherwise prefer IPv4 InetAddress
     * @return a first found <code>InetAddress</code>.
     * @throws IOException if an I/O error occurs or a network interface was not found
     */
    public static InetAddress getFirstInetAddress( boolean preferIPv6 ) throws IOException {
//        LOG.info("enter getFirstInetAddress preferIPv6=" + preferIPv6);
        if( preferIPv6 && firstInetAddressV6 != null ) {
//            LOG.info("exit getFirstInetAddress cached ipv6 result=" + firstInetAddressV6);
            return firstInetAddressV6;
        }
        else if( !preferIPv6 && firstInetAddressV4 != null ) {
//            LOG.info("exit getFirstInetAddress cached ipv4 result=" + firstInetAddressV4);
            return firstInetAddressV4;
        }
        NetworkInterface anInterface = getFirstNetworkInterface();
//        LOG.info("getFirstInetAddress: first network interface=" + anInterface);
        Enumeration<InetAddress> allIntfAddr = anInterface.getInetAddresses();
        while( allIntfAddr.hasMoreElements() ) {
            InetAddress anAddr = allIntfAddr.nextElement();
//            LOG.info("getFirstInetAddress: anAddr=" + anAddr);
            if( anAddr.isLoopbackAddress() || anAddr.isAnyLocalAddress() )
                continue;
            if( firstInetAddressV6 == null && anAddr instanceof Inet6Address )
                firstInetAddressV6 = anAddr;
            else if( firstInetAddressV4 == null && anAddr instanceof Inet4Address )
                firstInetAddressV4 = anAddr;
            if( firstInetAddressV6 != null && firstInetAddressV4 != null )
                break;
        }
        if( preferIPv6 ) {
//            LOG.info("exit getFirstInetAddress ipv6 result=" + firstInetAddressV6);
            return firstInetAddressV6;
        }else {
//            LOG.info("exit getFirstInetAddress ipv4 result=" + firstInetAddressV4);
            return firstInetAddressV4;
        }
    }

    /**
     * Return a first <code>InetAddress</code> of the given network interface
     *
     * @param anInterface <code>NeteworkInterface</code>
     * @param preferIPv6 if true, prefer IPv6 InetAddress. otherwise prefer IPv4 InetAddress
     * @return a first found <code>InetAddress</code>. returns <code>null</code> if not found.
     * @throws IOException if an I/O error occurs or a network interface was not found
     */
    public static InetAddress getFirstInetAddress( NetworkInterface anInterface, boolean preferIPv6 ) throws IOException {
        if( anInterface == null )
            return null;
        InetAddress firstInetAddressV4 = null;
        InetAddress firstInetAddressV6 = null;
        Enumeration<InetAddress> allIntfAddr = anInterface.getInetAddresses();
        while( allIntfAddr.hasMoreElements() ) {
            InetAddress anAddr = allIntfAddr.nextElement();
            if( anAddr.isLoopbackAddress() || anAddr.isAnyLocalAddress() )
                continue;
            if( firstInetAddressV6 == null && anAddr instanceof Inet6Address )
                firstInetAddressV6 = anAddr;
            else if( firstInetAddressV4 == null && anAddr instanceof Inet4Address )
                firstInetAddressV4 = anAddr;
            if( firstInetAddressV6 != null && firstInetAddressV4 != null )
                break;
        }
        if( preferIPv6 )
            return firstInetAddressV6;
        else
            return firstInetAddressV4;
    }

    public static boolean isLoopbackNetworkInterface( NetworkInterface anInterface ) {
        if( anInterface == null )
            return false;
        if( isLoopbackMethod != null ) {
            try {
                return (Boolean)isLoopbackMethod.invoke( anInterface );
            } catch( Throwable t ) {
            }
        }
        boolean hasLoopback = false;
        Enumeration<InetAddress> allIntfAddr = anInterface.getInetAddresses();
        while( allIntfAddr.hasMoreElements() ) {
            InetAddress anAddr = allIntfAddr.nextElement();
            if( anAddr.isLoopbackAddress() ) {
                hasLoopback = true;
                break;
            }
        }
        return hasLoopback;
    }

    public static boolean supportsMulticast( NetworkInterface anInterface ) {
        if( anInterface == null )
            return false;
        boolean result = true;
        if( isUpMethod != null ) {
            try {
                result = (Boolean)isUpMethod.invoke( anInterface );
            } catch( Throwable t ) {
                result = false;
            }
        }
        if (!result) {
            return result;
        } else if (IS_AIX_JDK) {

            // workaround for Network.supportsMulticast not working properly on AIX.
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Workaround for java.net.NetworkInterface.supportsMulticast() returning false on AIX");
            }
            return true;
        } else if( supportsMulticastMethod != null) {
            try {
                return (Boolean)supportsMulticastMethod.invoke( anInterface );
            } catch( Throwable t ) {
                // will just return false in this case
            }
        }

        return false;
    }

    public static boolean isUp( NetworkInterface anInterface ) {
        if( anInterface == null )
            return false;
        if( isUpMethod != null ) {
            try {
                return (Boolean)isUpMethod.invoke( anInterface );
            } catch( Throwable t ) {
                // will just return false in this case
            }
        }
        return false;
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
        return serialize( baos, messages, false);
    }

    public static int serialize( final ByteArrayOutputStream baos, final Map<String, Serializable> messages, final boolean debug ) throws MessageIOException {
        int count = 0;
        if( baos == null || messages == null )
            return count;
        String name = null;
        ObjectOutputStream oos = null;
        try {
            if( debug ) {
                oos = new DebuggingObjectOutputStream( baos );
            } else {
                oos = new ObjectOutputStream( baos );
            }
            for( Map.Entry<String, Serializable> entry : messages.entrySet() ) {
                name = entry.getKey();
                Serializable obj = entry.getValue();
                count++;
                oos.writeObject( name );
                oos.writeObject( obj );
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

    public static void deserialize( final InputStream is, final int count, final Map<String, Serializable> messages ) throws MessageIOException {
        if( is == null || count <= 0 || messages == null )
            return;
        String name = null;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream( is );
            Object obj = null;
            for( int i = 0; i < count; i++ ) {
                name = (String)ois.readObject();
                obj = ois.readObject();
                if( obj instanceof Serializable ) {
                    messages.put( name, (Serializable)obj );
                }
            }
        } catch( Throwable t ) {
            LOG.log(Level.WARNING,
                    "netutil.deserialize.failure", new Object[]{messages.toString(), name, Thread.currentThread().getName()});
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

    /**
     * Returns an available tcp port between <code>tcpStartPort</code> and <code>tcpEndPort</code>
     *
     * @param host specific host name
     * @param tcpStartPort start port
     * @param tcpEndPort end port
     * @return an available tcp port which is not bound yet. Throws IllegalStateException if no ports exist.
     */
    /*
    // Using grizzly tcp port selection from a range.
    public static int getAvailableTCPPort( String host, int tcpStartPort, int tcpEndPort ) {
        if( tcpStartPort > tcpEndPort )
            tcpEndPort = tcpStartPort + 30;
        for( int portInRange = tcpStartPort; portInRange <= tcpEndPort; portInRange++ ) {
            ServerSocket testSocket = null;
            try {
                testSocket = new ServerSocket( portInRange, -1, host == null ? null : InetAddress.getByName( host ) );
            } catch( IOException ie ) {
                continue;
            } finally {
                if( testSocket != null ) {
                    try {
                        testSocket.close();
                    } catch( IOException e ) {
                    }
                }
            }
            return portInRange;
        }
        LOG.log(Level.SEVERE, "netutil.no.available.ports", new Object[]{host,tcpStartPort,tcpEndPort});
        throw new IllegalStateException("Fatal error. No available ports exist for " + host + " in range " + tcpStartPort + " to " + tcpEndPort);
    }
    */

    private static final Field DEPTH_FIELD;

    static {
        try {
            DEPTH_FIELD = ObjectOutputStream.class.getDeclaredField( "depth" );
            DEPTH_FIELD.setAccessible( true );
        } catch( NoSuchFieldException e ) {
            throw new AssertionError( e );
        }
    }

    /**
     * This class extends <code>ObjectOutputStream</code> for providing any debugging informations when an object is written
     */
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

    public static boolean isBindAddressValid(String addressString) {
        ServerSocket socket = null;
        try {
            InetAddress ia = Inet4Address.getByName(addressString);

            // port 0 means any free port
            // backlog 0 means use default
            socket = new ServerSocket(0, 0, ia);

            // make extra sure
            boolean retVal = socket.isBound();
            if (!retVal) {
                LOG.log(Level.WARNING, "netutil.validate.bind.not.bound",
                    addressString);
            }
            return retVal;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "netutil.validate.bind.address.exception",
                new Object [] {addressString, e.toString()});
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioe) {
                    LOG.log(Level.FINE,
                        "Could not close socket used to validate address.");
                }
            }
        }
    }

    public static void main( String[] args ) throws IOException {
        System.out.println( "AllLocalAddresses() = " + getAllLocalAddresses() );
        System.out.println( "getFirstNetworkInterface() = " +getFirstNetworkInterface() );
        System.out.println( "getFirstInetAddress( true ) = " + getFirstInetAddress( true ) );
        System.out.println( "getFirstInetAddress( false ) = " + getFirstInetAddress( false ) );
        System.out.println( "getFirstNetworkInteface() = " + NetworkUtility.getFirstNetworkInterface());
        System.out.println( "getFirstInetAddress(firstNetworkInteface, true) = " +
               NetworkUtility.getFirstInetAddress(NetworkUtility.getFirstNetworkInterface(),true));
        System.out.println( "getFirstInetAddress(firstNetworkInteface, false) = " +
               NetworkUtility.getFirstInetAddress(NetworkUtility.getFirstNetworkInterface(),false));
    }
}
