package net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TestServer {

    public TestServer() {
    }

    /**
     * Wait for connections
     */
    public void run() {

        System.out.println("Starting ServerSocket");
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(9000);
            serverSocket.setSoTimeout(0);
        } catch (IOException e) {
            System.out.println("failed to create a server socket");
            e.printStackTrace();
            System.exit(-1);
        }

        while (true) {
            try {
                System.out.println("Waiting for connections");
                Socket socket = serverSocket.accept();
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * main
     *
     * @param args none recognized.
     */
    public static void main(String args[]) {
        try {
            Thread.currentThread().setName(TestServer.class.getName() + ".main()");
            TestServer testServer = new TestServer();
            testServer.run();
        } catch (Throwable e) {
            System.err.println("Failed : " + e);
            e.printStackTrace(System.err);
            System.exit(-1);
        }
    }

}
