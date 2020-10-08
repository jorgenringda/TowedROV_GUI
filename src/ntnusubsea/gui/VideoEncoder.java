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
import java.io.File;
import java.time.LocalDateTime;
import java.util.Observable;
import java.util.Observer;
import org.jcodec.api.awt.AWTSequenceEncoder;

/**
 * The class video encoder uses buffered image and encodes there images to a
 * video file.
 *@author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class VideoEncoder implements Runnable, Observer {
// private ArrayList<BufferedImage> list = new ArrayList();

    private BufferedImage videoImage;
    private Data data;
    private AWTSequenceEncoder enc;
    private int frame = 0;
    private LocalDateTime startTime;

    /**
     * Creates an instance of video encoder and create the MP4 file and gives it
     * a unique name
     *
     * @param data Data containing the images
     */
    public VideoEncoder(Data data) {
        try {
            startTime = LocalDateTime.now();
            String minute, hour, day, month, year;
            if (startTime.getMinute() < 10) {
                minute = "0" + Integer.toString(startTime.getMinute());
            } else {
                minute = Integer.toString(startTime.getMinute());
            }
            if (startTime.getHour() < 10) {
                hour = "0" + Integer.toString(startTime.getHour());
            } else {
                hour = Integer.toString(startTime.getHour());
            }
            if (startTime.getDayOfMonth() < 10) {
                day = "0" + Integer.toString(startTime.getDayOfMonth());
            } else {
                day = Integer.toString(startTime.getDayOfMonth());
            }
            if (startTime.getMonthValue() < 10) {
                month = "0" + Integer.toString(startTime.getMonthValue());
            } else {
                month = Integer.toString(startTime.getMonthValue());
            }
            year = Integer.toString(startTime.getYear());

            File dir = null;
            try {
                dir = new File("C:\\TowedROV\\ROV_Video\\");
                if (!dir.exists() || !dir.isDirectory()) {
                    System.out.println("No directory found, creating a new one at C://TowedROV/ROV_Video/");
                    dir.mkdir();
                }
            } catch (Exception e) {
                System.out.println("No directory found, creating a new one at C://TowedROV/ROV_Video/");
                dir.mkdir();
            }

            String fileName = dir.getPath() + "\\ROV Video" + hour + minute + " " + day + "."
                    + month + "." + year + ".mp4";
            enc = AWTSequenceEncoder.create24Fps(new File(fileName));
            this.data = data;
        } catch (Exception ex) {
            System.out.println("Exception while starting video encoder: " + ex.getMessage());
        }
    }

    /**
     * Runs the VideoEncoder thread and encodes each frame.
     */
    @Override
    public void run() {
        if (data.isStreaming()) {
            // if (frame < list.size()) {
            try {
                // BufferedImage image = list.get(frame);
                enc.encodeImage(videoImage);
                // frame++;
            } catch (Exception ex) {
                System.out.println("Error encoding frame: " + ex.getMessage());;
            }
        }

    }

    /**
     * Updates the video image to encode by observing the Data class.
     *
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {
        videoImage = data.getVideoImage();
    }

    /**
     * Finishes the video
     */
    public void finishVideo() {
        try {
            enc.finish();
        } catch (Exception ex) {
            System.out.println("Exception while finishing the video: " + ex.getMessage());
        }
    }
}
