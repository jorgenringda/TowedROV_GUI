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

import com.exlumina.j360.ValueListener;

/**
 * This class is responsible for handling the input from the X axis of the right
 * stick on the Xbox 360 controller.
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
class RightThumbXListener implements ValueListener {

    private InputController ic;

    /**
     * The constructor of the RightThumbXListener class
     *
     * @param ic the InputController
     */
    public RightThumbXListener(InputController ic) {
        this.ic = ic;
    }

    /**
     * Sets the new value from the Xbox controller
     *
     * @param newValue the new value
     */
    @Override
    public void value(int newValue) {
        newValue = map(newValue, -32768, 32768, 126, -126);
        if (newValue > -17 && newValue < 12) {
            newValue = 0;
        }
        this.ic.setBtnRx(newValue);
        System.out.printf("Rx: " + "%6d\n", newValue);
    }

    /**
     * Maps the given value range to the given output range
     *
     * @param x the value to be mapped
     * @param in_min the minimum value of the input range
     * @param in_max the maximum value of the input range
     * @param out_min the minimum value of the output range
     * @param out_max the maximum value of the output range
     * @return the mapped value
     */
    private int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }
}
