/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net;


        
import java.net.*;
import java.io.*;

public class MulticastSender {

    public static void main(String[] args) {
        InetAddress ia = null;
        InetAddress interfaceAddress = null;
        int port = 0;
        byte ttl = (byte) 1;
// read the address from the command line
        try {
            ia = InetAddress.getByName(args[0]);
            port = Integer.parseInt(args[1]);
            if (args.length >= 3) {
                interfaceAddress = InetAddress.getByName(args[2]);
            }
        } catch (Exception ex) {
            System.err.println(ex);
            System.err.println(
                    "Usage: java MulticastSender multicast_address port multicast_interface_address");
            System.exit(1);
        }
        byte[] data = "Here's some multicast data\r\n".getBytes();
        DatagramPacket dp = new DatagramPacket(data, data.length, ia, port);
        try {
            //InetSocketAddress isa = new InetSocketAddress(interfaceAddress, port);
            //MulticastSocket ms = new MulticastSocket(isa);
            MulticastSocket ms = new MulticastSocket(port);
            if (interfaceAddress != null) {
                ms.setInterface(interfaceAddress);
            }
            ms.joinGroup(ia);
            for (int i = 1; i < 10; i++) {
                ms.send(dp);
            }
            ms.leaveGroup(ia);
            ms.close();
        } catch (SocketException ex) {
            System.err.println(ex);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }
}
