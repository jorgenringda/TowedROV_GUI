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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 * This class is an extended version of JPanel, with the added methods required
 * to display BufferedImages in the panel.
 *@author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class ImagePanel extends JPanel {

    private BufferedImage image;
    private int width, height;

    /**
     * Constructor used to create the Sheet object
     *
     * @param width The width of the sheet
     * @param height The height of the sheet
     */
    public ImagePanel(int width, int height) {
        setSize(width, height);
    }

    /**
     * This methods updates the sheet to display the image used as a input
     * parameter
     *
     * @param img Image to display in the component
     */
    public void paintSheet(BufferedImage img) {
        image = null;
        image = img;
        repaint();
    }

    /**
     * Uses the the paintComponent method of the super class and makes the
     * component compatible with bufferedImage
     *
     * @param g A graphics context onto which a bufferedImage can be drawn
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, this);
    }
}
