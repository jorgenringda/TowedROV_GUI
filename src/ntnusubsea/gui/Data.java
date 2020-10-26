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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * The data class is a storage box that let's the different threads change and
 * retrieve various data. The data class is a subclass of the java class
 * Observable, which makes it possible for observers to subscribe and update
 * their values whenever they change.
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356 edited 2020, added
 * get and set method for stepper position on starboard and portside
 */
public final class Data extends Observable {

    public HashMap<String, String> comPortList = new HashMap<>();
    public ConcurrentHashMap<String, Boolean> completeAlarmListDh = new ConcurrentHashMap<>();

    //------------------------
    //Do not change the times, this is measured movement time without oil
    private static final long actuatorPSInitialMovementTime = 8000;
    private static final long actuatorSBInitialMovementTime = 8000;
    //------------------------
    private static final int actuatorTolerableSpeedLoss = 10; //Percent
    private long PSStepperMaxToMinTime;
    private long SBStepperMaxToMinTime;

    private int arduinoBaudRate = 115200;
    private byte[] dataFromArduino = new byte[11];
    private boolean dataFromArduinoAvailable = false;
    private byte requestCodeFromArduino;
    private boolean threadStatus = true;
    private boolean dataUpdated = false;
    private boolean controllerEnabled = false;

    //Dummy signals
    public double TestDepth = 0;

    // Feedback from GPS
    public int satellites = 0;
    public float altitude = 0;
    public double gpsAngle = 0;
    public float speed = 0;
    public float latitude = (float) 0;
    public float longitude = (float) 0;
    public float depth = (float) 0.01;
    public float temperature = (float) 0.01;
    public double voltage = 0.01;

    // Feedback from IMU
    public double roll = 0.00;
    public double pitch = 0.00;
    public float heading = 100;

    // Feedback from the Camera RPi
    private boolean leakStatus = false;
    private double rovDepth = -0.00;
    private double pressure = 0.00;
    private double outsideTemp = 0.00;
    private double insideTemp = 0.00;
    private double humidity = 0.00;

    // Feedback from ROV
    private Double rovPing = 999.99;
    private boolean rovReady = false;
    private boolean i2cError;
    private int fb_stepperPSPos;
    private int fb_stepperSBPos;
    private int fb_actuatorPScmd;
    private int fb_actuatorSBcmd;

    private int fb_actuatorPSMinPos;
    private int fb_actuatorSBMinPos;
    private int fb_actuatorPSMaxPos;
    private int fb_actuatorSBMaxPos;
    private double fb_tempElBoxFront;
    private double fb_tempElBoxRear;

    // Feedback from GUI
    public boolean startLogging = true;
    public ConcurrentHashMap<String, String> data = new ConcurrentHashMap<>();
    public List<String> rovDepthDataList = new ArrayList<>();
    public List<String> depthBeneathBoatDataList = new ArrayList<>();

    private double timeBetweenBoatAndRov = 4.0;
    private double depthBeneathRov = 0;
    private double depthBeneathBoat = 0;
    private double pitchAngle = 0;
    private float wingAngle = 0;
    private double rollAngle = 0;
    private float channel1 = 0;
    private float channel2 = 0;
    private float channel3 = 0;
    private float channel4 = 0;
    private float channel5 = 0;
    private float channel6 = 0;
    private float channel7 = 0;
    private float channel8 = 0;
    private float channel9 = 0;
    private byte actuatorStatus = 0;
    private ArrayList<String> labels = new ArrayList();
    private float[] channelValues = new float[9];
    private String IP_Rov = "";
    private String IP_Camera = "";
    private BufferedImage videoImage;
    private String KpDepth = "1";
    private String KiDepth = "2";
    private String KdDepth = "3";
    private String KpSeaFloor = "1";
    private String KiSeaFloor = "2";
    private String KdSeaFloor = "3";
    private String KpTrim = "1";
    private String KiTrim = "2";
    private String KdTrim = "3";
    private String offsetDepthBeneathROV = "0.00";
    private String offsetROVdepth = "0.00";
    private long timer = System.currentTimeMillis();
    private boolean photoMode = false;
    private double photoModeDelay = 1.00;
    private double photoModeDelay_FB = 1.00;
    private int imageNumber = 0;
    private boolean imagesCleared = false;
    private int cameraPitchValue = 0;
    private boolean doRovCalibration = false;
    private boolean emergencyMode = false;
    private boolean streaming = false;
    private boolean manualMode = false;
    private boolean kalmanOnOff = false;
    private float wingAnglePort = 0;
    private float wingAngleSB = 0;

    /**
     * Creates an object of the class Data.
     */
    public Data() {
        try {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(new File("ROV Options.txt")));
                //this.updateRovDepthDataList();
                IP_Rov = br.readLine();
                IP_Camera = br.readLine();
                labels.add(0, br.readLine());
                labels.add(1, br.readLine());
                labels.add(2, br.readLine());
                labels.add(3, br.readLine());
                labels.add(4, br.readLine());
                labels.add(5, br.readLine());
                labels.add(6, br.readLine());
                labels.add(7, br.readLine());
                this.setKpDepth(br.readLine());
                this.setKiDepth(br.readLine());
                this.setKdDepth(br.readLine());
                this.setKpSeaFloor(br.readLine());
                this.setKiSeaFloor(br.readLine());
                this.setKdSeaFloor(br.readLine());
                this.setKpTrim(br.readLine());
                this.setKiTrim(br.readLine());
                this.setKdTrim(br.readLine());
                this.setOffsetDepthBeneathROV(br.readLine());
                this.setOffsetROVdepth(br.readLine());
            } catch (Exception e) {
                System.out.println("Error getting the ROV Options.txt file.");
                IP_Rov = "0";
                IP_Camera = "0";
                labels.add(0, "1");
                labels.add(1, "2");
                labels.add(2, "3");
                labels.add(3, "4");
                labels.add(4, "5");
                labels.add(5, "6");
                labels.add(6, "7");
                labels.add(7, "8");
                this.setKpDepth("0");
                this.setKiDepth("0");
                this.setKdDepth("0");
                this.setKpSeaFloor("0");
                this.setKiSeaFloor("0");
                this.setKdSeaFloor("0");
                this.setKpTrim("0");
                this.setKiTrim("0");
                this.setKdTrim("0");
                this.setOffsetDepthBeneathROV("0");
                this.setOffsetROVdepth("0");
            }

            channelValues[0] = channel1;
            channelValues[1] = channel2;
            channelValues[2] = channel3;
            channelValues[3] = channel4;
            channelValues[4] = channel5;
            channelValues[5] = channel6;
            channelValues[6] = channel7;
            channelValues[7] = channel8;
            channelValues[8] = channel9;
            videoImage = ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/TowedROV.jpg"));
        } catch (IOException ex) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Sets the default ROV IP
     *
     * @param ip The default ROV IP
     */
    public synchronized void setIP_Rov(String ip) {
        this.IP_Rov = ip;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the default ROV IP
     *
     * @return The default ROV IP
     */
    public synchronized String getIP_Rov() {
        return IP_Rov;
    }

    /**
     * Sets the default Camera IP
     *
     * @param ip The default Camera IP
     */
    public synchronized void setIP_Camera(String ip) {
        this.IP_Camera = ip;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the default Camera IP
     *
     * @return The default Camera IP
     */
    public synchronized String getIP_Camera() {
        return IP_Camera;
    }

    /**
     * Sets the KpDepth parameter of the PID
     *
     * @param value the KpDepth parameter of the PID
     */
    public void setKpDepth(String value) {
        this.KpDepth = value;
    }

    /**
     * Returns the KpDepth parameter of the PID
     *
     * @return the KpDepth parameter of the PID
     */
    public synchronized String getKpDepth() {
        return KpDepth;
    }

    /**
     * Sets the KiDepth parameter of the PID
     *
     * @param value the KiDepth parameter of the PID
     */
    public void setKiDepth(String value) {
        this.KiDepth = value;
    }

    /**
     * Returns the KiDepth parameter of the PID
     *
     * @return the KiDepth parameter of the PID
     */
    public synchronized String getKiDepth() {
        return KiDepth;
    }

    /**
     * Sets the KdDepth parameter of the PID
     *
     * @param value the KdDepth parameter of the PID
     */
    public void setKdDepth(String value) {
        this.KdDepth = value;
    }

    /**
     * Returns the KdDepth parameter of the PID
     *
     * @return the KdDepth parameter of the PID
     */
    public synchronized String getKdDepth() {
        return KdDepth;
    }

    /**
     * Sets the KpSeaFloor parameter of the PID
     *
     * @param value the KpSeaFloor parameter of the PID
     */
    public void setKpSeaFloor(String value) {
        this.KpSeaFloor = value;
    }

    /**
     * Returns the KpSeaFloor parameter of the PID
     *
     * @return the KpSeaFloor parameter of the PID
     */
    public synchronized String getKpSeaFloor() {
        return KpSeaFloor;
    }

    /**
     * Sets the KiSeaFloor parameter of the PID
     *
     * @param value the KiSeaFloor parameter of the PID
     */
    public void setKiSeaFloor(String value) {
        this.KiSeaFloor = value;
    }

    /**
     * Returns the KiSeaFloor parameter of the PID
     *
     * @return the KiSeaFloor parameter of the PID
     */
    public synchronized String getKiSeaFloor() {
        return KiSeaFloor;
    }

    /**
     * Sets the KdSeaFloor parameter of the PID
     *
     * @param value the KdSeaFloor parameter of the PID
     */
    public void setKdSeaFloor(String value) {
        this.KdSeaFloor = value;
    }

    /**
     * Returns the KdSeaFloor parameter of the PID
     *
     * @return the KdSeaFloor parameter of the PID
     */
    public synchronized String getKdSeaFloor() {
        return KdSeaFloor;
    }

    public void setKpTrim(String value) {
        this.KpTrim = value;
    }

    /**
     * Returns the KpDepth parameter of the PID
     *
     * @return the KpDepth parameter of the PID
     */
    public synchronized String getKpTrim() {
        return KpTrim;
    }

    public void setKiTrim(String value) {
        this.KiTrim = value;
    }

    /**
     * Returns the KpDepth parameter of the PID
     *
     * @return the KpDepth parameter of the PID
     */
    public synchronized String getKiTrim() {
        return KiTrim;
    }

    public void setKdTrim(String value) {
        this.KdTrim = value;
    }

    /**
     * Returns the KpDepth parameter of the PID
     *
     * @return the KpDepth parameter of the PID
     */
    public synchronized String getKdTrim() {
        return KdTrim;
    }

    public void setKalmanOnOff(boolean value) {
        this.kalmanOnOff = value;
    }

    public boolean getKalmanOnOff() {
        return kalmanOnOff;
    }

    /**
     * Returns the depth beneath the ROV offset
     *
     * @return the depth beneath the ROV offset
     */
    public String getOffsetDepthBeneathROV() {
        return offsetDepthBeneathROV;
    }

    /**
     * Sets the depth beneath the ROV offset
     *
     * @param offsetDepthBeneathROV the depth beneath the ROV offset
     */
    public void setOffsetDepthBeneathROV(String offsetDepthBeneathROV) {
        this.offsetDepthBeneathROV = offsetDepthBeneathROV;
    }

    /**
     * Returns the ROV depth offset
     *
     * @return the ROV depth offset
     */
    public String getOffsetROVdepth() {
        return offsetROVdepth;
    }

    /**
     * Sets the ROV depth offset
     *
     * @param offsetROVdepth the ROV offset
     */
    public void setOffsetROVdepth(String offsetROVdepth) {
        this.offsetROVdepth = offsetROVdepth;
    }

    /**
     * Sets the label of all the different I/O channels and notifies observers
     *
     * @param c1 Channel 1 label
     * @param c2 Channel 2 label
     * @param c3 Channel 3 label
     * @param c4 Channel 4 label
     * @param c5 Channel 5 label
     * @param c6 Channel 6 label
     * @param c7 Channel 7 label
     * @param c8 Channel 8 label
     */
    public synchronized void setIOLabels(String c1, String c2, String c3, String c4, String c5, String c6, String c7, String c8) {
        labels.set(0, c1);
        labels.set(1, c2);
        labels.set(2, c3);
        labels.set(3, c4);
        labels.set(4, c5);
        labels.set(5, c6);
        labels.set(6, c7);
        labels.set(7, c8);
        setChanged();
        notifyObservers();
    }

    /**
     * Returns a string containing the label of the channel. If the channel is
     * an input, the string also contains its measure value. (Index 1-8)
     *
     * @param channel Index of channel to return
     * @return String containing label and value
     */
    public synchronized String getChannel(int channel) {
        if (channel > 0 && channel < 9) {
            if (channel < 5) {
                String channelString = labels.get(channel - 1) + ": ";
                channelString += channelValues[channel - 1];
                return channelString;
            } else {
                return labels.get(channel - 1);
            }
        } else {
            return null;
        }
    }

    /**
     * Returns the value of the channel as a float. (Index 1-4)
     *
     * @param channel Index of channel
     * @return Value of the channel as float
     */
    public synchronized float getChannelValue(int channel) {
        if (channel < 0 && channel > 5) {
            return channelValues[channel - 1];
        } else {
            return (float) 0.001;
        }
    }

    /**
     * Sets the value of one of the inputs and notifies observers (Index 1-4).
     *
     * @param value Value of the channel
     * @param channel Index of the channel
     */
    public synchronized void setChannel(float value, int channel) {
        if (channel < 0 && channel > 5) {
            channelValues[channel - 1] = value;
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Updates the current pitch angle of the ROV and notifies observers
     *
     * @param angle Current pitch angle of the ROV
     */
    public synchronized void setPitchAngle(double angle) {
        pitchAngle = angle;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current pitch angle
     *
     * @return Current pitch angle of the ROV
     */
    public synchronized double getPitchAngle() {
        return pitchAngle;
    }

    /**
     * Updates the current roll angle of the ROV
     *
     * @param angle Current roll angle of the ROV
     */
    public synchronized void setRollAngle(double angle) {
        rollAngle = angle;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current roll angle
     *
     * @return Current roll angle of the ROV
     */
    public synchronized double getRollAngle() {
        return rollAngle;
    }

    /**
     * Updates the current wing angle of the ROV and notifies observers
     *
     * @param angle Current wing angle of the ROV
     */
    public synchronized void setWingAngle(float angle) {
        wingAngle = angle;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current pitch angle
     *
     * @return Current wing angle of the ROV
     */
    public synchronized float getWingAngle() {
        return wingAngle;
    }

    /**
     * Updates the current wing angle of the ROV and notifies observers
     *
     * @param angle Current wing angle of the ROV
     */
    public synchronized void setWingAnglePort(float angle) {
        wingAnglePort = angle;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current pitch angle
     *
     * @return Current wing angle of the ROV
     */
    public synchronized float getWingAnglePort() {
        return wingAnglePort;
    }

    /**
     * Updates the current wing angle of the ROV and notifies observers
     *
     * @param angle Current wing angle of the ROV
     */
    public synchronized void setWingAngleSB(float angle) {
        wingAngleSB = angle;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current pitch angle
     *
     * @return Current wing angle of the ROV
     */
    public synchronized float getWingAngleSb() {
        return wingAngleSB;
    }

    /**
     * Updates the current heading of the ROV and notifies observers
     *
     * @param heading Current heading of the ROV
     */
    public synchronized void setHeading(float heading) {
        this.heading = heading;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current heading
     *
     * @return Current heading of the ROV
     */
    public synchronized float getHeading() {
        return heading;
    }

    /**
     * Updates the current latitude of the ROV and notifies observers
     *
     * @param latitude Current latitude of the ROV
     */
    public synchronized void setLatitude(float latitude) {
        this.latitude = latitude;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current latitude
     *
     * @return Current latitude of the ROV
     */
    public synchronized float getLatitude() {
        return latitude;
    }

    /**
     * Updates the current longitude of the ROV and notifies observers
     *
     * @param longitude Current longitude of the ROV
     */
    public synchronized void setLongitude(float longitude) {
        this.longitude = longitude;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current longitude
     *
     * @return Current longitude of the ROV
     */
    public synchronized float getLongitude() {
        return longitude;
    }

    /**
     * Updates the current depth of the ROV and notifies observers
     *
     * @param depth Current depth of the ROV
     */
    public synchronized void setDepth(float depth) {
        this.depth = depth;
        setChanged();
        notifyObservers();
    }

    /**
     * Retrieves the current depth
     *
     * @return Current depth of the ROV
     */
    public synchronized float getDepth() {
        return depth;
    }

    /**
     * Returns the time between the boat and the ROV
     *
     * @return the time between the boat and the ROV
     */
    public double getTimeBetweenBoatAndRov() {
        return timeBetweenBoatAndRov;
    }

    /**
     * Sets the time between the boat and the ROV
     *
     * @param timeBetweenBoatAndRov the time between the boat and the ROV
     */
    public void setTimeBetweenBoatAndRov(double timeBetweenBoatAndRov) {
        this.timeBetweenBoatAndRov = timeBetweenBoatAndRov;
    }

    /**
     * Updates the current depth beneath the ROV and notifies observers
     *
     * @param depth Depth beneath the ROV
     */
    public synchronized void setDepthBeneathRov(double depth) {
        depthBeneathRov = depth;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the depth beneath the ROV
     *
     * @return Depth beneath the ROV
     */
    public synchronized double getDepthBeneathRov() {
        return depthBeneathRov;
    }

    /**
     * Updates the current depth beneath the vessel and notifies observers
     *
     * @param depth Depth beneath the vessel
     */
    public synchronized void setDepthBeneathBoat(double depth) {
        depthBeneathBoat = depth;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the depth beneath the vessel
     *
     * @return Depth beneath the vessel
     */
    public synchronized double getDepthBeneathBoat() {
        return depthBeneathBoat;
    }

    /**
     * Updates the image of the video stream and notifies observers
     *
     * @param image New image in the video stream
     */
    public synchronized void setVideoImage(BufferedImage image) {
        videoImage = null;
        videoImage = image;
        setChanged();
        notifyObservers();
    }

    /**
     * Updates the status of the actuators. 1 if they are currently running and
     * 0 if they are currently idle.
     *
     * @param status Current status of the actuators
     */
    public synchronized void setActuatorStatus(byte status) {
        this.actuatorStatus = status;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the status of the actuators. true if they are currently running
     * and false if they are currently idle.
     *
     * @return Current status of the actuators
     */
    public synchronized boolean getActuatorStatus() {
        if (actuatorStatus == 1) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Updates the leak status in the ROV. 1 if a leak is detected, 0 if no leak
     * is detected.
     *
     * @param leak Current leak status of the ROV
     */
    public synchronized void setLeakStatus(boolean leak) {
        leakStatus = leak;
        if (!leak) {
            setEmergencyMode(false);
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the leak status of the ROV. Returns true if a leak is detected,
     * false if no leak is detected
     *
     * @return Current leak status of the ROV
     */
    public synchronized boolean getLeakStatus() {
        return leakStatus;
    }

    /**
     * Updates the temperature of the water and notifies observers
     *
     * @param temp Temperature of the water
     */
    public synchronized void setTemperature(float temp) {
        temperature = temp;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the current temperature of the water
     *
     * @return Temperature of the water
     */
    public synchronized float getTemperature() {
        return temperature;
    }

    /**
     * Updates the pressure surrounding the ROV and notifies observers
     *
     * @param pres Pressure surrounding the ROV
     */
    public synchronized void setPressure(double pres) {
        pressure = pres;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the current pressure around the ROV
     *
     * @return Current pressure around the ROV
     */
    public synchronized double getPressure() {
        return pressure;
    }

    /**
     * Returns the temperature in the sea
     *
     * @return the temperature in the sea
     */
    public double getOutsideTemp() {
        return outsideTemp;
    }

    /**
     * Sets the temperature in the sea
     *
     * @param outsideTemp the temperature in the sea
     */
    public void setOutsideTemp(double outsideTemp) {
        this.outsideTemp = outsideTemp;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the temperature inside the camera housing
     *
     * @return the temperature inside the camera housing
     */
    public double getInsideTemp() {
        return insideTemp;
    }

    /**
     * Sets the temperature inside the camera housing
     *
     * @param insideTemp the temperature inside the camera housing
     */
    public void setInsideTemp(double insideTemp) {
        this.insideTemp = insideTemp;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the humidity in the camera housing
     *
     * @return the humidity in the camera housing
     */
    public double getHumidity() {
        return humidity;
    }

    /**
     * Sets the humidity in the camera housing
     *
     * @param humidity the humidity in the camera housing
     */
    public void setHumidity(double humidity) {
        this.humidity = humidity;
        setChanged();
        notifyObservers();
    }

    /**
     * Sets the current speed of the vessel and notifies observers
     *
     * @param speed Current speed of the vessel
     */
    public synchronized void setSpeed(float speed) {
        this.speed = speed;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the current speed of the vessel
     *
     * @return Current speed of the vessel
     */
    public synchronized float getSpeed() {
        return speed;
    }

    /**
     * Returns the current image in the video stream
     *
     * @return Current image in the video stream
     */
    public synchronized BufferedImage getVideoImage() {
        return videoImage;
    }

    /**
     * Returns the state of the photoMode variable
     *
     * @return the state of the photoMode variable, true or false
     */
    public boolean isPhotoMode() {
        return photoMode;
    }

    /**
     * Sets the state of the photoMode variable
     *
     * @param photoMode the state of the photoMode variable, true or false
     */
    public void setPhotoMode(boolean photoMode) {
        this.photoMode = photoMode;
    }

    /**
     * Returns the photo mode delay
     *
     * @return the photo mode delay
     */
    public double getPhotoModeDelay() {
        return photoModeDelay;
    }

    /**
     * Sets the photo mode delay and notifies observers
     *
     * @param photoModeDelay
     */
    public void setPhotoModeDelay(double photoModeDelay) {
        this.photoModeDelay = photoModeDelay;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the photo mode delay feedback
     *
     * @return the photo mode delay feedback
     */
    public double getPhotoModeDelay_FB() {
        return photoModeDelay_FB;
    }

    /**
     * Sets the photo mode delay feedback and notifies observers
     *
     * @param photoModeDelay_FB
     */
    public void setPhotoModeDelay_FB(double photoModeDelay_FB) {
        this.photoModeDelay_FB = photoModeDelay_FB;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the camera pitch value
     *
     * @return the camera pitch value
     */
    public int getCameraPitchValue() {
        return cameraPitchValue;
    }

    /**
     * Sets the camera pitch value
     *
     * @param cameraPitchValue
     */
    public void setCameraPitchValue(int cameraPitchValue) {
        this.cameraPitchValue = cameraPitchValue;
    }

    /**
     * Returns the image number value
     *
     * @return the image number value
     */
    public int getImageNumber() {
        return imageNumber;
    }

    /**
     * Sets the image number value and notifies observers
     *
     * @param imageNumber the image number value
     */
    public void setImageNumber(int imageNumber) {
        this.imageNumber = imageNumber;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the images cleared status
     *
     * @return the images cleared status
     */
    public boolean isImagesCleared() {
        return imagesCleared;
    }

    /**
     * Sets the images cleared status
     *
     * @param imagesCleared the images cleared status
     */
    public void setImagesCleared(boolean imagesCleared) {
        this.imagesCleared = imagesCleared;
    }

    /**
     * Increases the image number by one and notifies observers
     */
    public void increaseImageNumberByOne() {
        this.imageNumber++;
        setChanged();
        notifyObservers();
    }

    // CODE BELOW ADDED FROM THE BASESTATION PROJECT
    /**
     * Checks status of thread
     *
     * @return thread status
     */
    public boolean shouldThreadRun() {
        return threadStatus;
    }

    /**
     * Sets the thread status
     *
     * @param threadStatus
     */
    public void setThreadStatus(boolean threadStatus) {
        this.threadStatus = threadStatus;
    }

    /**
     * Returns the data from the arduino
     *
     * @return the data from the arduino
     */
    public byte[] getDataFromArduino() {
        return dataFromArduino;
    }

    /**
     * Returns true if there is new data available, false if not
     *
     * @return true if new data available, false if not
     */
    public synchronized boolean isDataFromArduinoAvailable() {
        return this.dataFromArduinoAvailable;
    }

    /**
     * Sets the emergency mode status
     *
     * @param status the emergency mode status
     */
    public void setEmergencyMode(boolean status) {
        this.emergencyMode = status;
//        setChanged();
//        notifyObservers();
    }

    /**
     * Returns the emergency mode status
     *
     * @return the emergency mode status
     */
    public boolean isEmergencyMode() {
        return this.emergencyMode;
    }

    /**
     * Returns the streaming status
     *
     * @return the streaming status
     */
    public boolean isStreaming() {
        return streaming;
    }

    /**
     * Sets the streaming status
     *
     * @param streaming the streaming status
     */
    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    /**
     * Returns the manual mode status
     *
     * @return the manual mode status
     */
    public boolean isManualMode() {
        return manualMode;
    }

    /**
     * Sets the manual mode status
     *
     * @param manualMode the manual mode status
     */
    public void setManualMode(boolean manualMode) {
        this.manualMode = manualMode;
    }

    /**
     * Returns the data updated status
     *
     * @return the data updated status
     */
    public synchronized boolean isDataUpdated() {
        return dataUpdated;
    }

    /**
     * Sets the data updated status
     *
     * @param dataUpdated the data updated status
     */
    public synchronized void setDataUpdated(boolean dataUpdated) {
        this.dataUpdated = dataUpdated;
    }

    /**
     * Returns the controller enabled status
     *
     * @return the controller enabled status
     */
    public boolean isControllerEnabled() {
        return controllerEnabled;
    }

    /**
     * Sets the controller enabled status
     *
     * @param controllerEnabled the controller enabled status
     */
    public void setControllerEnabled(boolean controllerEnabled) {
        this.controllerEnabled = controllerEnabled;
    }

    /**
     * Returns the number of satellites
     *
     * @return the number of satellites
     */
    public synchronized int getSatellites() {
        return satellites;
    }

    /**
     * Sets the number of satellites
     *
     * @param satellites the number of satellites
     */
    public synchronized void setSatellites(int satellites) {
        this.satellites = satellites;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the altitude
     *
     * @returnthe altitude
     */
    public synchronized float getAltitude() {
        return altitude;
    }

    /**
     * Sets the altitude
     *
     * @param altitude the altitude
     */
    public synchronized void setAltitude(float altitude) {
        this.altitude = altitude;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the GPS angle
     *
     * @return the GPS angle
     */
    public synchronized double getGPSAngle() {
        return gpsAngle;
    }

    /**
     * Sets the GPS angle
     *
     * @param angle the GPS angle
     */
    public synchronized void setGPSAngle(double angle) {
        this.gpsAngle = angle;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the roll if the ROV
     *
     * @return the roll if the ROV
     */
    public synchronized double getRoll() {
        return roll;
    }

    /**
     * Sets the roll if the ROV
     *
     * @param roll the roll if the ROV
     */
    public synchronized void setRoll(double roll) {
        this.roll = roll;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the pitch of the ROV
     *
     * @return the pitch of the ROV
     */
    public synchronized double getPitch() {
        return pitch;
    }

    /**
     * Sets the pitch of the ROV
     *
     * @param pitch the pitch of the ROV
     */
    public synchronized void setPitch(double pitch) {
        this.pitch = pitch;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the voltage supply value
     *
     * @return the voltage supply value
     */
    public synchronized double getVoltage() {
        return voltage;
    }

    /**
     * Sets the voltage supply value
     *
     * @param voltage the voltage supply value
     */
    public synchronized void setVoltage(double voltage) {
        this.voltage = voltage;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the start logging status
     *
     * @return the start logging status
     */
    public boolean getStartLogging() {
        return startLogging;
    }

    /**
     * Sets the start logging status
     *
     * @param startLogging the start logging status
     */
    public void setStartLogging(boolean startLogging) {
        this.startLogging = startLogging;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the ROV ping
     *
     * @return the ROV ping
     */
    public Double getRovPing() {
        return rovPing;
    }

    /**
     * Sets the ROV ping
     *
     * @param rovPing the ROV ping
     */
    public void setRovPing(Double rovPing) {
        this.rovPing = rovPing;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the ROV ready status
     *
     * @return the ROV ready status
     */
    public boolean isRovReady() {
        return rovReady;
    }

    /**
     * Sets the ROV ready status
     *
     * @param rovReady the ROV ready status
     */
    public void setRovReady(boolean rovReady) {
        this.rovReady = rovReady;
    }

    /**
     * Returns the i2c error status
     *
     * @return the i2c error status
     */
    public boolean isI2cError() {
        return i2cError;
    }

    /**
     * Sets the i2c error status
     *
     * @param i2cError the i2c error status
     */
    public void setI2cError(boolean i2cError) {
        this.i2cError = i2cError;
    }

    /**
     * Returns the ROV depth
     *
     * @return the ROV depth
     */
    public Double getRovDepth() {
        return rovDepth;
    }

    /**
     * Sets the ROV depth
     *
     * @param rovDepth the ROV depth
     */
    public void setRovDepth(Double rovDepth) {
        this.rovDepth = rovDepth;
    }

    /**
     * Returns the stepper PS position
     *
     * @return the stepper PS position
     */
    public int getFb_stepperPSPos() {
        return fb_stepperPSPos;
    }

    /**
     * Sets the stepper position
     *
     * @param fb_stepperPSPos the PS actuator position
     */
    public void setFb_stepperPSPos(int fb_stepperPSPos) {
        this.fb_stepperPSPos = fb_stepperPSPos;
        setChanged();
        notifyObservers();
    }

    /**
     * Returns the stepper SB position
     *
     * @return the stepper SB position
     */
    public int getFb_stepperSBPos() {
        return fb_stepperSBPos;
    }

    /**
     * Sets the stepper SB position
     *
     * @param fb_stepperSBPos the SB actuator position
     */
    public void setFb_stepperSBPos(int fb_stepperSBPos) {
        this.fb_stepperSBPos = fb_stepperSBPos;
    }

    /**
     * Returns the PS minimum actuator position
     *
     * @return the PS minimum actuator position
     */
    public int getFb_actuatorPSMinPos() {
        return fb_actuatorPSMinPos;
    }

    /**
     * Sets the PS minimum actuator position
     *
     * @param fb_actuatorPSMinPos the PS minimum actuator position
     */
    public void setFb_actuatorPSMinPos(int fb_actuatorPSMinPos) {
        this.fb_actuatorPSMinPos = fb_actuatorPSMinPos;
    }

    /**
     * Returns the SB minimum actuator position
     *
     * @return the SB minimum actuator position
     */
    public int getFb_actuatorSBMinPos() {
        return fb_actuatorSBMinPos;
    }

    /**
     * Sets the SB minimum actuator position
     *
     * @param fb_actuatorSBMinPos the SB minimum actuator position
     */
    public void setFb_actuatorSBMinPos(int fb_actuatorSBMinPos) {
        this.fb_actuatorSBMinPos = fb_actuatorSBMinPos;
    }

    /**
     * Returns the PS maximum actuator position
     *
     * @return the PS maximum actuator position
     */
    public int getFb_actuatorPSMaxPos() {
        return fb_actuatorPSMaxPos;
    }

    /**
     * Sets the PS maximum actuator position
     *
     * @param fb_actuatorPSMaxPos the PS maximum actuator position
     */
    public void setFb_actuatorPSMaxPos(int fb_actuatorPSMaxPos) {
        this.fb_actuatorPSMaxPos = fb_actuatorPSMaxPos;
    }

    /**
     * Returns the SB maximum actuator position
     *
     * @return the SB maximum actuator position
     */
    public int getFb_actuatorSBMaxPos() {
        return fb_actuatorSBMaxPos;
    }

    /**
     * Sets the SB maximum actuator position
     *
     * @param fb_actuatorSBMaxPos the PS maximum actuator position
     */
    public void setFb_actuatorSBMaxPos(int fb_actuatorSBMaxPos) {
        this.fb_actuatorSBMaxPos = fb_actuatorSBMaxPos;
    }

    /**
     * Returns the temperature in the front of the electronics box
     *
     * @return the temperature in the front of the electronics box
     */
    public double getFb_tempElBoxFront() {
        return fb_tempElBoxFront;
    }

    /**
     * Sets the temperature in the front of the electronics box
     *
     * @param fb_tempElBoxFront the temperature in the front of the electronics
     * box
     */
    public void setFb_tempElBoxFront(double fb_tempElBoxFront) {
        this.fb_tempElBoxFront = fb_tempElBoxFront;
    }

    /**
     * Returns the temperature in the rear of the electronics box
     *
     * @return the temperature in the rear of the electronics box
     */
    public double getFb_tempElBoxRear() {
        return fb_tempElBoxRear;
    }

    /**
     * Sets the temperature in the rear of the electronics box
     *
     * @param fb_tempElBoxRear the temperature in the rear of the electronics
     * box
     */
    public void setFb_tempElBoxRear(double fb_tempElBoxRear) {
        this.fb_tempElBoxRear = fb_tempElBoxRear;
    }

    /**
     * Returns PS the stepper max-to-min time
     *
     * @return the PS stepper max-to-min time
     */
    public long getPSStepperMaxToMinTime() {
        return PSStepperMaxToMinTime;
    }

    /**
     * Sets the PS stepper max-to-min time
     *
     * @param PSStepperMaxToMinTime
     */
    public void setPSStepperMaxToMinTime(long PSStepperMaxToMinTime) {
        this.PSStepperMaxToMinTime = PSStepperMaxToMinTime;
    }

    /**
     * Returns the SB stepper max-to-min time
     *
     * @return the SB stepper max-to-min time
     */
    public long getSBStepperMaxToMinTime() {
        return SBStepperMaxToMinTime;
    }

    /**
     * Sets the SB stepper max-to-min time
     *
     * @param SBStepperMaxToMinTime
     */
    public void setSBStepperMaxToMinTime(long SBStepperMaxToMinTime) {
        this.SBStepperMaxToMinTime = SBStepperMaxToMinTime;
    }

    /**
     * Updates the ROV depth data list
     *
     * @param time the time variable
     * @param value the value at that time
     */
    public void updateRovDepthDataList(String time, String value) {
        if (rovDepthDataList.size() >= 260) {
            rovDepthDataList.remove(0);
        }
        this.rovDepthDataList.add(time + ":" + value);
    }

    /**
     * Updates the depth beneath the boat data list
     *
     * @param time the time variable
     * @param value the value at that time
     */
    public void updateDepthBeneathBoatDataList(String time, String value) {
        if (depthBeneathBoatDataList.size() >= 300) {
            depthBeneathBoatDataList.remove(0);
        }
        this.depthBeneathBoatDataList.add(time + ":" + value);
    }

    /**
     * Returns the PS actuator command
     *
     * @return the PS actuator command
     */
    public int getFb_actuatorPScmd() {
        return fb_actuatorPScmd;
    }

    /**
     * Sets the PS actuator command
     *
     * @param fb_actuatorPScmd the PS actuator command
     */
    public void setFb_actuatorPScmd(int fb_actuatorPScmd) {
        this.fb_actuatorPScmd = fb_actuatorPScmd;
    }

    /**
     * Returns the SB actuator command
     *
     * @return the SB actuator command
     */
    public int getFb_actuatorSBcmd() {
        return fb_actuatorSBcmd;
    }

    /**
     * Sets the SB actuator command
     *
     * @param fb_actuatorSBcmd the SB actuator command
     */
    public void setFb_actuatorSBcmd(int fb_actuatorSBcmd) {
        this.fb_actuatorSBcmd = fb_actuatorSBcmd;
    }

    /**
     * Returns the test depth
     *
     * @return the test depth
     */
    public double getTestDepth() {
        return TestDepth;
    }

    /**
     * Sets the test depth
     *
     * @param TestDepth the test depth
     */
    public void setTestDepth(double TestDepth) {
        this.TestDepth = TestDepth;
        this.setRovDepth(TestDepth);
    }

}
