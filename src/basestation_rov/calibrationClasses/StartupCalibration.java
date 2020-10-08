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
package basestation_rov.calibrationClasses;

import ntnusubsea.gui.*;

/**
 * Responsible for calibrating the necessary equipment at start-up. Not
 * finished.
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class StartupCalibration {

    private static Thread actuatorCalibrationThread;
    Data data = null;
    TCPClient client_ROV = null;
    TCPClient client_Camera = null;
    long currentTime = 0;
    long lastTimePS = 0;
    long lastTimeSB = 0;

    /**
     * The constructor of the StartupCalibration class.
     *
     * @param data the shared resource Data class
     * @param client_ROV the ROV TCP client
     */
    public StartupCalibration(Data data, TCPClient client_ROV) {
        this.data = data;
        this.client_ROV = client_ROV;
        this.client_Camera = client_Camera;

    }

    /**
     * Calibrates all the necessary equipment.
     *
     * @return a message saying the calibration was completed.
     */
    public String doStartupCalibration() {
        calibrateActuators();
        //testLights();

        return "Calibration complete...";
    }

    /**
     * Sets up the com ports
     */
    public void setupComPorts() {

    }

    /**
     * Calibrates the actuators
     */
    public void calibrateActuators() {
        actuatorCalibrationThread = new Thread(new ActuatorCalibration(data, client_ROV));
        actuatorCalibrationThread.start();
        actuatorCalibrationThread.setName("actuatorCalibrationThread");
    }

//    Test lights
//    public void testLights()
//    {
//        boolean testingLights = true;
//        long lastTime = System.nanoTime();
//        int lightIntensity = 1;
//        while (testingLights && lightIntensity < 100)
//        {
//            if (System.nanoTime() - lastTime >= 250000000)
//            {
//                dh.cmd_lightIntensity = lightIntensity + 1;
//                lastTime = System.nanoTime();
//            }
//        }
//        try
//        {
//            Thread.sleep(5000);
//            dh.cmd_lightIntensity = 0;
//        } catch (Exception e)
//        {
//        }
//    }
//
//Calibrate depth sensor here:
}
