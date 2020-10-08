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
 * Responsible for calibrating the actuators. Not currently in use/finished.
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class ActuatorCalibration implements Runnable {

    private final static String stepperEndPos = "100";
    private final static String stepperMiddlePos = "50";
    private final static String stepperStartPos = "1";
    private final static int accuracy = 4;
    private int posChangeTime = 8000;  // millisecond
    private int lastStepperPSPos = 0;
    private int lastStepperSBPos = 0;
    private boolean findingMinPS = true;
    private boolean findingMinSB = true;
    private boolean findingMaxPS = true;
    private boolean findingMaxSB = true;
    private long currentTime = 0;
    private long lastTimePS = 0;
    private long lastTimeSB = 0;
    private long PSStepperMaxToMinTime = 0;
    private long SBStepperMaxToMinTime = 0;

    //Error List
    //PS actuator
    private boolean Error_PSActuatorNotInMinPos = false;
    private boolean Error_PSActuatorNotInMaxPos = false;
    private boolean Error_PSActuatorTooSlow = false;

    //SB actuator
    private boolean Error_SBActuatorNotInMinPos = false;
    private boolean Error_SBActuatorNotInMaxPos = false;
    private boolean Error_SBActuatorTooSlow = false;

    private Data data;
    private TCPClient client_ROV;

    /**
     * The constructor of the ActuatorCalibration class.
     *
     * @param data the shared resource Data class
     * @param client_ROV The ROV TCP client
     */
    public ActuatorCalibration(Data data, TCPClient client_ROV) {
        this.data = data;
        this.client_ROV = client_ROV;
    }

    /**
     * Runs the ActuatorCalibration thread.
     */
    @Override
    public void run() {
        calibrateMinPos();
        speedFromMinToMax();
    }

    /**
     * Calibrates the minimum position of the actuator.
     */
    private void calibrateMinPos() {
        try {
            client_ROV.sendCommand("<cmd_stepperPS:" + stepperStartPos + ">");
            client_ROV.sendCommand("<cmd_stepperSB:" + stepperStartPos + ">");
            boolean waitingForPSPosChange = true;
            boolean waitingForSBPosChange = true;
            lastTimePS = System.currentTimeMillis();
            lastTimeSB = System.currentTimeMillis();
            while (waitingForPSPosChange || waitingForSBPosChange) {
                if (System.currentTimeMillis() - lastTimePS >= posChangeTime || !waitingForPSPosChange) {
                    client_ROV.sendCommand("<cmd_stepperPS:" + data.getFb_stepperPSPos() + ">");
                    System.out.println("Error: PS_Stepper did not reach min pos in time");
                    waitingForPSPosChange = false;
                    Error_PSActuatorNotInMinPos = true;
                }
                if (data.getFb_stepperPSPos() < 3) {
                    System.out.println("PS stepper in position");
                    waitingForPSPosChange = false;
                }

                if (System.currentTimeMillis() - lastTimeSB >= posChangeTime || !waitingForPSPosChange) {
                    client_ROV.sendCommand("<cmd_stepperSB:" + data.getFb_stepperSBPos() + ">");
                    System.out.println("Error: SB_Stepper did not reach min pos in time");
                    waitingForSBPosChange = false;
                    Error_PSActuatorNotInMinPos = true;
                }
                if (data.getFb_stepperSBPos() < 3) {
                    System.out.println("SB stepper in position");
                    waitingForSBPosChange = false;
                }
            }

        } catch (Exception e) {
        }
    }

    /**
     * Finds the speed from minimum to maximum position.
     */
    private void speedFromMinToMax() {
        try {

            client_ROV.sendCommand("<cmd_stepperPS:" + stepperEndPos + ">");
            client_ROV.sendCommand("<cmd_stepperSB:" + stepperEndPos + ">");

            boolean waitingForPSPosChange = true;
            boolean waitingForSBPosChange = true;
            lastTimePS = System.currentTimeMillis();
            lastTimeSB = System.currentTimeMillis();
            while (waitingForPSPosChange || waitingForSBPosChange) {
                if (System.currentTimeMillis() - lastTimePS >= posChangeTime || !waitingForPSPosChange) {
                    client_ROV.sendCommand("<cmd_stepperPS:" + data.getFb_stepperPSPos() + ">");
                    System.out.println("Error: PS_Stepper did not reach max pos in time");
                    waitingForPSPosChange = false;
                    Error_PSActuatorNotInMaxPos = true;
                }
                if (data.getFb_stepperPSPos() > 250) {
                    data.setPSStepperMaxToMinTime(System.currentTimeMillis() - lastTimePS);
                    System.out.println("PS stepper in position");
                    waitingForPSPosChange = false;
                }

                if (System.currentTimeMillis() - lastTimeSB >= posChangeTime || !waitingForSBPosChange) {
                    client_ROV.sendCommand("<cmd_stepperSB:" + data.getFb_stepperSBPos() + ">");
                    System.out.println("Error: SB_Stepper did not reach max pos in time");
                    waitingForSBPosChange = false;
                    Error_SBActuatorNotInMaxPos = true;
                }
                if (data.getFb_stepperSBPos() > 250) {
                    data.setSBStepperMaxToMinTime(System.currentTimeMillis() - lastTimeSB);
                    System.out.println("SB stepper in position");
                    waitingForPSPosChange = false;
                }
            }
        } catch (Exception e) {
        }
    }
}
