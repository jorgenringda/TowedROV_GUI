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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

/**
 * This class handles incoming images from a DatagramPacket. It receives the
 * image on a DatagramSocket and returns it as a BufferedImage.
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class UDPServer implements Runnable {

    static BufferedImage videoImage;
    //static Socket videoSocket;
    private int photoNumber = 1;
    boolean lastPhotoMode = false;
    private boolean debug = false;
    private Data data;
    private int test = 0;
    //private String IP;
    private int port;
    private DatagramSocket videoSocket;
    private long timer = System.currentTimeMillis();
    private File photoDirectory;
    private DatagramPacket receivePacket;
    private InetAddress returnIP;
    private int returnPort;
    private boolean connected = false;
    private double endTime;
    private double startTime;

    /**
     * |
     * The constructor of the UDPServer class. Sets up a DatagramSocket at the
     * given port.
     *
     * @param port the given port
     * @param data the shared resource class Data
     */
    public UDPServer(int port, Data data) {
        try {
            this.data = data;
            this.port = port;
            //this.IP = IP;
            videoSocket = new DatagramSocket(this.port);
            Date today = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
            String time = dateFormat.format(today);
            photoDirectory = new File("D://TowedRovPicture/" + time + "/");
        } catch (Exception e) {
            System.out.println("Error setting up UDP server: " + e.getMessage());
        }
    }

    /**
     * Sends the photo mode delay value to the UDP client
     */
    public void sendDelayCommand() {
        try {
            String message = "photoDelay:" + String.valueOf(data.getPhotoModeDelay());
            byte arr[] = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(arr, arr.length, this.returnIP, this.returnPort);
            videoSocket.send(sendPacket);
            System.out.println("Delay command sent to Camera RPi!");

        } catch (SocketException ex) {
            System.out.println("SocketException in UDPServer: " + ex.getMessage());

        } catch (IOException ex) {
            System.out.println("IOException in UDPServer: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("Exception in UDPServer: " + ex.getMessage());
        }
    }

    /**
     * Sends the reset image number command to the UDP client
     */
    public void sendResetIMGcommand() {
        try {
            String message = "resetImgNumber";
            byte arr[] = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(arr, arr.length, this.returnIP, this.returnPort);
            videoSocket.send(sendPacket);
            System.out.println("resetImgNumber command sent to Camera RPi!");

        } catch (SocketException ex) {
            System.out.println("SocketException in UDPServer: " + ex.getMessage());

        } catch (IOException ex) {
            System.out.println("IOException in UDPServer: " + ex.getMessage());
        } catch (Exception ex) {
            System.out.println("Exception in UDPServer: " + ex.getMessage());
        }
    }

    /**
     * Runs the UDPServer thread. Receives the image frames from the video
     * stream.
     */
    @Override
    public void run() {
        try {
            if (System.currentTimeMillis() - timer > 60000) {
                videoSocket.close();
                videoSocket = new DatagramSocket(this.port);
                timer = System.currentTimeMillis();
                //System.out.println("Reconnected");
                this.connected = true;
                this.data.setStreaming(true);
            }

            //Creates new DatagramSocket for reciving DatagramPackets
            //Creating new DatagramPacket form the packet recived on the videoSocket
            byte[] receivedData = new byte[60000];
            receivePacket = new DatagramPacket(receivedData,
                    receivedData.length);
            this.connected = true;
            this.data.setStreaming(true);
            if (receivePacket.getLength() > 0) {
                startTime = System.currentTimeMillis();
                //Updates the videoImage from the received DatagramPacket
                videoSocket.receive(receivePacket);
                this.returnIP = receivePacket.getAddress();
                this.returnPort = receivePacket.getPort();
                endTime = System.currentTimeMillis();
                data.setPhotoModeDelay_FB((endTime - startTime) / 1000);
                if (debug) {
                    System.out.println("Videopackage received");
                }
                //Reads incomming byte array into a BufferedImage
                ByteArrayInputStream bais = new ByteArrayInputStream(receivedData);
                videoImage = ImageIO.read(bais);
                data.setVideoImage(videoImage);

                if (lastPhotoMode && (endTime - startTime) > 500) {
                    data.increaseImageNumberByOne();
                }

                // Saves the photo to disk if photo mode is true
                if (data.isPhotoMode()) {
                    try {
                        if (this.photoDirectory.exists() && this.photoDirectory.isDirectory()) {
                            Date now = new Date();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("HH_mm_ss");
                            String time2 = dateFormat.format(now);
                            ImageIO.write(videoImage, "jpg", new File(this.photoDirectory.toString() + "/image_" + this.photoNumber + "_Time_" + time2 + ".png"));
                            this.photoNumber++;
                        } else {
                            System.out.println("No directory found, creating a new one at C://TowedROV/ROV_Photos/");
                            this.photoDirectory.mkdir();
                        }

                    } catch (Exception e) {
                        System.out.println("Exception occured :" + e.getMessage());
                    }
                    System.out.println("Image were saved to disk succesfully at C://TowedROV/ROV_Photos");
                }
                // Sends the command to the ROV
                if (data.isPhotoMode() != lastPhotoMode) {
                    String message = "photoMode:" + String.valueOf(data.isPhotoMode());
                    byte arr[] = message.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(arr, arr.length, this.returnIP, this.returnPort);
                    videoSocket.send(sendPacket);
                    lastPhotoMode = data.isPhotoMode();
                }

                receivedData = null;
                bais = null;
                test++;
                //System.out.println(endTime - startTime);
            }

        } catch (SocketException sex) {
            System.out.println("SocketException: " + sex.getMessage());
            this.connected = false;
            this.data.setStreaming(false);

        } catch (IOException ioex) {
            System.out.println("IOException: " + ioex.getMessage());
            this.connected = false;
            this.data.setStreaming(false);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            this.connected = false;
            this.data.setStreaming(false);
        }
    }
}
