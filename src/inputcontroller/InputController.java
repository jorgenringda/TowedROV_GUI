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
package InputController;

import com.exlumina.j360.Controller;
import com.exlumina.j360.ValueListener;
import com.exlumina.j360.ButtonListener;
import java.io.IOException;
import ntnusubsea.gui.Data;
import ntnusubsea.gui.TCPClient;

/**
 * This class is responsible for handling the inputs from the Xbox controller.
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class InputController implements Runnable {
    
    private double btnLyGUI = 0;
    private int btnLy = 0;
    private int btnLx = 0;
    private int btnRy = 0;
    private int btnRx = 0;
    private int angle = 0;

    private static int lastAngle = 0;
    private static float lastVal = 0;
    private int oldPS = 0;
    private int oldSB = 0;

    private String ipAddress = "";
    private int sendPort = 0;
    private Data data;
    private TCPClient client_ROV;

    /**
     * The constructor of the InputController class.
     *
     * @param data the Data object
     * @param client_ROV The ROV TCP client
     */
    public InputController(Data data, TCPClient client_ROV) {
        this.data = data;
        this.client_ROV = client_ROV;
    }

    /**
     * Runs the InputController thread. Sends the input values to the ROV TCP
     * client to run the actuators.
     *
     */
    @Override
    public void run() {
        ValueListener Ly = new LeftThumbYListener(this);
        //ValueListener Lx = new LeftThumbXListener(this);
        //ValueListener Ry = new RightThumbYListener(this);
        ValueListener Rx = new RightThumbXListener(this);
        
        Controller c1 = Controller.C1;

        c1.leftThumbY.addValueChangedListener(Ly);
        //c1.leftThumbX.addValueChangedListener(Lx);
        //c1.rightThumbY.addValueChangedListener(Ry);
        c1.rightThumbX.addValueChangedListener(Rx);
        

        for (;;) {
            try {
                if (data.isControllerEnabled() && data.isManualMode()) {
//                    int ps = 0;
//                    int sb = 0;
//                    if (btnLy <= 127)
//                    {
//                        ps = btnLy + btnRx;
//                        sb = btnLy - btnRx;
//                    }
//                    if (btnLy > 127)
//                    {
//                        ps = btnLy - (btnRx);
//                        sb = btnLy;
//                    }

                    int ps = btnLy;
                    int sb = btnLy;
                    if (ps != oldPS) {
                        this.client_ROV.sendCommand("cmd_actuatorPS:" + String.valueOf(ps));
                        oldPS = ps;
                    }
                    if (sb != oldSB) {
                        this.client_ROV.sendCommand("cmd_actuatorSB:" + String.valueOf(sb));
                        oldSB = sb;
                    }

                }
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                System.out.println("InterruptedException: " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        }
    }

    /**
     * Calculates the angle by the given x and y point.
     *
     * @param x given x point
     * @param y given y point
     * @return the angle by the given x and y point.
     */
    public static int FindDegree(int x, int y) {
        float value = (float) ((Math.atan2(x, y) / Math.PI) * 180f);
        if ((x < 98 && x > -98) && (y < 98 && y > -98)) {
            value = lastVal;
        } else if (value < 0) {
            value += 360f;
        }
        lastVal = value;
        return Math.round(value);
    }

    /**
     * Returns the y value of the left thumb stick
     *
     * @return the y value of the left thumb stick
     */
    public int getBtnLy() {
        return btnLy;
    }

    /**
     * Sets the y value of the left thumb stick
     *
     * @param btnLy the y value of the left thumb stick
     */
    public void setBtnLy(int btnLy) {
        this.btnLy = btnLy;
        this.btnLyGUI = (double) (this.btnLy / 100.0);
        //System.out.println("L_Y: " + btnLy);
    }

    /**
     * Returns the x value of the left thumb stick
     *
     * @return the x value of the left thumb stick
     */
    public int getBtnLx() {
        return btnLx;
    }

    /**
     * Sets the x value of the left thumb stick
     *
     * @param btnLx
     */
    public void setBtnLx(int btnLx) {
        this.btnLx = btnLx;
    }

    /**
     * Returns the y value of the right thumb stick
     *
     * @return the y value of the right thumb stick
     */
    public int getBtnRy() {
        return btnRy;
    }

    /**
     * Sets the y value of the right thumb stick
     *
     * @param btnRy the y value of the right thumb stick
     */
    public void setBtnRy(int btnRy) {
        this.btnRy = btnRy;
    }

    /**
     * Returns the x value of the right thumb stick
     *
     * @return the x value of the right thumb stick
     */
    public int getBtnRx() {
        return btnRx;
    }

    /**
     * Sets the x value of the right thumb stick
     *
     * @param btnRx the x value of the right thumb stick
     */
    public void setBtnRx(int btnRx) {
        //System.out.println("R_X: " + btnRx);
        this.btnRx = btnRx;
    }

    /**
     * Returns the angle of the right thumb stick x and y values
     *
     * @return the angle of the right thumb stick x and y values
     */
    public int getAngle() {
        this.angle = this.FindDegree(btnRx, btnRy);
        if (this.angle != this.lastAngle) {
            //System.out.println(angle);
            this.lastAngle = this.angle;
        }
        return this.angle;
    }

    /**
     * Returns the angle of the right thumb stick x and y values for the GUI
     *
     * @return the angle of the right thumb stick x and y values for the GUI
     */
    public int getAngleForGUI() {
        this.angle = this.FindDegree(btnRx, btnRy);
        if (this.angle != this.lastAngle) {
            //System.out.println(angle);
            this.lastAngle = this.angle;
        }
        int returnAngle = this.angle - 15;
        if (returnAngle < 0) {
            returnAngle = returnAngle + 360;
        }
        return returnAngle;
    }

}
