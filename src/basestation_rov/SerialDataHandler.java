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

import jssc.SerialPort;
import jssc.SerialPortList;
import ntnusubsea.gui.Data;

import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Responsible for finding and storing the com ports connected.
 *
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class SerialDataHandler {

  String comPort = "";
  SerialPort serialPort;
  Data data;
  String start_char = "<";
  String end_char = ">";
  String sep_char = ":";
  int comCheck = 0;
  private HashMap<String, String> portNamesList = new HashMap<>();

  /**
   * The constructor of the SerialDataHandler
   *
   * @param data the shared resource Data class
   */
  public SerialDataHandler(Data data) {
    this.data = data;
  }

  /**
   * Initiates the com ports. Not finished
   */
  public void initiateComPorts() {

  }

  /**
   * Saves the usable com ports found.
   */
  private void saveUsableComPorts() {
    for (Entry e : portNamesList.entrySet()) {
      String comPortKey = (String) e.getKey();
      String comPortValue = (String) e.getValue();
      if (!comPortValue.contains("Unknown")) {
        data.comPortList.put(comPortKey, comPortValue);
        comCheck++;
      }
    }
    if (comCheck < 3) {
      //Not all comports was found
      System.out.println("ERROR: Not all com ports was found, trying again...");
      findComPorts();
    }
  }

  /**
   * Finds the com ports available.
   */
  public void findComPorts() {
    int e_numb = 0;
    int baudrate = 0;
    int searchRuns = 0;
    while (searchRuns != 3) {
      String[] portNames = getAvailableComPorts();
      for (int i = 0; i < portNames.length; i++) {
        if (portNames[i].contains("COM")) {
          portNamesList.put(portNames[i], "Unknown");
        }
      }

      if (searchRuns == 0) {
        baudrate = 115200;
      }
      if (searchRuns == 1) {
        baudrate = 9600;
      }

      for (Entry e : portNamesList.entrySet()) {
        String comPortKey = (String) e.getKey();
        String comPortValue = (String) e.getValue();

        if (comPortValue.contains("Unknown")) {
          serialPort = new SerialPort(comPortKey);
        }
        try {
          serialPort.openPort();
          serialPort.setParams(baudrate, 8, 1, 0);
          String buffer = "";
          Thread.sleep(5000);
          buffer = serialPort.readString();

          if (buffer != null) {

            if (buffer.contains("<") && buffer.contains(">")) {
              buffer = buffer.substring(buffer.indexOf(start_char) + 1);
              buffer = buffer.substring(0, buffer.indexOf(end_char));
              // buffer = buffer.replace("?", "");
              String[] data = buffer.split(sep_char);

              for (int i = 0; i < data.length; i = i + 2) {
                if (data[i].contains("Roll")) {
                  String key = (String) e.getKey();
                  portNamesList.put(key, "IMU");
                }
                 if (data[i].contains("GPS")) {
                  String key = (String) e.getKey();
                  portNamesList.put(key, "GPS");
                }

                 if (data[i].contains("ROVDummy") || data[i].contains("Test")) {
                  String key = (String) e.getKey();
                  portNamesList.put(key, "ROVDummy");
                }
                 if (data[i].contains("EchoSounder")||buffer.contains("<[")) {
                  String key = (String) e.getKey();
                  portNamesList.put(key, "EchoSounder");
                }
              }
            }
          }
          serialPort.closePort();
        } catch (Exception ex) {

          e_numb++;
          System.out.println("Error: " + ex);

          if (e_numb < 2) {
            findComPorts();
          } else {
            portNamesList.put(comPortKey, "Unreadable");
          }
          try {
            serialPort.closePort();

          } catch (Exception exe) {
            System.out.println("Error: Failed to close port " + exe);
          }
        }
      }
      saveUsableComPorts();
      searchRuns++;
    }
  }

  /**
   * Returns the available com ports
   *
   * @return the available com ports
   */
  private String[] getAvailableComPorts() {
    // getting serial ports list into the array
    String[] portNames = SerialPortList.getPortNames();

    if (portNames.length == 0) {
      System.out.println("No com ports is connected...");
      try {
        System.in.read();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
//        for (int i = 0; i < portNames.length; i++)
//        {
//            System.out.println(portNames[i]);
//        }
    return portNames;
  }
}
