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

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Responsible for playing an audio file representing an alarm. This class is
 * needed for the GUI not to freeze while loading and playing the audio file.
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class Sounder implements Runnable {

    private boolean isAlive = false;

    /**
     * The constructor of the Sounder class.
     */
    public Sounder() {
        this.isAlive = true;
    }

    /**
     * Runs the Sounder thread. Plays the audio file.
     */
    @Override
    public void run() {
        try {
            String gongFile = "src/ntnusubsea/gui/audio/Emergency_Warning_06.wav";
            InputStream in = new FileInputStream(gongFile);
            // AudioStream audioStream = new AudioStream(in);
            // AudioPlayer.player.start(audioStream);
            Thread.sleep(15000);

        } catch (Exception e) {
            System.out.println("Error while trying to play an audio file: " + e.getMessage());
        }
        this.setAlive(false);
    }

    /**
     * Sets the state of the thread.
     *
     * @param bool the status to set
     */
    public void setAlive(boolean bool) {
        this.isAlive = bool;
    }

    /**
     * Returns the alive status of the thread
     *
     * @return the thread alive status
     */
    public boolean isAlive() {
        return this.isAlive;
    }

}
