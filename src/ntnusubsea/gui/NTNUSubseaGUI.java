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

import basestation_rov.LogFileHandler;
import basestation_rov.SerialDataHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * Main class that launches the application and schedules the different threads
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class NTNUSubseaGUI {
//
//    private static Thread readSerialData;
//    private static Thread imuThread;
//    private static Thread gpsThread;
//    private static Thread echoSounderThread;
//    private static Thread ROVDummyThread;

    private static Thread client_Rov;
    private static Thread InputControllerThread;
    private static Thread comPortFinderThread;

    protected static String ipAddress = "localHost";
    protected static int sendPort = 5057;
    protected static String IP_ROV = "192.168.0.101";
    protected static String IP_camera = "192.168.0.102";
    protected static int Port_ROV = 8088;
    protected static int Port_cameraStream = 8083;
    protected static int Port_cameraCom = 9006;
    protected static ROVFrame frame;

    /**
     * Main class that launches the application and schedules the different
     * threads
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Data data = new Data();
        Sounder sounder = new Sounder();
        SerialDataHandler sdh = new SerialDataHandler(data);
        EchoSounderFrame sonar = new EchoSounderFrame(data);
        RollPlot rovRotationPlot = new RollPlot(data);
        LogFileHandler lgh = new LogFileHandler(data);
        TCPpinger client_Pinger = new TCPpinger(IP_ROV, Port_ROV, data);
        client_Rov = new TCPClient(IP_ROV, Port_ROV, data);

        TCPClient client_ROV = new TCPClient(IP_ROV, Port_ROV, data);
        TCPClient client_Camera = new TCPClient(IP_camera, Port_cameraCom, data);
        UDPServer stream = new UDPServer(Port_cameraStream, data);
        IOControlFrame io = new IOControlFrame(data, client_ROV);
        frame = new ROVFrame(sonar, rovRotationPlot, data, io, client_Pinger, client_ROV, client_Camera, stream, sounder, lgh);
        data.addObserver(frame);
        DataUpdater dataUpdater = new DataUpdater(client_ROV, client_Camera, data);

        ScheduledExecutorService executor
                = Executors.newScheduledThreadPool(8);
        SwingUtilities.invokeLater(frame);
        SwingUtilities.invokeLater(io);
        sonar.setVisible(false);
        data.addObserver(sonar);
        rovRotationPlot.setVisible(false);
        data.addObserver(rovRotationPlot);

        data.addObserver(io);
        executor.scheduleAtFixedRate(lgh,
                0, 100, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(sonar,
                0, 100, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(rovRotationPlot,
                0, 100, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(dataUpdater,
                1000, 100, TimeUnit.MILLISECONDS);

//        comPortFinderThread = new Thread(new ComPortFinder(sdh, data));
//        comPortFinderThread.start();
//        comPortFinderThread.setName("ComPortFinder");
//        // Start searching for com ports:
//        long timeDifference = 0;
//        long lastTime = 0;
//        long timeDelay = 5000;
//        boolean connected = false;
//        boolean foundComPort = false;
//        boolean listedCom = false;
//
//        while (true)
//        {
//
//            if (!foundComPort)
//
//            {
//                System.out.println("Searching for com ports...");
//                sdh.findComPorts();
//                foundComPort = true;
//            }
//
//            if (!listedCom)
//            {
//                System.out.println("Com ports found:");
//
//                if (data.comPortList.isEmpty())
//                {
//                    System.out.println("None");
//                } else
//                {
//                    for (Entry e : data.comPortList.entrySet())
//                    {
//                        String comPortKey = (String) e.getKey();
//                        String comPortValue = (String) e.getValue();
//                        System.out.println(comPortKey + " : " + comPortValue);
//
//                    }
//                }
//                System.out.println("--End of com list--");
//                listedCom = true;
//
//                for (Entry e : data.comPortList.entrySet())
//                {
//                    String comPortKey = (String) e.getKey();
//                    String comPortValue = (String) e.getValue();
//                    if (comPortValue.contains("IMU"))
//                    {
//                        imuThread = new Thread(new ReadSerialData(data, comPortKey, 115200, comPortValue));
//                        imuThread.start();
//                        imuThread.setName(comPortValue);
//
//                    }
//
//                    if (comPortValue.contains("GPS"))
//                    {
//                        gpsThread = new Thread(new ReadSerialData(data, comPortKey, 115200, comPortValue));
//                        gpsThread.start();
//                        gpsThread.setName(comPortValue);
//
//                    }
//
//                    if (comPortValue.contains("EchoSounder"))
//                    {
//                        echoSounderThread = new Thread(new ReadSerialData(data, comPortKey, 4800, comPortValue));
//                        echoSounderThread.start();
//                        echoSounderThread.setName(comPortValue);
//                    }
//
//                    if (comPortValue.contains("ROVDummy"))
//                    {
//                        ROVDummyThread = new Thread(new ReadSerialData(data, comPortKey, 115200, comPortValue));
//                        ROVDummyThread.start();
//                        ROVDummyThread.setName(comPortValue);
//                    }
//                }
//            }
//        }
    }
}
