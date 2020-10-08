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

import basestation_rov.ReadSerialData;
import basestation_rov.SerialDataHandler;
import java.util.Map;

/**
 * The constructor of the ComPortFinder class.
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class ComPortFinder implements Runnable {

    private static Thread imuThread;
    private static Thread gpsThread;
    private static Thread echoSounderThread;
    private static Thread stepperArduinoThread;
    private static Thread ROVDummyThread;
    private SerialDataHandler sdh;
    private Data data;

    /**
     * The constructor of the ComPortFinder class.
     *
     * @param sdh the given SerialDataHandler
     * @param data the shared resource class Data
     */
    public ComPortFinder(SerialDataHandler sdh, Data data) {
        this.sdh = sdh;
        this.data = data;
    }

    /**
     * Runs the ComPortFinder thread. Starts the necessary threads based on the
     * com ports connected.
     */
    @Override
    public void run() {

        // Start searching for com ports:
        long timeDifference = 0;
        long lastTime = 0;
        long timeDelay = 5000;
        boolean connected = false;
        boolean foundComPort = false;
        boolean listedCom = false;

        //while (true) {
        if (!foundComPort) {
            System.out.println("Searching for com ports...");
            sdh.findComPorts();
            foundComPort = true;
        }

        if (!listedCom) {
            System.out.println("Com ports found:");

            if (data.comPortList.isEmpty()) {
                System.out.println("None");
            } else {
                for (Map.Entry e : data.comPortList.entrySet()) {
                    String comPortKey = (String) e.getKey();
                    String comPortValue = (String) e.getValue();
                    System.out.println(comPortKey + " : " + comPortValue);
                }
            }
            System.out.println("--End of com list--");
            listedCom = true;

            for (Map.Entry e : data.comPortList.entrySet()) {
                String comPortKey = (String) e.getKey();
                String comPortValue = (String) e.getValue();
                if (comPortValue.contains("IMU")) {
                    imuThread = new Thread(new ReadSerialData(data, comPortKey, 115200, comPortValue));
                    imuThread.start();
                    imuThread.setName(comPortValue);
                }
                if (comPortValue.contains("GPS")) {
                    gpsThread = new Thread(new ReadSerialData(data, comPortKey, 115200, comPortValue));
                    gpsThread.start();
                    gpsThread.setName(comPortValue);
                }
                if (comPortValue.contains("EchoSounder")) {
                    echoSounderThread = new Thread(new ReadSerialData(data, comPortKey, 4800, comPortValue));
                    echoSounderThread.start();
                    echoSounderThread.setName(comPortValue);
                }
                if (comPortValue.contains("StepperArduino")) {
                    stepperArduinoThread = new Thread(new ReadSerialData(data, comPortKey, 128000, comPortValue));
                    stepperArduinoThread.start();
                    stepperArduinoThread.setName(comPortValue);
                }
                if (comPortValue.contains("ROVDummy")) {
                    ROVDummyThread = new Thread(new ReadSerialData(data, comPortKey, 115200, comPortValue));
                    ROVDummyThread.start();
                    ROVDummyThread.setName(comPortValue);
                }
            }
        }
        //}
    }
}
