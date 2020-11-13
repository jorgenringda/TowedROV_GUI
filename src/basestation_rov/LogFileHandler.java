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
package basestation_rov;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import ntnusubsea.gui.Data;
import org.apache.commons.io.FileUtils;

/**
 * This class is responsible for logging the data, ship position, exif and
 * telementry to seperate .csv files.
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class LogFileHandler implements Runnable {

    //User settings
    int pointFreqMillis = 5000;
    int lenghtOfUmbillicalCord = 500;
    //End of user settings

    Data data;

    long lastTime = 0;
    long timeDifference = 0;

    double adjustedCoordinateRovOffset = ((lenghtOfUmbillicalCord / 100) * 0.000892);

    String shipTrack = "null";
    String Data = "null";
    String telementry = "null";
    String photoLocationTrack = "null";
    String exif = "null";

    int shipTrackPointNumb = 1;
    int DataPointNumb = 1;
    int photoLocationNumb = 1;

    String timeStampString = "";
    SimpleDateFormat timeAndDateCSV;
    SimpleDateFormat exifDateAndTime;

    String logStorageLocation = "C:\\TowedROV\\Log\\";

    String photoPosLog = "";
    String shipPosLog = "";
    String dataLog = "";
    String telementryLog = "";
    String exifLog = "";
    boolean setupIsDone = false;

    File shipPosLogFile = null;
    File dataLogFile = null;
    File telementryLogFile = null;
    File exifLogFile = null;

    Date date;
    Date dateCSV;
    Date dateExif;

    BufferedWriter outputWriterShipPos = null;
    BufferedWriter outputWriterData = null;
    BufferedWriter outputWriterTelementry = null;
    BufferedWriter outputWriterExif = null;

    int lastImageNumber = 0;
    boolean exifSetup = false;

    /**
     * The constructor of the LogFileHandler class
     *
     * @param data the shared resource Data class
     */
    public LogFileHandler(Data data) {
        this.data = data;
    }

    /**
     * Runs the LogFileHandler thread.
     */
    public void run() {
        if (!exifSetup || data.isImagesCleared()) {
            try {
                lastImageNumber = 0;
                //SimpleDateFormat exifTime = new SimpleDateFormat("yyyyMMddHHmmss");
                exifDateAndTime = new SimpleDateFormat("yyyyMMddHHmmss");
                dateExif = new Date(System.currentTimeMillis());
                exifLogFile = new File(logStorageLocation + "EXIF_LOG_" + exifDateAndTime.format(dateExif) + ".csv");
                FileUtils.touch(exifLogFile);

                outputWriterExif = new BufferedWriter(new FileWriter(exifLogFile));
                outputWriterExif.append("Latitude,Longtitude,Time");
                outputWriterExif.flush();
                exifSetup = true;
                data.setImagesCleared(true);

            } catch (Exception e) {
            }

        }
        if (data.getImageNumber() != lastImageNumber) {
            exifDateAndTime = new SimpleDateFormat("yyyyMMddHHmmss");
            dateExif = new Date(System.currentTimeMillis());
            exifDateAndTime.format(dateExif);
            lastImageNumber++;
            logExifData();

        }

        if (data.startLogging) {

            if (!setupIsDone) {
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
                    date = new Date(System.currentTimeMillis());

                    shipPosLogFile = new File(logStorageLocation + "ShipPos_LOG_" + formatter.format(date) + ".csv");
                    FileUtils.touch(shipPosLogFile);

//                File photoPosLog = new File(logStorageLocation + "PhotoPos_LOG_" + formatter.format(date) + ".csv");
//                FileUtils.touch(photoPosLog);
                    dataLogFile = new File(logStorageLocation + "Data_LOG_" + formatter.format(date) + ".csv");
                    FileUtils.touch(dataLogFile);

                    telementryLogFile = new File(logStorageLocation + "Telementry_LOG_" + formatter.format(date) + ".csv");
                    FileUtils.touch(telementryLogFile);

                    outputWriterShipPos = new BufferedWriter(new FileWriter(shipPosLogFile));

                    outputWriterData = new BufferedWriter(new FileWriter(dataLogFile));

                    outputWriterTelementry = new BufferedWriter(new FileWriter(telementryLogFile));

                    outputWriterShipPos.append("Point,Time,Latitude,Longtitude,Speed,ROV Depth,GPSHeading");
                    outputWriterShipPos.flush();

                    outputWriterData.append("Point,Time,Roll,Pitch,Depth,"
                            + "DepthToSeaFloor,ROV_Depth,WingAngleSB,"
                            + "WingAnglePort,setPoint,"
                            + "Voltage,Emergency, outsideTemp,"
                            + "insideTempCameraHouse, humidity, tempElBoxFromt,"
                            + "tempElBoxRear, I2CError, LeakDetection");
                    outputWriterData.flush();

                    outputWriterTelementry.append("Latitude,Longtitude, Elevation, Time");
//                        + "Elevation,Heading,Time");
                    outputWriterTelementry.flush();

                    setupIsDone = true;

                } catch (Exception ex) {
                    System.out.println("ERROR: " + ex);
                }
            }

            dateCSV = new Date(System.currentTimeMillis());
            timeStampString = String.valueOf(java.time.LocalTime.now());
            timeAndDateCSV = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

            logShipPosition();
            logData();
            logTelementry();

        } else {
            setupIsDone = false;
        }
    }

//    private void logPhotoPosition(BufferedWriter bw)
//    {
//        try
//        {
//            double shipsLatitude = data.getLatitude();
//            double shipsLontitude = data.getLongitude();
//            double rovHeadingRelToShip = data.getHeading() - 180;
//            if (rovHeadingRelToShip < 0)
//            {
//                rovHeadingRelToShip = rovHeadingRelToShip + 360;
//            }
//            //convert to radians
//
//            double normalizedHeading = Math.atan2(Math.sin(rovHeadingRelToShip), Math.cos(rovHeadingRelToShip));
//            double rovHeadingRelToShipSin = sin(normalizedHeading * PI / 180);
//            double rovHeadingRelToShipSinRest = 0;
//
//            if (rovHeadingRelToShipSin < 0)
//            {
//                rovHeadingRelToShipSinRest = 1 + rovHeadingRelToShipSin;
//
//            }
//
//            double rovLatitude = 0;
//            double rovLongtitude = 0;
//
//            rovLatitude = data.getLatitude() - (adjustedCoordinateRovOffset * rovHeadingRelToShipSinRest);
//            if (rovHeadingRelToShipSinRest != 0)
//            {
//                rovLongtitude = data.getLongitude() + (adjustedCoordinateRovOffset * rovHeadingRelToShipSin);
//            } else
//            {
//                rovLongtitude = data.getLongitude();
//            }
//
//            photoLocationTrack = "";
//            photoLocationTrack = photoLocationNumb + ","
//                    + data.getLatitude() + "," + data.getLongitude() + ","
//                    + data.getSpeed() + "," + data.getRovDepth();
//
//            bw.append(photoLocationTrack);
//            bw.append('\n');
//            bw.flush();
//
//            photoLocationNumb++;
//        } catch (Exception e)
//        {
//            System.out.println("Error: " + e);
//        }
//
//    }
    /**
     * Closes the BufferedWriter for each log file.
     */
    public void closeLog() {
        try {
            outputWriterShipPos.close();
            outputWriterData.close();
            outputWriterExif.close();
            outputWriterTelementry.close();
        } catch (Exception e) {
            System.out.println("Problem closing log file");
        }

    }

    /**
     * Logs the exif data to file
     */
    private void logExifData() {
        try {
            exifLog = "";
            exifLog = data.getLatitude() + ","
                    + data.getLongitude();

            outputWriterExif.append('\n');
            outputWriterExif.append(telementryLog);
            outputWriterExif.flush();
        } catch (Exception e) {
        }

    }

    /**
     * Logs the telementry data to file
     */
    private void logTelementry() {
        try {
            telementryLog = "";
            telementryLog = data.getLatitude() + ","
                    + data.getLongitude() + ","
                    + data.getDepth() + ","
                    + timeAndDateCSV.format(dateCSV);

            outputWriterTelementry.append('\n');
            outputWriterTelementry.append(telementryLog);
            outputWriterTelementry.flush();

        } catch (Exception e) {
            System.out.println("Error writing telementry...");
        }

    }

    /**
     * Logs the data to file
     */
    private void logData() {
        try {
            dataLog = "";
            dataLog = DataPointNumb + ","
                    + timeStampString + ","
                    + String.valueOf(data.getRollAngle()) + ","
                    + String.valueOf(data.getPitchAngle()) + ","
                    + String.valueOf(data.getDepthBeneathBoat()) + ","
                    + String.valueOf(data.getDepthBeneathRov()) + ","
                    + String.valueOf(data.getRovDepth()) + ","
                    + String.valueOf(data.getWingAngleSb()) + ","
                    + String.valueOf(data.getWingAnglePort()) + ","
                    + String.valueOf(data.getTargetDistance()) + ","
                    + String.valueOf(data.getVoltage()) + ","
                    + String.valueOf(data.getOutsideTemp()) + ","
                    + String.valueOf(data.getInsideTemp()) + ","
                    + String.valueOf(data.getHumidity()) + ","
                    + String.valueOf(data.getFb_tempElBoxFront()) + ","
                    + String.valueOf(data.getFb_tempElBoxRear()) + ","
                    + String.valueOf(data.isI2cError()) + ","
                    + String.valueOf(data.getLeakStatus()) + ",";

//            outputWriterData.append(String.valueOf(DataPointNumb));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(timeStampString));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getRollAngle()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getPitchAngle()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getDepth()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getDepthBeneathRov()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getRovDepth()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getFb_actuatorPSPos()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getFb_actuatorSBPos()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getFb_actuatorPScmd()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getFb_actuatorSBcmd()));
//            outputWriterData.append(',');
//            outputWriterData.append(String.valueOf(data.getVoltage()));
            outputWriterData.append('\n');
            outputWriterData.append(dataLog);
            outputWriterData.flush();
            DataPointNumb++;
        } catch (Exception e) {
        }
    }

    /**
     * Logs the error data to file. Not finished
     */
    private void logErrorMessages() {

    }

    /**
     * Logs the ship position data to file
     */
    private void logShipPosition() {
        try {
            shipTrack = "";
            shipTrack = shipTrackPointNumb + "," + timeStampString + ","
                    + data.getLatitude() + "," + data.getLongitude() + ","
                    + data.getSpeed() + "," + data.getRovDepth() + "," + data.getGPSAngle();
            outputWriterShipPos.append('\n');
            outputWriterShipPos.append(shipTrack);
            outputWriterShipPos.flush();
            shipTrackPointNumb++;

        } catch (Exception e) {
            System.out.println("Error: " + e);
        }
    }
}
