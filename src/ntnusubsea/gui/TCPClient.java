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
import java.util.HashMap;
import java.util.Map;

/**
 * Client class that handles the connection to the server, retrieves the video
 * stream and sends commands to the server
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356 edited 2020, added
 * feedback for stepper positions
 */
public class TCPClient extends Thread {

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

    String start_char = "<";
    String end_char = ">";
    String sep_char = ":";

    BufferedReader inFromServer;
    PrintWriter outToServer;

    /**
     * The constructor of the TCPClient.
     *
     * @param IP the given IP to connect to
     * @param port the given port to connect to
     * @param data the shared resource Data class
     */
    public TCPClient(String IP, int port, Data data) {
        this.data = data;
        this.port = port;
        this.IP = IP;
    }

    /**
     * Runs the TCPClient thread. Connects to the TCP server, and reconnects if
     * the connection is lost.
     */
    @Override
    public void run() {
        while (!this.connected) {
            try {
                this.connect(this.IP, this.port);
            } catch (Exception e1) {
                //System.out.println("Could not connect to server (" + this.IP + ":" + this.port + "): " + e1.getMessage());
                long sec = 5000;
                //System.out.println("Trying to reconnect in " + sec + " ms...");
                try {
                    Thread.sleep(sec);
                } catch (Exception e2) {
                }
            }
        }

        boolean finished = false;

        while (!finished) {
            try {
                //Run

                if (this.connectionResetError && !isConnected()) {
                    int sec = 5000;
                    System.out.println("Trying to reconnect (" + this.IP + ":" + this.port + ") in " + sec + " sec...");
                    Thread.sleep(sec);
                    this.connect(this.IP, this.port);
                }
            } catch (SocketTimeoutException ex) {
                System.out.println("Error: Read timed out");
                this.connectionResetError = true;
                this.connected = false;
            } catch (SocketException ex) {
                System.out.println("An error occured: Connection reset");
                this.connectionResetError = true;
                this.connected = false;
            } catch (Exception e) {
                System.out.println("An error occured: " + e);
                this.connectionResetError = true;
                this.connected = false;
            }
        }

    }

    /**
     * Sends a command to the server.
     *
     * @param cmd
     * @throws IOException Throws IOException if client is disconnected
     */
    public synchronized void sendCommand(String cmd) throws IOException {
        try {
            if (isConnected()) {

                String commandString = "<" + cmd + ">";
                outToServer.println(commandString);
//                System.out.println("Cmd sent: " + commandString);

                if (cmd.contains("stepper")) {
                    System.out.println("Stepper Cmd sent: " + commandString);
                }

                outToServer.flush();

                String serverResponse = inFromServer.readLine();
                if (serverResponse.contains("not ready")) {
                    System.out.println("Server not ready!");
                } else {
                    //System.out.println("Server response: " + serverResponse);
                    if (cmd.equals("fb_allData") || cmd.equals("getData")) {
                        HashMap<String, String> newDataList = new HashMap<>();
                        String key = "";
                        String value = "";
                        if (serverResponse.contains("<") && serverResponse.contains(">")) {
                            serverResponse = serverResponse.substring(serverResponse.indexOf(start_char) + 1);
                            serverResponse = serverResponse.substring(0, serverResponse.indexOf(end_char));
                            serverResponse = serverResponse.replace("?", "");
                            if (serverResponse.contains(":")) {
                                String[] dataArray = serverResponse.split(sep_char);
                                for (int i = 0; i < dataArray.length; i += 2) {
                                    newDataList.put(dataArray[i], dataArray[i + 1]);
                                }
                            } else {
                                System.out.println(serverResponse);
                            }

                        } else {
                            System.out.println("The data string which was received was not complete...");
                        }

                        this.handleDataFromRemote(newDataList);
                    }
                }

            } else {
                System.out.println("Command not sent: Not connected to server");
            }

        } catch (SocketTimeoutException ex) {
            System.out.println("Error: Read timed out");
            this.connectionResetError = true;
            this.connected = false;
        } catch (SocketException ex) {
            System.out.println("An error occured: Connection reset");
            this.connectionResetError = true;
            this.connected = false;
        } catch (Exception e) {
            System.out.println("An error occured: " + e);
            this.connectionResetError = true;
            this.connected = false;
        }

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
        this.inputStream = clientSocket.getInputStream();
        this.outputStream = clientSocket.getOutputStream();

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
     * Sends the given data string to the TCP server.
     *
     * @param sentence the given string
     * @return the server response
     */
    public synchronized String sendData(String sentence) {
        try {
            outToServer.println(sentence);
            //System.out.println("Data is sent...");
            outToServer.flush();
            serverResponse = inFromServer.readLine();

        } catch (Exception e) {
        }

        return serverResponse;

    }

    /**
     * Sends a ping command to the TCP server.
     *
     * @return the ping of the connection
     */
    public String ping() {
        double elapsedTimer = 0;
        double elapsedTimerNano = 0;
        long lastTime = 0;

        String ping = "<Ping:null>";
        lastTime = System.nanoTime();
        String serverResponse = sendData("ping");
        if (serverResponse.equals("<ping:true>")) {
            elapsedTimerNano = (System.nanoTime() - lastTime);
            elapsedTimer = elapsedTimerNano / 1000000;
            System.out.println("<Ping: " + elapsedTimer + ">");

            elapsedTimer = 0;
        } else {

            ping = "<ping:null>";
        }

        return ping;
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

    /**
     * Compare keys to control values coming in from remote, and puts the
     * correct value to correct variable in the shared resource Data class.
     *
     * @param newDataList the data list
     */
    public void handleDataFromRemote(HashMap<String, String> newDataList) {
        for (Map.Entry e : newDataList.entrySet()) {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            switch (key) {
                // From ROV RPi:
                case "Fb_wingPosSb":
                    data.setWingAngleSB(Float.parseFloat(value));
                    break;
                case "Fb_wingPosPort":
                    data.setWingAnglePort(Float.parseFloat(value));
                    break;
                case "Fb_rollAngle":
                    data.setRollAngle(Double.parseDouble(value));
                    break;
                case "Fb_pitchAngle":
                    data.setPitchAngle(Double.parseDouble(value));
                    break;
                case "Fb_depthToSeabedEcho":
                    data.setDepthBeneathRov(Double.parseDouble(value));
                    break;
                case "Fb_depthBelowTransduser":
                    data.setDepthBeneathRov(Double.parseDouble(value));
                    break;
                case "Fb_depthBeneathROV":
                    data.setDepthBeneathRov(Double.parseDouble(value));
                    break;
                case "Fb_tempElBoxFront":
                    data.setFb_tempElBoxFront(Double.parseDouble(value));
                    break;
                case "Fb_tempElBoxRear":
                    data.setFb_tempElBoxRear(Double.parseDouble(value));
                    break;
                case "Fb_ROVReady":
                    data.setRovReady(Boolean.parseBoolean(value));
                    break;
                case "ERROR_I2C":
                    data.setI2cError(Boolean.parseBoolean(value));
                    break;

                // From Camera RPi:
                case "leakAlarm":
                    if (value.equals("1")) {
                        data.setLeakStatus(true);
                    } else if (equals("0")) {
                        data.setLeakStatus(false);
                    }
                    break;
                case "depth":
                    data.setRovDepth(Double.parseDouble(value));
                    break;
                case "pressure":
                    data.setPressure(Double.parseDouble(value));
                    break;
                case "outsideTemp":
                    data.setOutsideTemp(Double.parseDouble(value));
                    break;
                case "insideTemp":
                    data.setInsideTemp(Double.parseDouble(value));
                    break;
                case "humidity":
                    data.setHumidity(Double.parseDouble(value));
                    break;
            }
        }
    }
}
