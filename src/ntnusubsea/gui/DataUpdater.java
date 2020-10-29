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

import java.io.IOException;

/**
 * This class updates all of the data from the RPis in the ROV by using their
 * respective TCP clients. It also sends the value from the echo sounder onboard
 * the boat to the ROV.
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class DataUpdater implements Runnable {

    private TCPClient client_Rov;
    private TCPClient client_Camera;
    private Data data;

    /**
     * Creates an instance of the DataUpdater class.
     *
     * @param client_Rov The ROV TCP client
     * @param client_Camera The camera TCP client
     * @param data The shared resource Data object
     */
    public DataUpdater(TCPClient client_Rov, TCPClient client_Camera, Data data) {
        this.client_Rov = client_Rov;
        this.client_Camera = client_Camera;
        this.data = data;
    }

    /**
     * Runs the DataUpdater thread and sends the "get data" commands to the TCP
     * servers on the main RPi and the camera RPi. It also sends the echo
     * sounder depth value to the main RPi.
     */
    @Override
    public void run() {
        if (client_Rov.isConnected()) {
            try {
                client_Rov.sendCommand("fb_allData");
//                if (!data.comPortList.containsKey("ROVDummy")
//                        && !data.comPortList.containsValue("ROVDummy")) {
//                    client_Rov.sendCommand("cmd_rovDepth:" + data.getRovDepth());
//                } else {
//                    client_Rov.sendCommand("cmd_rovDepth:" + data.getTestDepth());
//                }
            } catch (IOException ex) {
                System.out.println("Error while getting data from remote: " + ex.getMessage());
            }

        }
        if (client_Camera.isConnected()) {
            try {
                client_Camera.sendCommand("getData");
                System.out.println("camcommandsent");
            } catch (IOException ex) {
                System.out.println("Error while getting data from remote: " + ex.getMessage());
            }

        }

    }

}
