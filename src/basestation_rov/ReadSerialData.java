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

import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jssc.SerialPort;
import jssc.SerialPortList;
import jssc.SerialPortException;
import ntnusubsea.gui.Data;

/**
 * Responsible for reading serial data from the GPS, Sonar and IMU values on the
 * base station.
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class ReadSerialData implements Runnable {

    boolean portIsOpen = false;
    String comPort = "";
    String myName = "";
    int baudRate = 0;
    Data data = null;
    public HashMap<String, String> incommingData = new HashMap<>();
    private static volatile double depth;
    private static volatile double tempC;

    /**
     * The constructor of the ReadSerialData class.
     *
     * @param data the shared resource Data class
     * @param comPort the given com port to read from
     * @param baudRate the given baud rate
     * @param myName the name of the com port
     */
    public ReadSerialData(Data data, String comPort, int baudRate, String myName) {
        this.myName = myName;

        this.comPort = comPort;
        this.baudRate = baudRate;
        this.data = data;
    }

    /**
     * Runs the ReadSerialData thread. Reads serial data form the given com port
     * and at the given baud rate.
     */
    @Override
    public void run() {
        while (true) {
            try {

                readData(comPort, baudRate);
            } catch (Exception e) {
            }

        }
    }

    /**
     * Returns the available com ports
     *
     * @return the available com ports
     */
    public String[] getAvailableComPorts() {
        String[] portNames = SerialPortList.getPortNames();

        if (portNames.length == 0) {
             System.out.println("There are no serial-ports available!");
             //System.out.println("Press enter to exit...");

            try {
                System.in.read();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < portNames.length; i++) {
            System.out.println(portNames[i]);
        }
        return portNames;
    }

    /**
     * Sets the depth to the shared resource Data class
     */
    public void sendDepth() {
        data.setDepth((float) depth);
    }

    /**
     * Sets the temperature to the shared resource Data class
     */
    public void sendTempC() {
        data.setTemperature((float) tempC);
    }

    /**
     * Reads serial data form the given com port and at the given baud rate.
     *
     * @param comPort the given com port
     * @param baudRate the given baud rate
     */
    public void readData(String comPort, int baudRate) {

        // long lastTime = System.nanoTime();
        // ConcurrentHashMap<String, String> SerialDataList = new ConcurrentHashMap<>();
        boolean recievedData = false;
        //Declare special symbol used in serial data stream from Arduino
        String startChar = "<";
        String endChar = ">";
        String seperationChar = ":";

        SerialPort serialPort = new SerialPort(comPort);

        if (!portIsOpen) {
            try {
                serialPort.openPort();
                portIsOpen = true;
                System.out.println(comPort + " is open");
            } catch (SerialPortException ex) {
                System.out.println(ex.getMessage());
            }
        }

        while (recievedData == false) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {
            }
            String buffer;
            try {
                serialPort.setParams(baudRate, 8, 1, 0);
                buffer = serialPort.readString();

                System.out.println("reading serial data "+buffer);
                // System.out.println(buffer);
                boolean dataNotNull = false;
                boolean dataHasFormat = false;

                if ((buffer != null)) {
                    dataHasFormat = true;
                } else {
                    dataHasFormat = false;
                    dataNotNull = false;
                }
                if (dataHasFormat) {


                        if (buffer.contains("<") && buffer.contains(">")) {
                        String dataStream = buffer;
                        dataStream = dataStream.substring(dataStream.indexOf(startChar) + 1);
                        dataStream = dataStream.substring(0, dataStream.indexOf(endChar));
                        //dataStream = dataStream.replace("?", "");
                        String[] data = dataStream.split(seperationChar);

                        for (int i = 0; i < data.length; i = i + 2) {
                            //this.data.data.put(data[i], data[i + 1]);
                            incommingData.put(data[i], data[i + 1]);
                        }
                        //recievedData = true;
                        //this.data.handleDataFromRemote();
                        sendIncommingDataToDataHandler();
                    }
                }

//            if (elapsedTimer != 0)
//            {
//                System.out.println("Data is recieved in: " + elapsedTimer + " millis"
//                        + " or with: " + 1000 / elapsedTimer + " Hz");
//            } else
//            {
//                System.out.println("Data is recieved in: " + elapsedTimer + " millis"
//                        + " or with: unlimited Hz!");
//            }
            } catch (Exception ex) {
                System.out.println("Lost connection to " + myName + "    Ex: " + ex);
            }
        }
    }

    /**
     * Compare keys to control values coming in from remote, and puts the
     * correct value to correct variable in the shared resource Data class.
     */
    private void sendIncommingDataToDataHandler() {
        for (Map.Entry e : incommingData.entrySet()) {
            String key = (String) e.getKey();
            String value = (String) e.getValue();

            switch (key) {
                case "Satelites_in_view_value_0":
                    data.setSatellites(Integer.parseInt(value));
                    // setSatellites(Integer.parseInt(value));
                    break;
                case "Altitude":
                    data.setAltitude(Float.parseFloat(value));
                    //setAltitude(Float.parseFloat(value));
                    break;
                case "GPSAngle":
                    data.setGPSAngle(Double.parseDouble(value));
                    //setAngle(Float.parseFloat(value));
                    break;
                case "Speed":
                    data.setSpeed(Float.parseFloat(value));
                    //setSpeed(Float.parseFloat(value));
                    break;
                case "GPS_and_DOP_and_active_satalites_value_0":
                    data.setLatitude(Float.parseFloat(value));
                    //setLatitude(Float.parseFloat(value));
                    break;
                case "Global_Positions_System_fix_data_value_1":
                    data.setLongitude(Float.parseFloat(value));
                    //setLongitude(Float.parseFloat(value));
                    break;
                case "Depth_of_water_0":
                    data.setDepthBeneathBoat(Double.parseDouble(value) * -1);
                case "Depth_below_Transducer_M":
                    double doubleDepth = Double.parseDouble(value) * -1;
                    data.setDepthBeneathBoat(doubleDepth);
                    //setDepth(Float.parseFloat(value));
                    break;
                case "Depth_of_water_1":
                    data.setDepthBeneathBoat(Double.parseDouble(value) * -1);
                    //setDepth(Float.parseFloat(value));
                    break;
                case "Depth_of_water_value_0":
                    data.setDepthBeneathBoat(Double.parseDouble(value) * -1);
                    //setDepth(Float.parseFloat(value));
                    break;
                case "Depth_of_water_2":
                    data.setDepthBeneathBoat(Double.parseDouble(value) * -1);
                    //setDepth(Float.parseFloat(value));
                    break;
                case "Mean_Temprature_Water_C":
                    data.setTemperature(Float.parseFloat(value));
                    //setTemperature(Float.parseFloat(value));
                    break;
                case "Roll":
                    data.setRoll(Double.parseDouble(value));
                    //setRoll(Integer.parseInt(value));
                    break;
                case "Pitch":
                    data.setPitch(Double.parseDouble(value));
                    //setPitch(Integer.parseInt(value));
                    break;
                case "Heading":
//                    data.setHeading(Integer.parseInt(value));
                    //setHeading(Integer.parseInt(value));
                    break;
                case "Voltage":
                    data.setVoltage(Double.parseDouble(value));
                    break;
            
                case "Depth_below_Transducer_M":
                    data.setTestDepth(Double.parseDouble(value));
                    break;
              
                default:
                    break;
            }
        }
    }
}
