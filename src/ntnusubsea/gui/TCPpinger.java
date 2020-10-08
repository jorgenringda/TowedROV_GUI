/*
 * This code is for the bachelor thesis named "Towed-ROV".
 * The purpose is to build a ROV which will be towed behind a surface vessel
 * and act as a multi-sensor platform, were it shall be easy to place new 
 * sensors. There will also be a video stream from the ROV.
 * 
 * The system consists of two Raspberry Pis in the ROV that is connected to
 * several Arduino micro controllers. These micro controllers are connected to
 * feedback from the actuators, the echo sounder and extra optional sensors.
 * The external computer which is on the surface vessel is connected to a GPS,
 * echo sounder over USB, and the ROV over ethernet. It will present and
 * log data in addition to handle user commands for controlling the ROV.
 */
package ntnusubsea.gui;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client class that handles the connection to the server, retrieves the video
 * stream and sends commands to the server
 *@author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class TCPpinger implements Runnable {

    boolean connectionResetError = false;
    private boolean connected = false;
    private static String sentence;
    private static String serverResponse;
    private Socket clientSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int port;
    private String IP;
    private Data data;

    BufferedReader inFromServer;
    PrintWriter outToServer;

    /**
     * The constructor of the TCPClient.
     *
     * @param IP the given IP to connect to
     * @param port the given port to connect to
     * @param data the shared resource Data class
     */
    public TCPpinger(String IP, int port, Data data) {
        this.data = data;
        this.port = port;
        this.IP = IP;
    }

    /**
     * Runs the TCPpinger thread. Connects to the TCP server, and reconnects if
     * the connection is lost.
     */
    @Override
    public void run() {
//        if (!this.isConnected())
//        {
//            try {
//                this.connect(this.IP, this.port);
//            } catch (IOException ex) {
//                Logger.getLogger(TCPpinger.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        if (this.isConnected()) {
            data.setRovPing(this.getPing());
            //System.out.println("Ping (ROV): " + data.getRovPing());
        }
    }

    /**
     * Connects the client to the server through a socket and saves the IP and
     * port to the global variables IP and port
     *
     * @param IP IP of the server to connect to
     * @param port
     * @throws IOException Throws an IOException when the connection is
     * unsuccessful
     */
    public void connect(String IP, int port) throws IOException {
        clientSocket = new Socket(IP, port);
        clientSocket.setSoTimeout(3000);
        outToServer = new PrintWriter(
                clientSocket.getOutputStream(), true);
        inFromServer = new BufferedReader(new InputStreamReader(
                clientSocket.getInputStream()));
        System.out.println("Success! Connected to server " + this.IP + ":" + this.port);
        this.connected = true;
        this.connectionResetError = false;
    }

    /**
     * Closes the socket if the client is currently connected
     *
     * @throws IOException Throws IOException if there is a problem with the
     * connection
     */
    public void disconnect() throws IOException {
        if (clientSocket != null) {
            clientSocket.close();
        }
        connected = false;
    }

    /**
     * Sends a ping command to the TCP server.
     *
     * @return the ping of the connection
     */
    public Double getPing() {
        double pingValue = 0.00;
        double elapsedTimer = 0;
        double elapsedTimerNano = 0;
        long lastTime = 0;

        String ping = "<Ping:null>";
        lastTime = System.nanoTime();
        String serverResponse = sendData("ping");
        if (serverResponse != null && serverResponse.equals("<ping:true>")) {
            elapsedTimerNano = (System.nanoTime() - lastTime);
            elapsedTimer = elapsedTimerNano / 1000000;
            pingValue = elapsedTimer;
            //System.out.println("<Ping: " + elapsedTimer + ">");

            elapsedTimer = 0;
        } else {
            pingValue = 0.00;
            ping = "<Ping:null>";
        }
        return pingValue;
    }

    /**
     * Sends the given data string to the TCP server.
     *
     * @param sentence the given string
     * @return the server response
     */
    public String sendData(String sentence) {
        try {
            outToServer.println(sentence);
            //System.out.println("Data is sent...");
            outToServer.flush();
            serverResponse = inFromServer.readLine();

        } catch (SocketTimeoutException ste) {
            System.out.println("SocketTimeoutException: " + ste.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return serverResponse;
    }

    /**
     * Returns the connection status of the socket
     *
     * @return The connection status of the socket
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Returns the port of the server
     *
     * @return the port of the server
     */
    public int getPort() {
        return port;
    }

    /**
     * Sets the port of the server
     *
     * @param port the port of the server
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the IP of the server
     *
     * @return the IP of the server
     */
    public String getIP() {
        return IP;
    }

    /**
     * Sets the IP of the server
     *
     * @param IP the IP of the server
     */
    public void setIP(String IP) {
        this.IP = IP;
    }
}
