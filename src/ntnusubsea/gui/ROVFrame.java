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
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * Main frame of the application. Lets the user connect, watch the video stream,
 * observe sensor values, control the lights, open all the extra tools etc.
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356 edited 2020 changed
 * actuator bars to show from 0-2000, changed manual controll to buttons
 */
public class ROVFrame extends javax.swing.JFrame implements Runnable, Observer {

    ImagePanel videoSheet;
    ImagePanel fullscreenVideoSheet;
    private BufferedImage videoImage;
    private Data data;
    private Double setpoint = 0.00;
    private int targetMode = 0;
    private EchoSounderFrame echoSounder;
    private OptionsFrame options;
    private Thread sounderThread;
    private TCPpinger client_Pinger;
    private TCPClient client_ROV;
    private TCPClient client_Camera;
    private UDPServer udpServer;
    private Sounder sounder;
    private LogFileHandler lgh;
    private VideoEncoder encoder;
    private ScheduledExecutorService clientThreadExecutor;
    private ScheduledExecutorService encoderThreadExecutor;
    private IOControlFrame io;
    private int cameraPitchValue = 0;
    private double photoModeDelay = 1.0;
    private static DecimalFormat df2 = new DecimalFormat("#.##");
    private boolean debugMode = false;

    private int cmd_stepper = 0;

    /**
     * Creates new form ROVFrame
     *
     * @param echoSounder Echo sounder frame to show graphs
     * @param data Data containing shared variables
     * @param client_Pinger the ping TCP client
     * @param io I/O frame to control inputs and outputs
     * @param client_ROV the ROV TCP client
     * @param client_Camera the camera TCP client
     * @param sounder the alarm sounder
     * @param udpServer the camera UDP server
     * @param lgh the log file handler
     */
    public ROVFrame(EchoSounderFrame echoSounder, Data data, IOControlFrame io, TCPpinger client_Pinger, TCPClient client_ROV, TCPClient client_Camera, UDPServer udpServer, Sounder sounder, LogFileHandler lgh) {
        this.clientThreadExecutor = null;
        this.encoderThreadExecutor = null;
        initComponents();
        this.data = data;
        this.echoSounder = echoSounder;
        this.client_Pinger = client_Pinger;
        this.client_ROV = client_ROV;
        this.client_Camera = client_Camera;
        this.udpServer = udpServer;
        this.options = new OptionsFrame(this.data, this.client_ROV);
        this.io = io;
        this.sounder = sounder;
        this.lgh = lgh;
        this.getContentPane().setBackground(new Color(39, 44, 50));
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        videoSheet = new ImagePanel(cameraPanel.getWidth(), cameraPanel.getHeight());
        videoSheet.setBackground(cameraPanel.getBackground());
        fullscreenVideoSheet = new ImagePanel(cameraPanel1.getWidth(), cameraPanel1.getHeight());
        fullscreenVideoSheet.setBackground(cameraPanel1.getBackground());
        videoSheet.setOpaque(false);
        fullscreenVideoSheet.setOpaque(false);
        cameraPanel.add(videoSheet);
        cameraPanel1.add(fullscreenVideoSheet);
        setpointLabel.setText("Current setpoint: " + setpoint + "m");
        exitFullscreenButton.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitFullscreen");
//        depthInputTextField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendInput");
        exitFullscreenButton.getActionMap().put("exitFullscreen", exitFullscreenAction);
//        depthInputTextField.getActionMap().put("sendInput", sendInputAction);

        if (this.debugMode) {
            // ROV RPi:
            emergencyStopButton.setEnabled(true);
            targetDistanceTextField.setEnabled(true);
            depthModeButton.setEnabled(true);
            seafloorModeButton.setEnabled(true);
            InputControllerButton.setEnabled(true);
            manualControlButton.setEnabled(true);
            resetManualControlButton.setEnabled(true);
            lockButton.setEnabled(true);
            io.enableIO();
            // Camera RPi:
            lightSwitch_lbl.setEnabled(true);
            lightSwitch.setEnabled(true);
            lightSlider.setEnabled(true);
            photoModeButton.setEnabled(true);
            cameraPitchSlider.setEnabled(true);
            cameraOffsetTextField.setEnabled(true);
            delayTextField.setEnabled(true);

            // Setup for report:
//            manualControlButton.setSelected(true);
//            InputControllerButton.setSelected(true);
//            lightSwitchBlueLED.setSelected(true);
//            photoModeDelay_FB_Label.setText("0.02 s");
//            jMenuConnect.setText("Connected 2/2");
//            jMenuCalibrate.setText("Calibrated!");
//            jMenuRovReady.setText("ROV Ready!");
//            jMenuLogger.setText("Logging!");
//            jMenuVoltage.setText("Voltage: 38.65 V");
//            jMenuPing.setText("Ping (ROV): 3.24 ms");
//            actuatorPosLabel.setText("<html>PS: 85<br/><br/>SB: 85");
//            actuatorSBPosBar.setValue(85);
//            actuatorPSPosBar.setValue(85);
//            rollLabel.setText("Roll Angle: 2");
//            pitchLabel.setText("Pitch Angle: -26");
//            wingLabel.setText("Wing Angle: -40");
//            actuatorPSPosLabel.setText("PS Actuator Pos: 86");
//            actuatorSBPosLabel.setText("SB Actuator Pos: 87");
//            i2cErrorLabel.setText("I2C: OK");
//            outsideTempLabel.setText("Outside Temp: 8.25 C");
//            insideTempLabel.setText("Inside Temp: 18.48 C");
//            humidityLabel.setText("Humidity: 55.45");
//            pressureLabel.setText("Pressure: 150 mBar");
//            leakLabel.setText("No leak detected");
//            headingLabel.setText("Heading: 19.0500");
//            latitudeLabel.setText("Latitude: 62.5274");
//            longitudeLabel.setText("Longitude: 6.2086");
//            seafloorDepthBoatLabel.setText("Beneath Boat: 9.71 m");
//            seafloorDepthRovLabel.setText("Beneath ROV: 7.84 m");
//            rovDepthLabel.setText("ROV Depth: 1.57 m");
//
//            try {
//                jMenuConnect.setIcon(new ImageIcon(ImageIO.read(new File("src/ntnusubsea/gui/Images/Calibrated.gif"))));
//                jMenuCalibrate.setIcon(new ImageIcon(ImageIO.read(new File("src/ntnusubsea/gui/Images/Calibrated.gif"))));
//                jMenuRovReady.setIcon(new ImageIcon(ImageIO.read(new File("src/ntnusubsea/gui/Images/Calibrated.gif"))));
//                jMenuLogger.setIcon(new ImageIcon(ImageIO.read(new File("src/ntnusubsea/gui/Images/Calibrated.gif"))));
//                jMenuVoltage.setIcon(new ImageIcon(ImageIO.read(new File("src/ntnusubsea/gui/Images/Calibrated.gif"))));
//                jMenuPing.setIcon(new ImageIcon(ImageIO.read(new File("src/ntnusubsea/gui/Images/Calibrated.gif"))));
//            } catch (IOException e) {
//            }
        }
    }

    /**
     * Calls the paintSheet method which updates the diplayed image.
     *
     * @param image The image to be displayed on the GUI
     */
    public void showImage(BufferedImage image) {
        if (fullscreen.isVisible()) {
            fullscreenVideoSheet.setSize(cameraPanel1.getSize());
            fullscreenVideoSheet.paintSheet(ImageUtils.resize(image, fullscreenVideoSheet.getParent().getWidth(), fullscreenVideoSheet.getParent().getHeight()));
        } else {
            videoSheet.paintSheet(ImageUtils.resize(image, videoSheet.getParent().getWidth(), videoSheet.getParent().getHeight()));
            videoSheet.setSize(cameraPanel.getSize());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        fullscreen = new javax.swing.JFrame();
        cameraPanel1 = new javax.swing.JPanel();
        exitFullscreenButton = new javax.swing.JButton();
        buttonGroup1 = new javax.swing.ButtonGroup();
        helpframe = new javax.swing.JFrame();
        jPanel1 = new javax.swing.JPanel();
        helpFrameOKbutton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        window = new javax.swing.JPanel();
        background = new javax.swing.JPanel();
        cameraPanel = new javax.swing.JPanel();
        fullscreenButton = new javax.swing.JButton();
        controlPanel = new javax.swing.JPanel();
        depthPanel = new javax.swing.JPanel();
        targetDistanceTextField = new javax.swing.JFormattedTextField();
        depthHeader = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        depthModeButton = new javax.swing.JRadioButton();
        seafloorModeButton = new javax.swing.JRadioButton();
        setpointLabel = new javax.swing.JLabel();
        manualControlButton = new javax.swing.JToggleButton();
        jLabel5 = new javax.swing.JLabel();
        resetManualControlButton = new javax.swing.JButton();
        lockButton = new javax.swing.JToggleButton();
        InputControllerButton = new javax.swing.JToggleButton();
        winAngLabel = new javax.swing.JLabel();
        wingAngTextField = new javax.swing.JFormattedTextField();
        jTextField2 = new javax.swing.JTextField();
        lightPanel = new javax.swing.JPanel();
        lightHeader = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        lightSwitch = new javax.swing.JToggleButton();
        lightSwitch_lbl = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 180), new java.awt.Dimension(0, 180), new java.awt.Dimension(32767, 180));
        lightSlider = new javax.swing.JSlider();
        emergencyPanel = new javax.swing.JPanel();
        emergencyHeader = new javax.swing.JLabel();
        emergencyStopButton = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JSeparator();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 100), new java.awt.Dimension(0, 100), new java.awt.Dimension(32767, 100));
        cameraControlPanel = new javax.swing.JPanel();
        delayTextField = new javax.swing.JFormattedTextField();
        cameraHeader = new javax.swing.JLabel();
        jSeparator9 = new javax.swing.JSeparator();
        photoModeButton = new javax.swing.JToggleButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cameraPitchSlider = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        cameraOffsetTextField = new javax.swing.JFormattedTextField();
        cameraOffsetLabel = new javax.swing.JLabel();
        photoModeDelayLabel = new javax.swing.JLabel();
        photoModeDelay_FB_Label = new javax.swing.JLabel();
        imageNumberLabel = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator8 = new javax.swing.JSeparator();
        jSeparator1 = new javax.swing.JSeparator();
        infoPanel = new javax.swing.JPanel();
        actuatorPanel1 = new javax.swing.JPanel();
        stepperHeader1 = new javax.swing.JLabel();
        wingAngPSPosBar = new javax.swing.JProgressBar();
        warningLabel1 = new javax.swing.JLabel();
        actuatorPanel2 = new javax.swing.JPanel();
        stepperHeader2 = new javax.swing.JLabel();
        wingAngleSBPosBar = new javax.swing.JProgressBar();
        warningLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        seafloorDepthRovLabel = new javax.swing.JLabel();
        rovDepthLabel = new javax.swing.JLabel();
        seafloorDepthBoatLabel = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        latitudeLabel = new javax.swing.JLabel();
        longitudeLabel = new javax.swing.JLabel();
        headingLabel = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        leakLabel = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        outsideTempLabel = new javax.swing.JLabel();
        insideTempLabel = new javax.swing.JLabel();
        humidityLabel = new javax.swing.JLabel();
        pressureLabel = new javax.swing.JLabel();
        actuatorSBPosLabel1 = new javax.swing.JLabel();
        jDesktopPane1 = new javax.swing.JDesktopPane();
        jPanel3 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        pitchLabel = new javax.swing.JLabel();
        rollLabel = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        wingAnglePSPosLabel = new javax.swing.JLabel();
        wingAngleSBPosLabel = new javax.swing.JLabel();
        i2cErrorLabel = new javax.swing.JLabel();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        jMenuBar = new javax.swing.JMenuBar();
        jMenuTools = new javax.swing.JMenu();
        jMenuEchosounder = new javax.swing.JMenuItem();
        jMenuIOController = new javax.swing.JMenuItem();
        jMenuOptions = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuAbout = new javax.swing.JMenuItem();
        jMenuConnect = new javax.swing.JMenu();
        jMenuItemConnect = new javax.swing.JMenuItem();
        jMenuItemDisconnect = new javax.swing.JMenuItem();
        jMenuCalibrate = new javax.swing.JMenu();
        calibrateMenuItem = new javax.swing.JMenuItem();
        jMenuRovReady = new javax.swing.JMenu();
        jMenuLogger = new javax.swing.JMenu();
        jMenuItemStartLogging = new javax.swing.JMenuItem();
        jMenuItemStopLogging = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuVoltage = new javax.swing.JMenu();
        jMenu3 = new javax.swing.JMenu();
        jMenuPing = new javax.swing.JMenu();

        fullscreen.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        fullscreen.setBackground(new java.awt.Color(39, 44, 50));
        fullscreen.setFocusTraversalPolicyProvider(true);
        fullscreen.setForeground(new java.awt.Color(39, 44, 50));
        fullscreen.setLocationByPlatform(true);
        fullscreen.setUndecorated(true);
        fullscreen.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                fullscreenKeyPressed(evt);
            }
        });

        cameraPanel1.setBackground(new java.awt.Color(39, 44, 50));
        cameraPanel1.setForeground(new java.awt.Color(39, 44, 50));
        cameraPanel1.setToolTipText("");
        cameraPanel1.setMinimumSize(new java.awt.Dimension(450, 320));
        cameraPanel1.setPreferredSize(new java.awt.Dimension(718, 580));

        exitFullscreenButton.setBackground(new java.awt.Color(0, 0, 0));
        exitFullscreenButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/exitfullscreen1.gif"))); // NOI18N
        exitFullscreenButton.setBorder(null);
        exitFullscreenButton.setContentAreaFilled(false);
        exitFullscreenButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        exitFullscreenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitFullscreenButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout cameraPanel1Layout = new javax.swing.GroupLayout(cameraPanel1);
        cameraPanel1.setLayout(cameraPanel1Layout);
        cameraPanel1Layout.setHorizontalGroup(
            cameraPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, cameraPanel1Layout.createSequentialGroup()
                .addGap(0, 708, Short.MAX_VALUE)
                .addComponent(exitFullscreenButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        cameraPanel1Layout.setVerticalGroup(
            cameraPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, cameraPanel1Layout.createSequentialGroup()
                .addGap(0, 572, Short.MAX_VALUE)
                .addComponent(exitFullscreenButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout fullscreenLayout = new javax.swing.GroupLayout(fullscreen.getContentPane());
        fullscreen.getContentPane().setLayout(fullscreenLayout);
        fullscreenLayout.setHorizontalGroup(
            fullscreenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cameraPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 738, Short.MAX_VALUE)
        );
        fullscreenLayout.setVerticalGroup(
            fullscreenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(cameraPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 602, Short.MAX_VALUE)
        );

        helpframe.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        helpframe.setTitle("About");
        helpframe.setBackground(new java.awt.Color(39, 44, 50));
        helpframe.setForeground(new java.awt.Color(39, 44, 50));
        helpframe.setType(java.awt.Window.Type.POPUP);

        jPanel1.setBackground(new java.awt.Color(39, 44, 50));
        jPanel1.setForeground(new java.awt.Color(39, 44, 50));
        jPanel1.setMaximumSize(new java.awt.Dimension(395, 304));
        jPanel1.setMinimumSize(new java.awt.Dimension(395, 304));
        jPanel1.setPreferredSize(new java.awt.Dimension(395, 304));

        helpFrameOKbutton.setBackground(new java.awt.Color(39, 44, 50));
        helpFrameOKbutton.setForeground(new java.awt.Color(255, 255, 255));
        helpFrameOKbutton.setText("Close");
        helpFrameOKbutton.setFocusPainted(false);
        helpFrameOKbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpFrameOKbuttonActionPerformed(evt);
            }
        });

        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("<html>This code is for the bachelor thesis named \"Towed-ROV\". The purpose is to build a ROV which will be towed behind a surface vessel and act as a multi-sensor platform, were it shall be easy to place new sensors. There is also a video stream from the ROV.  <br/><br/>The system consists of two Raspberry Pis in the ROV that is connected to several Arduino micro controllers. These micro controllers asre connected to feedback from the actuator, the echo sounder and extra optional I/O. The external computer which is on the surface vessel is connected to a GPS and echo sounder over USB, and the ROV over Ethernet. It will present and log data in addition to handle user commands for controlling the ROV.<br/><br/> Created by Håkon Longva Haram, Robin Stamnes Thorholm and Bjørnar Magnus Tennfjord.<html>");
        jLabel1.setToolTipText("");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 160, Short.MAX_VALUE)
                        .addComponent(helpFrameOKbutton)
                        .addGap(0, 161, Short.MAX_VALUE))
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(helpFrameOKbutton)
                .addContainerGap())
        );

        javax.swing.GroupLayout helpframeLayout = new javax.swing.GroupLayout(helpframe.getContentPane());
        helpframe.getContentPane().setLayout(helpframeLayout);
        helpframeLayout.setHorizontalGroup(
            helpframeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
        );
        helpframeLayout.setVerticalGroup(
            helpframeLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jTextField1.setText("jTextField1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Towed ROV");
        setAutoRequestFocus(false);
        setBackground(new java.awt.Color(39, 44, 50));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setForeground(new java.awt.Color(39, 44, 50));
        setMinimumSize(new java.awt.Dimension(1460, 890));
        setSize(new java.awt.Dimension(1445, 853));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        window.setBackground(new java.awt.Color(39, 44, 50));
        window.setForeground(new java.awt.Color(39, 44, 50));
        window.setMinimumSize(new java.awt.Dimension(1445, 830));
        window.setPreferredSize(new java.awt.Dimension(1445, 830));
        window.setLayout(new java.awt.GridBagLayout());

        background.setBackground(new java.awt.Color(39, 44, 50));
        background.setForeground(new java.awt.Color(39, 44, 50));
        background.setToolTipText("");
        background.setAlignmentX(5.0F);
        background.setMinimumSize(new java.awt.Dimension(1445, 830));
        background.setPreferredSize(new java.awt.Dimension(1445, 830));

        cameraPanel.setBackground(new java.awt.Color(39, 44, 50));
        cameraPanel.setForeground(new java.awt.Color(39, 44, 50));
        cameraPanel.setToolTipText("");
        cameraPanel.setAlignmentX(0.8F);
        cameraPanel.setAlignmentY(0.8F);
        cameraPanel.setMaximumSize(new java.awt.Dimension(985, 605));
        cameraPanel.setMinimumSize(new java.awt.Dimension(985, 605));
        cameraPanel.setPreferredSize(new java.awt.Dimension(985, 605));

        fullscreenButton.setBackground(new java.awt.Color(0, 0, 0));
        fullscreenButton.setForeground(new java.awt.Color(255, 255, 255));
        fullscreenButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/fullscreen1.gif"))); // NOI18N
        fullscreenButton.setBorder(null);
        fullscreenButton.setContentAreaFilled(false);
        fullscreenButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        fullscreenButton.setHideActionText(true);
        fullscreenButton.setName(""); // NOI18N
        fullscreenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fullscreenButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout cameraPanelLayout = new javax.swing.GroupLayout(cameraPanel);
        cameraPanel.setLayout(cameraPanelLayout);
        cameraPanelLayout.setHorizontalGroup(
            cameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cameraPanelLayout.createSequentialGroup()
                .addContainerGap(955, Short.MAX_VALUE)
                .addComponent(fullscreenButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        cameraPanelLayout.setVerticalGroup(
            cameraPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, cameraPanelLayout.createSequentialGroup()
                .addContainerGap(575, Short.MAX_VALUE)
                .addComponent(fullscreenButton, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        controlPanel.setBackground(new java.awt.Color(39, 44, 50));
        controlPanel.setForeground(new java.awt.Color(39, 44, 50));
        controlPanel.setMinimumSize(new java.awt.Dimension(150, 140));
        controlPanel.setPreferredSize(new java.awt.Dimension(768, 190));

        depthPanel.setBackground(new java.awt.Color(39, 44, 50));
        depthPanel.setMaximumSize(new java.awt.Dimension(260, 210));

        targetDistanceTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        targetDistanceTextField.setToolTipText("Depth (m)");
        targetDistanceTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                targetDistanceTextFieldActionPerformed(evt);
            }
        });

        depthHeader.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        depthHeader.setForeground(new java.awt.Color(255, 255, 255));
        depthHeader.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        depthHeader.setText("ROV Control");

        jSeparator4.setBackground(new java.awt.Color(67, 72, 83));
        jSeparator4.setForeground(new java.awt.Color(67, 72, 83));

        depthModeButton.setBackground(new java.awt.Color(39, 44, 50));
        buttonGroup1.add(depthModeButton);
        depthModeButton.setForeground(new java.awt.Color(255, 255, 255));
        depthModeButton.setSelected(true);
        depthModeButton.setText("Depth");
        depthModeButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        depthModeButton.setFocusPainted(false);
        depthModeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                depthModeButtonActionPerformed(evt);
            }
        });

        seafloorModeButton.setBackground(new java.awt.Color(39, 44, 50));
        buttonGroup1.add(seafloorModeButton);
        seafloorModeButton.setForeground(new java.awt.Color(255, 255, 255));
        seafloorModeButton.setText("Distance from seafloor");
        seafloorModeButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        seafloorModeButton.setFocusPainted(false);
        seafloorModeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                seafloorModeButtonActionPerformed(evt);
            }
        });

        setpointLabel.setBackground(new java.awt.Color(39, 44, 50));
        setpointLabel.setForeground(new java.awt.Color(255, 255, 255));
        setpointLabel.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        setpointLabel.setText("0.0");
        setpointLabel.setToolTipText("");
        setpointLabel.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        setpointLabel.setOpaque(true);

        manualControlButton.setBackground(new java.awt.Color(39, 44, 50));
        buttonGroup1.add(manualControlButton);
        manualControlButton.setForeground(new java.awt.Color(39, 44, 50));
        manualControlButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-off.gif"))); // NOI18N
        manualControlButton.setBorder(null);
        manualControlButton.setContentAreaFilled(false);
        manualControlButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        manualControlButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-off.gif"))); // NOI18N
        manualControlButton.setFocusPainted(false);
        manualControlButton.setFocusable(false);
        manualControlButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-on.gif"))); // NOI18N
        manualControlButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manualControlButtonActionPerformed(evt);
            }
        });

        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Manual Control:");
        jLabel5.setPreferredSize(new java.awt.Dimension(85, 16));

        resetManualControlButton.setBackground(new java.awt.Color(39, 44, 50));
        resetManualControlButton.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        resetManualControlButton.setForeground(new java.awt.Color(255, 255, 255));
        resetManualControlButton.setText("RESET");
        resetManualControlButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        resetManualControlButton.setFocusPainted(false);
        resetManualControlButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetManualControlButtonActionPerformed(evt);
            }
        });

        lockButton.setBackground(new java.awt.Color(39, 44, 50));
        lockButton.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        lockButton.setForeground(new java.awt.Color(255, 255, 255));
        lockButton.setText("LOCK");
        lockButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lockButton.setFocusPainted(false);
        lockButton.setMaximumSize(new java.awt.Dimension(63, 30));
        lockButton.setMinimumSize(new java.awt.Dimension(63, 30));
        lockButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lockButtonActionPerformed(evt);
            }
        });

        InputControllerButton.setBackground(new java.awt.Color(39, 44, 50));
        InputControllerButton.setFont(new java.awt.Font("Dialog", 1, 10)); // NOI18N
        InputControllerButton.setForeground(new java.awt.Color(255, 255, 255));
        InputControllerButton.setText("IC");
        InputControllerButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        InputControllerButton.setFocusPainted(false);
        InputControllerButton.setMaximumSize(new java.awt.Dimension(63, 30));
        InputControllerButton.setMinimumSize(new java.awt.Dimension(63, 30));
        InputControllerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                InputControllerButtonActionPerformed(evt);
            }
        });

        winAngLabel.setForeground(new java.awt.Color(255, 255, 255));
        winAngLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        winAngLabel.setText("Wing Angle");
        winAngLabel.setPreferredSize(new java.awt.Dimension(85, 16));

        wingAngTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        wingAngTextField.setToolTipText("Degrees(Deg)");
        wingAngTextField.setEnabled(false);
        wingAngTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                wingAngTextFieldActionPerformed(evt);
            }
        });

        jTextField2.setBackground(new java.awt.Color(39, 44, 50));
        jTextField2.setForeground(new java.awt.Color(255, 255, 255));
        jTextField2.setText("Current set point:");

        javax.swing.GroupLayout depthPanelLayout = new javax.swing.GroupLayout(depthPanel);
        depthPanel.setLayout(depthPanelLayout);
        depthPanelLayout.setHorizontalGroup(
            depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator4)
            .addComponent(depthHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(depthPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(manualControlButton, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(winAngLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(depthPanelLayout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addComponent(wingAngTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(29, 29, 29)
                .addGroup(depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lockButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(resetManualControlButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(InputControllerButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
            .addGroup(depthPanelLayout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addGroup(depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(depthPanelLayout.createSequentialGroup()
                        .addGap(57, 57, 57)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(setpointLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(targetDistanceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(depthPanelLayout.createSequentialGroup()
                        .addComponent(depthModeButton)
                        .addGap(6, 6, 6)
                        .addComponent(seafloorModeButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        depthPanelLayout.setVerticalGroup(
            depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(depthPanelLayout.createSequentialGroup()
                .addComponent(depthHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jSeparator4, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(setpointLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(targetDistanceTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(depthModeButton)
                    .addGroup(depthPanelLayout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(seafloorModeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(depthPanelLayout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addComponent(manualControlButton, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(depthPanelLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(depthPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(depthPanelLayout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(resetManualControlButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, 0)
                                .addComponent(lockButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)
                                .addComponent(InputControllerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(depthPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(winAngLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(wingAngTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(18, 18, 18))
        );

        lightPanel.setBackground(new java.awt.Color(39, 44, 50));
        lightPanel.setMaximumSize(new java.awt.Dimension(156, 213));

        lightHeader.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        lightHeader.setForeground(new java.awt.Color(255, 255, 255));
        lightHeader.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lightHeader.setText("Lights");

        jSeparator5.setBackground(new java.awt.Color(67, 72, 83));
        jSeparator5.setForeground(new java.awt.Color(67, 72, 83));

        lightSwitch.setBackground(new java.awt.Color(39, 44, 50));
        lightSwitch.setForeground(new java.awt.Color(39, 44, 50));
        lightSwitch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-off.gif"))); // NOI18N
        lightSwitch.setBorder(null);
        lightSwitch.setContentAreaFilled(false);
        lightSwitch.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lightSwitch.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-off.gif"))); // NOI18N
        lightSwitch.setFocusPainted(false);
        lightSwitch.setFocusable(false);
        lightSwitch.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-on.gif"))); // NOI18N
        lightSwitch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lightSwitchActionPerformed(evt);
            }
        });

        ImageIcon myimage = new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/light-bulb.png"));
        Image img = myimage.getImage();
        Image img2 = img.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        ImageIcon imgIcon = new ImageIcon(img2);
        lightSwitch_lbl.setIcon(imgIcon);
        lightSwitch_lbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lightSwitch_lbl.setEnabled(false);
        lightSwitch_lbl.setMaximumSize(new java.awt.Dimension(63, 100));
        lightSwitch_lbl.setMinimumSize(new java.awt.Dimension(63, 100));

        lightSlider.setBackground(new java.awt.Color(39, 44, 50));
        lightSlider.setMaximum(24);
        lightSlider.setMinimum(19);
        lightSlider.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lightSlider.setEnabled(false);
        lightSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                lightSliderMouseReleased(evt);
            }
        });

        javax.swing.GroupLayout lightPanelLayout = new javax.swing.GroupLayout(lightPanel);
        lightPanel.setLayout(lightPanelLayout);
        lightPanelLayout.setHorizontalGroup(
            lightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lightPanelLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(lightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(lightPanelLayout.createSequentialGroup()
                        .addGroup(lightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(lightPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(lightSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(lightPanelLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lightSwitch, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(46, 46, 46))
                    .addComponent(lightHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator5)
                    .addGroup(lightPanelLayout.createSequentialGroup()
                        .addGap(60, 60, 60)
                        .addComponent(lightSwitch_lbl, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(60, Short.MAX_VALUE))))
        );
        lightPanelLayout.setVerticalGroup(
            lightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(lightPanelLayout.createSequentialGroup()
                .addComponent(lightHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator5, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addComponent(lightSwitch_lbl, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(lightSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(lightSwitch, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(66, 66, 66))
            .addComponent(filler2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        emergencyPanel.setBackground(new java.awt.Color(39, 44, 50));
        emergencyPanel.setMaximumSize(new java.awt.Dimension(153, 213));

        emergencyHeader.setBackground(new java.awt.Color(28, 28, 28));
        emergencyHeader.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        emergencyHeader.setForeground(new java.awt.Color(255, 255, 255));
        emergencyHeader.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        emergencyHeader.setText("Emergency surfacing");

        emergencyStopButton.setBackground(new java.awt.Color(39, 44, 50));
        emergencyStopButton.setForeground(new java.awt.Color(28, 28, 28));
        ImageIcon egm_myimage = new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/emg-stop.gif"));
        Image egm_img = egm_myimage.getImage();
        Image egm_img2 = egm_img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        ImageIcon egm_imgIcon = new ImageIcon(egm_img2);
        emergencyStopButton.setIcon(egm_imgIcon);
        emergencyStopButton.setBorder(null);
        emergencyStopButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        emergencyStopButton.setEnabled(false);
        emergencyStopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                emergencyStopButtonActionPerformed(evt);
            }
        });

        jSeparator6.setBackground(new java.awt.Color(67, 72, 83));
        jSeparator6.setForeground(new java.awt.Color(67, 72, 83));

        javax.swing.GroupLayout emergencyPanelLayout = new javax.swing.GroupLayout(emergencyPanel);
        emergencyPanel.setLayout(emergencyPanelLayout);
        emergencyPanelLayout.setHorizontalGroup(
            emergencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(emergencyPanelLayout.createSequentialGroup()
                .addGroup(emergencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSeparator6)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, emergencyPanelLayout.createSequentialGroup()
                        .addGap(47, 47, 47)
                        .addComponent(emergencyStopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, emergencyPanelLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(emergencyHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)))
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );
        emergencyPanelLayout.setVerticalGroup(
            emergencyPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(emergencyPanelLayout.createSequentialGroup()
                .addComponent(emergencyHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35)
                .addComponent(emergencyStopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(35, 35, 35))
            .addGroup(emergencyPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(filler1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cameraControlPanel.setBackground(new java.awt.Color(39, 44, 50));
        cameraControlPanel.setMaximumSize(new java.awt.Dimension(190, 213));
        cameraControlPanel.setPreferredSize(new java.awt.Dimension(190, 213));

        delayTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        delayTextField.setToolTipText("Time between each frame (0-99). - Press enter to send command.");
        delayTextField.setEnabled(false);
        delayTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delayTextFieldActionPerformed(evt);
            }
        });

        cameraHeader.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        cameraHeader.setForeground(new java.awt.Color(255, 255, 255));
        cameraHeader.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        cameraHeader.setText("Camera Control");
        cameraHeader.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jSeparator9.setBackground(new java.awt.Color(67, 72, 83));
        jSeparator9.setForeground(new java.awt.Color(67, 72, 83));

        photoModeButton.setBackground(new java.awt.Color(39, 44, 50));
        photoModeButton.setForeground(new java.awt.Color(39, 44, 50));
        photoModeButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-off.gif"))); // NOI18N
        photoModeButton.setBorder(null);
        photoModeButton.setContentAreaFilled(false);
        photoModeButton.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        photoModeButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-off.gif"))); // NOI18N
        photoModeButton.setEnabled(false);
        photoModeButton.setFocusPainted(false);
        photoModeButton.setFocusable(false);
        photoModeButton.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/switch-on.gif"))); // NOI18N
        photoModeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                photoModeButtonActionPerformed(evt);
            }
        });

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Photo Mode");

        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Delay:");

        cameraPitchSlider.setBackground(new java.awt.Color(39, 44, 50));
        cameraPitchSlider.setMaximum(10);
        cameraPitchSlider.setMinimum(-10);
        cameraPitchSlider.setValue(0);
        cameraPitchSlider.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        cameraPitchSlider.setEnabled(false);
        cameraPitchSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                cameraPitchSliderMouseReleased(evt);
            }
        });

        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Camera Pitch offset:");

        cameraOffsetTextField.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        cameraOffsetTextField.setToolTipText("Camera Pitch (50-75). - Press enter to send command.");
        cameraOffsetTextField.setEnabled(false);
        cameraOffsetTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cameraOffsetTextFieldActionPerformed(evt);
            }
        });

        cameraOffsetLabel.setForeground(new java.awt.Color(255, 255, 255));
        cameraOffsetLabel.setText("0");

        photoModeDelayLabel.setForeground(new java.awt.Color(255, 255, 255));
        photoModeDelayLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        photoModeDelayLabel.setText("0.00 s");

        photoModeDelay_FB_Label.setForeground(new java.awt.Color(255, 255, 255));
        photoModeDelay_FB_Label.setText("1.00 s");

        imageNumberLabel.setForeground(new java.awt.Color(255, 255, 255));
        imageNumberLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        imageNumberLabel.setText("0");

        javax.swing.GroupLayout cameraControlPanelLayout = new javax.swing.GroupLayout(cameraControlPanel);
        cameraControlPanel.setLayout(cameraControlPanelLayout);
        cameraControlPanelLayout.setHorizontalGroup(
            cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator9)
            .addGroup(cameraControlPanelLayout.createSequentialGroup()
                .addGap(69, 69, 69)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(cameraHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(cameraControlPanelLayout.createSequentialGroup()
                .addGroup(cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, cameraControlPanelLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cameraPitchSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(cameraControlPanelLayout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addGroup(cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(cameraControlPanelLayout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(photoModeDelayLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addGap(18, 18, 18)
                .addGroup(cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(photoModeDelay_FB_Label, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cameraOffsetTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cameraOffsetLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, cameraControlPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(photoModeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(imageNumberLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(83, 83, 83))
        );
        cameraControlPanelLayout.setVerticalGroup(
            cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(cameraControlPanelLayout.createSequentialGroup()
                .addComponent(cameraHeader, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator9, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addGroup(cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(delayTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(photoModeDelayLabel)
                    .addComponent(photoModeDelay_FB_Label))
                .addGap(4, 4, 4)
                .addGroup(cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(cameraOffsetLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(cameraControlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(cameraOffsetTextField, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cameraPitchSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(photoModeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(imageNumberLabel)
                .addGap(246, 246, 246))
        );

        delayTextField.getAccessibleContext().setAccessibleDescription("Time between each frame");

        jSeparator2.setBackground(new java.awt.Color(67, 72, 83));
        jSeparator2.setForeground(new java.awt.Color(67, 72, 83));
        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jSeparator8.setBackground(new java.awt.Color(67, 72, 83));
        jSeparator8.setForeground(new java.awt.Color(67, 72, 83));
        jSeparator8.setOrientation(javax.swing.SwingConstants.VERTICAL);

        jSeparator1.setBackground(new java.awt.Color(67, 72, 83));
        jSeparator1.setForeground(new java.awt.Color(67, 72, 83));
        jSeparator1.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout controlPanelLayout = new javax.swing.GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(depthPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lightPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator8, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cameraControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(emergencyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        controlPanelLayout.setVerticalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator2, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator8, javax.swing.GroupLayout.Alignment.TRAILING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cameraControlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(emergencyPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(depthPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 1, Short.MAX_VALUE))
            .addComponent(lightPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        infoPanel.setBackground(new java.awt.Color(39, 44, 50));
        infoPanel.setForeground(new java.awt.Color(39, 44, 50));
        infoPanel.setMaximumSize(new java.awt.Dimension(455, 509));
        infoPanel.setMinimumSize(new java.awt.Dimension(455, 509));

        actuatorPanel1.setBackground(new java.awt.Color(42, 48, 57));
        actuatorPanel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(45, 53, 62), 1, true));
        actuatorPanel1.setForeground(new java.awt.Color(255, 255, 255));

        stepperHeader1.setBackground(new java.awt.Color(42, 48, 57));
        stepperHeader1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        stepperHeader1.setForeground(new java.awt.Color(255, 255, 255));
        stepperHeader1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        stepperHeader1.setText("PS Wing Angle");

        wingAngPSPosBar.setBackground(new java.awt.Color(42, 48, 57));
        wingAngPSPosBar.setForeground(new java.awt.Color(97, 184, 114));
        wingAngPSPosBar.setMaximum(30);
        wingAngPSPosBar.setMinimum(-30);
        wingAngPSPosBar.setToolTipText("");
        wingAngPSPosBar.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wingAngPSPosBarStateChanged(evt);
            }
        });

        warningLabel1.setBackground(new java.awt.Color(42, 48, 57));
        warningLabel1.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        warningLabel1.setForeground(new java.awt.Color(255, 255, 255));
        warningLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        warningLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        warningLabel1.setOpaque(true);

        javax.swing.GroupLayout actuatorPanel1Layout = new javax.swing.GroupLayout(actuatorPanel1);
        actuatorPanel1.setLayout(actuatorPanel1Layout);
        actuatorPanel1Layout.setHorizontalGroup(
            actuatorPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(stepperHeader1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(warningLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(wingAngPSPosBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        actuatorPanel1Layout.setVerticalGroup(
            actuatorPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(actuatorPanel1Layout.createSequentialGroup()
                .addComponent(stepperHeader1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(wingAngPSPosBar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(warningLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        actuatorPanel2.setBackground(new java.awt.Color(42, 48, 57));
        actuatorPanel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(45, 53, 62), 1, true));
        actuatorPanel2.setForeground(new java.awt.Color(255, 255, 255));

        stepperHeader2.setBackground(new java.awt.Color(42, 48, 57));
        stepperHeader2.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        stepperHeader2.setForeground(new java.awt.Color(255, 255, 255));
        stepperHeader2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        stepperHeader2.setText("SB Wing Angle");

        wingAngleSBPosBar.setBackground(new java.awt.Color(42, 48, 57));
        wingAngleSBPosBar.setForeground(new java.awt.Color(97, 184, 114));
        wingAngleSBPosBar.setMaximum(30);
        wingAngleSBPosBar.setMinimum(-30);
        wingAngleSBPosBar.setToolTipText("");
        wingAngleSBPosBar.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                wingAngleSBPosBarStateChanged(evt);
            }
        });

        warningLabel2.setBackground(new java.awt.Color(42, 48, 57));
        warningLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        warningLabel2.setForeground(new java.awt.Color(255, 255, 255));
        warningLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        warningLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout actuatorPanel2Layout = new javax.swing.GroupLayout(actuatorPanel2);
        actuatorPanel2.setLayout(actuatorPanel2Layout);
        actuatorPanel2Layout.setHorizontalGroup(
            actuatorPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(stepperHeader2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(wingAngleSBPosBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(warningLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        actuatorPanel2Layout.setVerticalGroup(
            actuatorPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(actuatorPanel2Layout.createSequentialGroup()
                .addComponent(stepperHeader2, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(wingAngleSBPosBar, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(warningLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel2.setBackground(new java.awt.Color(39, 44, 50));

        jPanel4.setBackground(new java.awt.Color(39, 44, 50));
        jPanel4.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(45, 53, 62), 1, true));

        seafloorDepthRovLabel.setBackground(new java.awt.Color(39, 46, 54));
        seafloorDepthRovLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        seafloorDepthRovLabel.setForeground(new java.awt.Color(255, 255, 255));
        seafloorDepthRovLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        seafloorDepthRovLabel.setText("Beneath ROV: 0.1m");
        seafloorDepthRovLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        seafloorDepthRovLabel.setOpaque(true);

        rovDepthLabel.setBackground(new java.awt.Color(39, 46, 54));
        rovDepthLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        rovDepthLabel.setForeground(new java.awt.Color(255, 255, 255));
        rovDepthLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rovDepthLabel.setText("ROV Depth: 0.1m");
        rovDepthLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rovDepthLabel.setOpaque(true);

        seafloorDepthBoatLabel.setBackground(new java.awt.Color(39, 46, 54));
        seafloorDepthBoatLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        seafloorDepthBoatLabel.setForeground(new java.awt.Color(255, 255, 255));
        seafloorDepthBoatLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        seafloorDepthBoatLabel.setText("Beneath Boat: 0.1m");
        seafloorDepthBoatLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        seafloorDepthBoatLabel.setOpaque(true);

        jLabel8.setBackground(new java.awt.Color(42, 48, 57));
        jLabel8.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Depth");
        jLabel8.setOpaque(true);

        jLabel10.setBackground(new java.awt.Color(39, 46, 54));
        jLabel10.setOpaque(true);

        jLabel16.setBackground(new java.awt.Color(39, 46, 54));
        jLabel16.setOpaque(true);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(seafloorDepthRovLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(seafloorDepthBoatLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(rovDepthLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(seafloorDepthBoatLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(seafloorDepthRovLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(rovDepthLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0))
        );

        jPanel5.setBackground(new java.awt.Color(39, 44, 50));
        jPanel5.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(45, 53, 62), 1, true));

        latitudeLabel.setBackground(new java.awt.Color(39, 46, 54));
        latitudeLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        latitudeLabel.setForeground(new java.awt.Color(255, 255, 255));
        latitudeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        latitudeLabel.setText("Latitude: 0.1");
        latitudeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        latitudeLabel.setOpaque(true);

        longitudeLabel.setBackground(new java.awt.Color(39, 46, 54));
        longitudeLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        longitudeLabel.setForeground(new java.awt.Color(255, 255, 255));
        longitudeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        longitudeLabel.setText("Longitude: 0.1");
        longitudeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        longitudeLabel.setOpaque(true);

        headingLabel.setBackground(new java.awt.Color(39, 46, 54));
        headingLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        headingLabel.setForeground(new java.awt.Color(255, 255, 255));
        headingLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        headingLabel.setText("Heading: 0.1");
        headingLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        headingLabel.setOpaque(true);

        jLabel11.setBackground(new java.awt.Color(42, 48, 57));
        jLabel11.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel11.setText("Position");
        jLabel11.setOpaque(true);

        jLabel12.setBackground(new java.awt.Color(39, 46, 54));
        jLabel12.setOpaque(true);

        jLabel17.setBackground(new java.awt.Color(39, 46, 54));
        jLabel17.setOpaque(true);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(latitudeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(headingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(longitudeLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(headingLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(latitudeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(longitudeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel17, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel7.setBackground(new java.awt.Color(39, 44, 50));
        jPanel7.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(45, 53, 62), 1, true));

        leakLabel.setBackground(new java.awt.Color(39, 46, 54));
        leakLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        leakLabel.setForeground(new java.awt.Color(255, 255, 255));
        leakLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        leakLabel.setText("No leak detected");
        leakLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        leakLabel.setOpaque(true);

        jLabel19.setBackground(new java.awt.Color(42, 48, 57));
        jLabel19.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel19.setForeground(new java.awt.Color(255, 255, 255));
        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel19.setText("Camera Housing");
        jLabel19.setOpaque(true);

        jLabel20.setBackground(new java.awt.Color(39, 46, 54));
        jLabel20.setOpaque(true);

        jLabel21.setBackground(new java.awt.Color(39, 46, 54));
        jLabel21.setOpaque(true);

        outsideTempLabel.setBackground(new java.awt.Color(39, 46, 54));
        outsideTempLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        outsideTempLabel.setForeground(new java.awt.Color(255, 255, 255));
        outsideTempLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        outsideTempLabel.setText("Outside Temp: 0.1");
        outsideTempLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        outsideTempLabel.setMinimumSize(new java.awt.Dimension(140, 110));
        outsideTempLabel.setOpaque(true);

        insideTempLabel.setBackground(new java.awt.Color(39, 46, 54));
        insideTempLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        insideTempLabel.setForeground(new java.awt.Color(255, 255, 255));
        insideTempLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        insideTempLabel.setText("Inside Temp: 0.1");
        insideTempLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        insideTempLabel.setOpaque(true);

        humidityLabel.setBackground(new java.awt.Color(39, 46, 54));
        humidityLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        humidityLabel.setForeground(new java.awt.Color(255, 255, 255));
        humidityLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        humidityLabel.setText("Humidity: 0.1");
        humidityLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        humidityLabel.setOpaque(true);

        pressureLabel.setBackground(new java.awt.Color(39, 46, 54));
        pressureLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        pressureLabel.setForeground(new java.awt.Color(255, 255, 255));
        pressureLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pressureLabel.setText("Pressure: 0.1");
        pressureLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        pressureLabel.setOpaque(true);

        actuatorSBPosLabel1.setBackground(new java.awt.Color(39, 46, 54));
        actuatorSBPosLabel1.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        actuatorSBPosLabel1.setForeground(new java.awt.Color(255, 255, 255));
        actuatorSBPosLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        actuatorSBPosLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        actuatorSBPosLabel1.setOpaque(true);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(leakLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(pressureLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(humidityLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(outsideTempLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(insideTempLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(actuatorSBPosLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel20, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(outsideTempLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(insideTempLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(humidityLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pressureLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(leakLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(actuatorSBPosLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel3.setBackground(new java.awt.Color(39, 44, 50));
        jPanel3.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(45, 53, 62), 1, true));

        jLabel7.setBackground(new java.awt.Color(42, 48, 57));
        jLabel7.setFont(new java.awt.Font("Tahoma", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("ROV");
        jLabel7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel7.setOpaque(true);

        pitchLabel.setBackground(new java.awt.Color(39, 46, 54));
        pitchLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        pitchLabel.setForeground(new java.awt.Color(255, 255, 255));
        pitchLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        pitchLabel.setText("Pitch Angle: 0.1");
        pitchLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        pitchLabel.setOpaque(true);

        rollLabel.setBackground(new java.awt.Color(39, 46, 54));
        rollLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        rollLabel.setForeground(new java.awt.Color(255, 255, 255));
        rollLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rollLabel.setText("Roll Angle: 0.1");
        rollLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        rollLabel.setMinimumSize(new java.awt.Dimension(140, 110));
        rollLabel.setOpaque(true);

        jLabel9.setBackground(new java.awt.Color(39, 46, 54));
        jLabel9.setOpaque(true);

        jLabel15.setBackground(new java.awt.Color(39, 46, 54));
        jLabel15.setOpaque(true);

        wingAnglePSPosLabel.setBackground(new java.awt.Color(39, 46, 54));
        wingAnglePSPosLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        wingAnglePSPosLabel.setForeground(new java.awt.Color(255, 255, 255));
        wingAnglePSPosLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        wingAnglePSPosLabel.setText("PS Wing Angle: 0.1");
        wingAnglePSPosLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        wingAnglePSPosLabel.setOpaque(true);

        wingAngleSBPosLabel.setBackground(new java.awt.Color(39, 46, 54));
        wingAngleSBPosLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        wingAngleSBPosLabel.setForeground(new java.awt.Color(255, 255, 255));
        wingAngleSBPosLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        wingAngleSBPosLabel.setText("SB Wing Angle: 0.1");
        wingAngleSBPosLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        wingAngleSBPosLabel.setOpaque(true);

        i2cErrorLabel.setBackground(new java.awt.Color(39, 46, 54));
        i2cErrorLabel.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        i2cErrorLabel.setForeground(new java.awt.Color(255, 255, 255));
        i2cErrorLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        i2cErrorLabel.setText("I2C: OK");
        i2cErrorLabel.setToolTipText("");
        i2cErrorLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        i2cErrorLabel.setOpaque(true);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 164, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(wingAnglePSPosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel3Layout.createSequentialGroup()
                    .addGap(176, 176, 176)
                    .addComponent(i2cErrorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rollLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pitchLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(wingAngleSBPosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(rollLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pitchLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(wingAnglePSPosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(wingAngleSBPosLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(i2cErrorLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jDesktopPane1.setLayer(jPanel3, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout jDesktopPane1Layout = new javax.swing.GroupLayout(jDesktopPane1);
        jDesktopPane1.setLayout(jDesktopPane1Layout);
        jDesktopPane1Layout.setHorizontalGroup(
            jDesktopPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDesktopPane1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jDesktopPane1Layout.setVerticalGroup(
            jDesktopPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDesktopPane1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(30, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jDesktopPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 40, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(60, 60, 60)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jDesktopPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(50, 50, 50)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(64, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout infoPanelLayout = new javax.swing.GroupLayout(infoPanel);
        infoPanel.setLayout(infoPanelLayout);
        infoPanelLayout.setHorizontalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(actuatorPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(actuatorPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        infoPanelLayout.setVerticalGroup(
            infoPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(infoPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(actuatorPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15)
                .addComponent(actuatorPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout backgroundLayout = new javax.swing.GroupLayout(background);
        background.setLayout(backgroundLayout);
        backgroundLayout.setHorizontalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundLayout.createSequentialGroup()
                .addGroup(backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(controlPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 985, Short.MAX_VALUE)
                    .addComponent(cameraPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(infoPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        backgroundLayout.setVerticalGroup(
            backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundLayout.createSequentialGroup()
                .addGroup(backgroundLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, backgroundLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(infoPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, backgroundLayout.createSequentialGroup()
                        .addComponent(cameraPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(controlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(170, 170, 170))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 856;
        gridBagConstraints.ipady = 318;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        window.add(background, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(window, gridBagConstraints);
        getContentPane().add(filler3, new java.awt.GridBagConstraints());

        jMenuBar.setForeground(new java.awt.Color(39, 44, 50));

        jMenuTools.setText("Tools");
        jMenuTools.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        jMenuEchosounder.setText("Echo sounder");
        jMenuEchosounder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuEchosounderActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuEchosounder);

        jMenuIOController.setText("I/O Controller");
        jMenuIOController.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuIOControllerActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuIOController);

        jMenuOptions.setText("Options");
        jMenuOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuOptionsActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuOptions);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemExit);

        jMenuBar.add(jMenuTools);

        jMenuHelp.setText("Help");
        jMenuHelp.setToolTipText("");
        jMenuHelp.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        jMenuAbout.setText("About");
        jMenuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuAbout);

        jMenuBar.add(jMenuHelp);

        jMenuConnect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))); // NOI18N
        jMenuConnect.setText("Connect");
        jMenuConnect.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jMenuConnect.setFocusPainted(true);

        jMenuItemConnect.setText("Connect");
        jMenuItemConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectActionPerformed(evt);
            }
        });
        jMenuConnect.add(jMenuItemConnect);

        jMenuItemDisconnect.setText("Disconnect");
        jMenuItemDisconnect.setEnabled(false);
        jMenuItemDisconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDisconnectActionPerformed(evt);
            }
        });
        jMenuConnect.add(jMenuItemDisconnect);

        jMenuBar.add(jMenuConnect);

        jMenuCalibrate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))); // NOI18N
        jMenuCalibrate.setText("Not Calibrated!");
        jMenuCalibrate.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jMenuCalibrate.setDisabledIcon(null);

        calibrateMenuItem.setText("Calibrate");
        calibrateMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                calibrateMenuItemActionPerformed(evt);
            }
        });
        jMenuCalibrate.add(calibrateMenuItem);

        jMenuBar.add(jMenuCalibrate);

        jMenuRovReady.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))); // NOI18N
        jMenuRovReady.setText("ROV not ready");
        jMenuRovReady.setContentAreaFilled(false);
        jMenuRovReady.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jMenuRovReady.setFocusable(false);
        jMenuBar.add(jMenuRovReady);

        jMenuLogger.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))); // NOI18N
        jMenuLogger.setText("Not logging");
        jMenuLogger.setActionCommand("jMenuLogger");
        jMenuLogger.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jMenuLogger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuLoggerActionPerformed(evt);
            }
        });

        jMenuItemStartLogging.setText("Start logging");
        jMenuItemStartLogging.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jMenuItemStartLogging.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStartLoggingActionPerformed(evt);
            }
        });
        jMenuLogger.add(jMenuItemStartLogging);

        jMenuItemStopLogging.setText("Stop logging");
        jMenuItemStopLogging.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jMenuItemStopLogging.setEnabled(false);
        jMenuItemStopLogging.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemStopLoggingActionPerformed(evt);
            }
        });
        jMenuLogger.add(jMenuItemStopLogging);

        jMenuBar.add(jMenuLogger);

        jMenu1.setText("                ");
        jMenu1.setContentAreaFilled(false);
        jMenu1.setEnabled(false);
        jMenu1.setFocusable(false);
        jMenu1.setPreferredSize(new java.awt.Dimension(600, 21));
        jMenuBar.add(jMenu1);

        jMenuVoltage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))); // NOI18N
        jMenuVoltage.setText("Voltage: 0.0 V");
        jMenuVoltage.setContentAreaFilled(false);
        jMenuVoltage.setFocusable(false);
        jMenuBar.add(jMenuVoltage);

        jMenu3.setContentAreaFilled(false);
        jMenu3.setEnabled(false);
        jMenu3.setFocusable(false);
        jMenu3.setPreferredSize(new java.awt.Dimension(20, 5));
        jMenuBar.add(jMenu3);

        jMenuPing.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))); // NOI18N
        jMenuPing.setText("Ping (ROV): Not connected");
        jMenuBar.add(jMenuPing);

        setJMenuBar(jMenuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fullscreenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullscreenButtonActionPerformed
        Rectangle maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int width = (int) maximumWindowBounds.getWidth();
        int height = (int) maximumWindowBounds.getHeight();
        fullscreen.setLocationRelativeTo(this);
        this.setVisible(false);
        fullscreen.setExtendedState(MAXIMIZED_BOTH);
        fullscreen.setUndecorated(true);
        fullscreen.setVisible(true);
        exitFullscreenButton.setBounds(width - exitFullscreenButton.getWidth(), height - exitFullscreenButton.getHeight() - 25, 30, 30);
    }//GEN-LAST:event_fullscreenButtonActionPerformed

    private void exitFullscreenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitFullscreenButtonActionPerformed
        fullscreen.setVisible(false);
        fullscreen.dispose();
        this.setVisible(true);
    }//GEN-LAST:event_exitFullscreenButtonActionPerformed

    private void fullscreenKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_fullscreenKeyPressed
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ESCAPE) {
            fullscreen.dispose();
        }
        System.out.println(key);
    }//GEN-LAST:event_fullscreenKeyPressed

    private void wingAngPSPosBarStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wingAngPSPosBarStateChanged

        wingAngPSPosBar.setForeground(new Color(77, 192, 99));
        warningLabel1.setText("");
        warningLabel1.setBackground(new Color(42, 48, 57));
    }//GEN-LAST:event_wingAngPSPosBarStateChanged

    private void seafloorModeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_seafloorModeButtonActionPerformed
        double d = data.getDepthBeneathBoat();
        targetDistanceTextField.setText(String.valueOf(d));
        setpointLabel.setText(String.valueOf(d));
        /* actuatorControlPS.setValue(127);
        actuatorControlSB.setValue(127);
        actuatorPosLabel.setText("<html>PS: " + String.valueOf(actuatorControlPS.getValue()) + "<br/><br/>SB: " + String.valueOf(actuatorControlSB.getValue()));*/
        //jButtonManualUp.setEnabled(false);
        //jButtonManualDown.setEnabled(false);
        if (this.targetMode != 1) {
            this.targetMode = 1;
            System.out.println("Mode 1 - Distance from seafloor");
            data.setManualMode(false);
            try {
                this.client_ROV.sendCommand("cmd_targetMode:" + String.valueOf(this.targetMode));
                this.client_ROV.sendCommand("cmd_targetDistance:" + String.valueOf(d));
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        } else {
            System.out.println("Already in seafloor mode.");
        }
    }//GEN-LAST:event_seafloorModeButtonActionPerformed

    private void lightSwitchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lightSwitchActionPerformed
        if (lightSwitch.isSelected()) {
            try {
                int value = lightSlider.getValue();
                client_Camera.sendCommand("setLed:" + String.valueOf(value));
                //lightSlider.setValue(40);
            } catch (IOException ex) {
                Logger.getLogger(ROVFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                client_Camera.sendCommand("setLed:0");
                //lightSlider.setValue(19);
            } catch (IOException ex) {
                Logger.getLogger(ROVFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_lightSwitchActionPerformed

    private void emergencyStopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_emergencyStopButtonActionPerformed
//        previousSetpoint = setpoint;
//        setpoint = 0.000;
        if (!data.isEmergencyMode()) {
            data.setEmergencyMode(true);
        }
        targetDistanceTextField.setText("0.00");
        setpointLabel.setText("EMERGENCY STOP: " + targetDistanceTextField.getText() + "m");
        setpointLabel.setBackground(new Color(255, 0, 0));
        manualControlButton.doClick();
        try {
            this.client_ROV.sendCommand("cmd_emergencySurface");
        } catch (IOException ex) {
            System.out.println("IOException in emergencyStopButtonActionPerformed: " + ex.getMessage());
        }
    }//GEN-LAST:event_emergencyStopButtonActionPerformed

    private void jMenuItemConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectActionPerformed
//        String ip = (String) JOptionPane.showInputDialog(this, "Enter IP", "Connection", JOptionPane.PLAIN_MESSAGE, null, null, data.getIP_Rov());
        try {
            this.clientThreadExecutor = Executors.newScheduledThreadPool(4);
            clientThreadExecutor.scheduleAtFixedRate(client_ROV,
                    0, 100, TimeUnit.MILLISECONDS);
            clientThreadExecutor.scheduleAtFixedRate(client_Camera,
                    0, 100, TimeUnit.MILLISECONDS);
            clientThreadExecutor.scheduleAtFixedRate(udpServer,
                    0, 20, TimeUnit.MILLISECONDS);
            Thread.sleep(500);
            if (client_ROV.isConnected() && client_Camera.isConnected()) {
                // ROV RPi:
                clientThreadExecutor.scheduleAtFixedRate(client_Pinger,
                        0, 1000, TimeUnit.MILLISECONDS);
                lightSwitch_lbl.setEnabled(true);
                emergencyStopButton.setEnabled(true);
                targetDistanceTextField.setEnabled(true);
                depthModeButton.setEnabled(true);
                seafloorModeButton.setEnabled(true);
                InputControllerButton.setEnabled(true);
                manualControlButton.setEnabled(true);
                resetManualControlButton.setEnabled(true);
                lockButton.setEnabled(true);
                io.enableIO();
                // Camera RPi:
                lightSwitch.setEnabled(true);
                lightSlider.setEnabled(true);
                photoModeButton.setEnabled(true);
                cameraPitchSlider.setEnabled(true);
                cameraOffsetTextField.setEnabled(true);
                delayTextField.setEnabled(true);
                System.out.println("conenct2");

                this.client_ROV.sendCommand("cmd_pid_p:" + data.getKpDepth());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_pid_i:" + data.getKiDepth());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_pid_d:" + data.getKdDepth());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_offsetDepthBeneathROV:" + data.getOffsetDepthBeneathROV());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_offsetROVdepth:" + data.getOffsetROVdepth());
                jMenuConnect.setText("Connected 2/2");
                jMenuConnect.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/Calibrated.gif"))));
                jMenuItemDisconnect.setEnabled(true);
                jMenuItemConnect.setEnabled(false);
                JOptionPane.showMessageDialog(this,
                        "Successfully connected to the ROV RPi and the camera RPi.",
                        "Connected",
                        JOptionPane.PLAIN_MESSAGE);
            } else if (client_ROV.isConnected() && !client_Camera.isConnected()) {
                // ROV RPi:
                clientThreadExecutor.scheduleAtFixedRate(client_Pinger,
                        0, 1000, TimeUnit.MILLISECONDS);
                emergencyStopButton.setEnabled(true);
                targetDistanceTextField.setEnabled(true);
                depthModeButton.setEnabled(true);
                seafloorModeButton.setEnabled(true);
                InputControllerButton.setEnabled(true);
                manualControlButton.setEnabled(true);
                resetManualControlButton.setEnabled(true);
                lockButton.setEnabled(true);
                io.enableIO();
                this.client_ROV.sendCommand("cmd_pid_p:" + data.getKpDepth());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_pid_i:" + data.getKiDepth());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_pid_d:" + data.getKdDepth());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_offsetDepthBeneathROV:" + data.getOffsetDepthBeneathROV());
                Thread.sleep(10);
                this.client_ROV.sendCommand("cmd_offsetROVdepth:" + data.getOffsetROVdepth());
                jMenuConnect.setText("Connected 1/2");
                jMenuConnect.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
                jMenuItemDisconnect.setEnabled(true);
                jMenuItemConnect.setEnabled(false);
                JOptionPane.showMessageDialog(this,
                        "Could only connect to the ROV RPi, but not the camera RPi...",
                        "Connected 1/2",
                        JOptionPane.PLAIN_MESSAGE);
            } else if (!client_ROV.isConnected() && client_Camera.isConnected()) {
                // Camera RPi:
                lightSwitch_lbl.setEnabled(true);
                lightSwitch.setEnabled(true);
                lightSlider.setEnabled(true);
                photoModeButton.setEnabled(true);
                cameraPitchSlider.setEnabled(true);
                cameraOffsetTextField.setEnabled(true);
                delayTextField.setEnabled(true);
                jMenuConnect.setText("Connected 1/2");
                jMenuConnect.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
                jMenuItemDisconnect.setEnabled(true);
                jMenuItemConnect.setEnabled(false);
                JOptionPane.showMessageDialog(this,
                        "Could only connect to the camera RPi, but not the ROV RPi...",
                        "Connected 1/2",
                        JOptionPane.PLAIN_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Error: Could not connect to either the ROV RPi nor the camera RPi.",
                        "Error: Could not connect",
                        JOptionPane.PLAIN_MESSAGE);
                client_Pinger.disconnect();
                client_ROV.disconnect();
                client_Camera.disconnect();

                if (clientThreadExecutor != null) {
                    clientThreadExecutor.shutdown();
                    clientThreadExecutor = null;
                }
            }

        } catch (Exception ex) {
            jMenuConnect.setText("Connect");
            try {
                jMenuConnect.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
            } catch (IOException ex1) {
                System.out.println("IOException: " + ex.getMessage());
            }
            JOptionPane.showMessageDialog(this,
                    "Connection failed.",
                    "Conncetion error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jMenuItemConnectActionPerformed

    private void jMenuItemDisconnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDisconnectActionPerformed

        try {
            client_Pinger.disconnect();
            client_ROV.disconnect();
            client_Camera.disconnect();
            jMenuPing.setText("Ping (ROV): Not connected");

            if (clientThreadExecutor != null) {
                clientThreadExecutor.shutdown();
            }
            // ROV RPi:
            emergencyStopButton.setEnabled(false);
            targetDistanceTextField.setEnabled(false);
            depthModeButton.setEnabled(false);
            seafloorModeButton.setEnabled(false);
            manualControlButton.setEnabled(false);
            InputControllerButton.setEnabled(false);
            resetManualControlButton.setEnabled(false);
            lockButton.setEnabled(false);
            io.disableIO();
            // Camera RPi:
            lightSwitch_lbl.setEnabled(false);
            lightSwitch.setEnabled(false);
            lightSlider.setEnabled(false);
            photoModeButton.setEnabled(false);
            cameraPitchSlider.setEnabled(false);
            cameraOffsetTextField.setEnabled(false);
            delayTextField.setEnabled(false);

            jMenuItemDisconnect.setEnabled(false);
            jMenuItemConnect.setEnabled(true);
            jMenuConnect.setText("Connect");
            jMenuConnect.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
            JOptionPane.showMessageDialog(this,
                    "Successfully disconnected from the ROV RPi and the camera RPi.",
                    "Disconnected",
                    JOptionPane.PLAIN_MESSAGE);
            videoImage = ImageIO.read(getClass().getResource("ntnusubsea/gui/Images/TowedROV.jpg"));
            data.setVideoImage(videoImage);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to disconnect.",
                    "Disconnect error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jMenuItemDisconnectActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuEchosounderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuEchosounderActionPerformed
        echoSounder.setVisible(true);
    }//GEN-LAST:event_jMenuEchosounderActionPerformed

    private void helpFrameOKbuttonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpFrameOKbuttonActionPerformed
        helpframe.dispose();
    }//GEN-LAST:event_helpFrameOKbuttonActionPerformed

    private void jMenuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuAboutActionPerformed
        helpframe.setVisible(true);
        helpframe.setSize(helpframe.getPreferredSize());
        helpframe.pack();
        helpframe.setLocationRelativeTo(null);
        //helpframe.setLocation(this.getLocation().x, this.getLocation().y);
    }//GEN-LAST:event_jMenuAboutActionPerformed

    private void wingAngleSBPosBarStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_wingAngleSBPosBarStateChanged

        wingAngleSBPosBar.setForeground(new Color(77, 192, 99));
        warningLabel2.setText("");
        warningLabel2.setBackground(new Color(42, 48, 57));
    }//GEN-LAST:event_wingAngleSBPosBarStateChanged

    private void jMenuOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuOptionsActionPerformed
        options.setVisible(true);
    }//GEN-LAST:event_jMenuOptionsActionPerformed

    private void depthModeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_depthModeButtonActionPerformed
        double d = data.getRovDepth();
        targetDistanceTextField.setText(String.valueOf(d));
        /* actuatorControlPS.setValue(127);
        actuatorControlSB.setValue(127);
        actuatorPosLabel.setText("<html>PS: " + String.valueOf(actuatorControlPS.getValue()) + "<br/><br/>SB: " + String.valueOf(actuatorControlSB.getValue()));*/
        wingAngTextField.setEnabled(false);
        if (this.targetMode != 0) {
            this.targetMode = 0;
            data.setManualMode(false);
            System.out.println("Mode 0 - Depth");
            try {
                this.client_ROV.sendCommand("cmd_targetMode:" + String.valueOf(this.targetMode));
                this.client_ROV.sendCommand("cmd_targetDistance:" + String.valueOf(d));
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        } else {
            System.out.println("Already in depth mode.");
        }
    }//GEN-LAST:event_depthModeButtonActionPerformed

    private void jMenuIOControllerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuIOControllerActionPerformed
        io.setVisible(true);
    }//GEN-LAST:event_jMenuIOControllerActionPerformed

    private void cameraOffsetTextFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cameraOffsetTextFieldActionPerformed
    {//GEN-HEADEREND:event_cameraOffsetTextFieldActionPerformed

        try {
            cameraOffsetTextField.commitEdit();
            if (cameraOffsetTextField.getText() != null && isInteger(cameraOffsetTextField.getText())) {
                this.cameraPitchValue = Integer.parseInt(cameraOffsetTextField.getText());
            }
            if (this.cameraPitchValue > 10) {
                this.cameraPitchValue = 10;
                System.out.println("Camera Pitch input too high! Set to max (10)");
            } else if (this.cameraPitchValue < -10) {
                this.cameraPitchValue = -10;
                System.out.println("Camera Pitch input too low! Set to min (-10)");
            }

            if (this.cameraPitchValue <= 10 && this.cameraPitchValue >= -10) {
                cameraOffsetLabel.setBackground(new Color(28, 28, 28));
                cameraPitchSlider.setValue(this.cameraPitchValue);
                cameraOffsetTextField.setText(Integer.toString(this.cameraPitchValue));
                cameraOffsetLabel.setText(String.valueOf(this.cameraPitchValue));
                data.setCameraPitchValue(this.cameraPitchValue);
                System.out.println("Camera Pitch set to " + this.cameraPitchValue);
                //this.client_Camera.sendCommand("setPitch:" + String.valueOf(this.cameraPitchValue));
                // Send this to the python TcpController program running on the Camera RPi

            } else {
                cameraOffsetTextField.setValue(null);
                JOptionPane.showMessageDialog(this,
                        "Input is invalid. Valid integer values are -10 to 10.",
                        "Input error",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (ParseException ex) {
            System.out.println(ex.getMessage());

        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }//GEN-LAST:event_cameraOffsetTextFieldActionPerformed

    private void cameraPitchSliderMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_cameraPitchSliderMouseReleased
    {//GEN-HEADEREND:event_cameraPitchSliderMouseReleased
        try {
            this.cameraPitchValue = cameraPitchSlider.getValue();
            cameraOffsetLabel.setText(Integer.toString(this.cameraPitchValue));
            cameraOffsetTextField.setText(Integer.toString(this.cameraPitchValue));
            data.setCameraPitchValue(cameraPitchValue);
            System.out.println("Camera Pitch set to " + cameraPitchSlider.getValue());
            this.client_Camera.sendCommand("setPitch:" + String.valueOf(this.cameraPitchValue));
            // Send this to the java program running on the Camera RPi
        } catch (Exception ex) {
            Logger.getLogger(ROVFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_cameraPitchSliderMouseReleased

    private void delayTextFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_delayTextFieldActionPerformed
    {//GEN-HEADEREND:event_delayTextFieldActionPerformed
        try {
            if (delayTextField.getText() != null && isNumeric(delayTextField.getText())) {
                this.photoModeDelay = Double.parseDouble(delayTextField.getText());
                if (this.photoModeDelay > 99) {
                    this.photoModeDelay = 99;
                    System.out.println("Photo Mode Delay input too high! Set to max (99)");
                } else if (this.photoModeDelay < 0) {
                    this.photoModeDelay = 0;
                    System.out.println("Photo Mode Delay input too low! Set to min (0)");
                }
                photoModeDelayLabel.setText(String.valueOf(this.photoModeDelay) + " s");
                data.setPhotoModeDelay(this.photoModeDelay);
                System.out.println("Photo Mode Delay set to " + String.valueOf(this.photoModeDelay));

                this.udpServer.sendDelayCommand();
            } else {
                System.out.println("Invalid delay entered.");
            }
        } catch (NumberFormatException ex) {
            Logger.getLogger(ROVFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_delayTextFieldActionPerformed

    private void photoModeButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_photoModeButtonActionPerformed
    {//GEN-HEADEREND:event_photoModeButtonActionPerformed
        if (photoModeButton.isSelected()) {
            try {
                data.setPhotoMode(true);
            } catch (Exception ex) {
                Logger.getLogger(ROVFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            try {
                data.setPhotoMode(false);
            } catch (Exception ex) {
                Logger.getLogger(ROVFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_photoModeButtonActionPerformed

    private void lightSliderMouseReleased(java.awt.event.MouseEvent evt)//GEN-FIRST:event_lightSliderMouseReleased
    {//GEN-HEADEREND:event_lightSliderMouseReleased
        try {
            if (lightSwitch.isSelected()) {
                //data.setCameraPitchValue(cameraPitchValue);
                int value = lightSlider.getValue();
                System.out.println("ROV Lights set to " + value);
                this.client_Camera.sendCommand("setLed:" + String.valueOf(value));
                // Send this to the python TcpController program running on the Camera RPi
            }
        } catch (Exception ex) {
            Logger.getLogger(ROVFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_lightSliderMouseReleased

    private void manualControlButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_manualControlButtonActionPerformed
    {//GEN-HEADEREND:event_manualControlButtonActionPerformed
        if (this.targetMode != 2) {
            wingAngTextField.setEnabled(true);
            data.setManualMode(true);
            this.targetMode = 2;
            System.out.println("Mode 2 - Manual wing control");
            try {
                this.client_ROV.sendCommand("cmd_targetMode:" + String.valueOf(this.targetMode));
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        } else {
            depthModeButton.doClick();
            try {
                wingAngTextField.setText(0 + "°");
                System.out.println("wingAng set to " + 0);
                this.client_ROV.sendCommand("cmd_wingAng:" + 0);
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        }
    }//GEN-LAST:event_manualControlButtonActionPerformed

    private void resetManualControlButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_resetManualControlButtonActionPerformed
    {//GEN-HEADEREND:event_resetManualControlButtonActionPerformed
        /* actuatorControlPS.setValue(127);
        actuatorControlSB.setValue(127);
        actuatorPosLabel.setText("<html>PS: " + String.valueOf(actuatorControlPS.getValue()) + "<br/><br/>SB: " + String.valueOf(actuatorControlSB.getValue()));*/
        if (manualControlButton.isSelected()) {
            try {
                this.client_ROV.sendCommand("cmd_resetSteppers");
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        } else {
            //System.out.println("Not in manual control mode.");
        }
    }//GEN-LAST:event_resetManualControlButtonActionPerformed

    private void lockButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_lockButtonActionPerformed
    {//GEN-HEADEREND:event_lockButtonActionPerformed
        /*  if (actuatorControlPS.getValue() != 127) {
            actuatorControlPS.setValue(127);
            actuatorPosLabel.setText("<html>PS: " + String.valueOf(actuatorControlPS.getValue()) + "<br/><br/>SB: " + String.valueOf(actuatorControlSB.getValue()));
            try {
                this.client_ROV.sendCommand("cmd_actuatorPS:" + String.valueOf(actuatorControlPS.getValue()));
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        }
        if (actuatorControlSB.getValue() != 127) {
            actuatorControlSB.setValue(127);
            actuatorPosLabel.setText("<html>PS: " + String.valueOf(actuatorControlPS.getValue()) + "<br/><br/>SB: " + String.valueOf(actuatorControlSB.getValue()));
            try {
                this.client_ROV.sendCommand("cmd_actuatorSB:" + String.valueOf(actuatorControlSB.getValue()));
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            }
        }*/
    }//GEN-LAST:event_lockButtonActionPerformed

    private void calibrateMenuItemActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_calibrateMenuItemActionPerformed
    {//GEN-HEADEREND:event_calibrateMenuItemActionPerformed
        try {
            // TODO add your handling code here:
            // Kjør kalibrering!
            jMenuCalibrate.setText("Calibrated!");
            jMenuCalibrate.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/Calibrated.gif"))));
        } catch (IOException ex) {
            System.out.println("IOException when calibrating: " + ex.getMessage());

        }
    }//GEN-LAST:event_calibrateMenuItemActionPerformed

    private void InputControllerButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_InputControllerButtonActionPerformed
    {//GEN-HEADEREND:event_InputControllerButtonActionPerformed
        if (data.isControllerEnabled()) {
            data.setControllerEnabled(false);
        } else {
            data.setControllerEnabled(true);
        }
    }//GEN-LAST:event_InputControllerButtonActionPerformed

    private void jMenuLoggerActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuLoggerActionPerformed
    {//GEN-HEADEREND:event_jMenuLoggerActionPerformed

    }//GEN-LAST:event_jMenuLoggerActionPerformed

    private void jMenuItemStartLoggingActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemStartLoggingActionPerformed
    {//GEN-HEADEREND:event_jMenuItemStartLoggingActionPerformed
        this.data.setStartLogging(true);
        encoder = new VideoEncoder(this.data);
        this.data.addObserver(encoder);
        this.encoderThreadExecutor = Executors.newScheduledThreadPool(1);
        encoderThreadExecutor.scheduleAtFixedRate(encoder,
                0, 40, TimeUnit.MILLISECONDS);

        Runtime.getRuntime()
                .addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (encoder != null) {
                            encoder.finishVideo();
                        }
                        if (encoderThreadExecutor != null) {
                            encoderThreadExecutor.shutdown();
                        }
                    }
                },
                        "Shutdown-thread"));
        jMenuLogger.setText("Logging!");
        jMenuItemStartLogging.setEnabled(false);
        jMenuItemStopLogging.setEnabled(true);
        try {
            jMenuLogger.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/Calibrated.gif"))));
        } catch (IOException ex) {
            System.out.println("Error setting icon: " + ex.getMessage());
        }
    }//GEN-LAST:event_jMenuItemStartLoggingActionPerformed

    private void jMenuItemStopLoggingActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_jMenuItemStopLoggingActionPerformed
    {//GEN-HEADEREND:event_jMenuItemStopLoggingActionPerformed
        this.data.setStartLogging(false);
        this.lgh.closeLog();
        encoderThreadExecutor.shutdown();
        encoder.finishVideo();
        encoderThreadExecutor = null;
        encoder = null;
        jMenuLogger.setText("Not logging");
        jMenuItemStopLogging.setEnabled(false);
        jMenuItemStartLogging.setEnabled(true);

        try {
            jMenuLogger.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
        } catch (IOException ex) {
            System.out.println("Error setting icon: " + ex.getMessage());
        }
    }//GEN-LAST:event_jMenuItemStopLoggingActionPerformed

    private void targetDistanceTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_targetDistanceTextFieldActionPerformed
        data.setEmergencyMode(false);
        try {
            targetDistanceTextField.commitEdit();
            if (targetDistanceTextField.getText() != null && isNumeric(targetDistanceTextField.getText())) {
                double newSetpoint;
                try {
                    newSetpoint = Double.parseDouble(targetDistanceTextField.getText());
                } catch (ClassCastException ex) {
                    Long newSetpointLong = Long.parseLong(targetDistanceTextField.getText());
                    newSetpoint = newSetpointLong.doubleValue();
                }

                if (newSetpoint <= 50 && newSetpoint >= 0) {
                    setpointLabel.setBackground(new Color(39, 44, 50));
                    //                    previousSetpoint = setpoint;
                    //                    setpoint = newSetpoint;
                    //                    depthInputTextField.setValue(null);
                    setpointLabel.setText("Current setpoint: " + newSetpoint + "m");
                    System.out.println("targetDistance set to " + String.valueOf(newSetpoint));
                    this.client_ROV.sendCommand("cmd_targetDistance:" + String.valueOf(newSetpoint));
                } else {
                    targetDistanceTextField.setValue(null);
                    targetDistanceTextField.setText("");
                    JOptionPane.showMessageDialog(this,
                            "Input is invalid. (Max depth 50m)",
                            "Input error",
                            JOptionPane.ERROR_MESSAGE);
                }

            } else {
                System.out.println("Invalid input entered.");
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex.getMessage());
        } catch (ParseException ex) {
            System.out.println("ParseException: " + ex.getMessage());
        }
    }//GEN-LAST:event_targetDistanceTextFieldActionPerformed

    private void wingAngTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_wingAngTextFieldActionPerformed
        try {
            wingAngTextField.commitEdit();
            if (wingAngTextField.getText() != null && isNumeric(wingAngTextField.getText())) {
                double newSetpoint;
                try {
                    newSetpoint = Double.parseDouble(wingAngTextField.getText());
                } catch (ClassCastException ex) {
                    Long newSetpointLong = Long.parseLong(wingAngTextField.getText());
                    newSetpoint = newSetpointLong.doubleValue();
                }

                if (newSetpoint <= 30 && newSetpoint >= -30) {
                    //wingLabel.setBackground(new Color(39, 44, 50));
                    //                    previousSetpoint = setpoint;
                    //                    setpoint = newSetpoint;
                    //                    depthInputTextField.setValue(null);
                    wingAngTextField.setText(newSetpoint + "");
                    System.out.println("wingAng set to " + String.valueOf(newSetpoint));
                    this.client_ROV.sendCommand("cmd_wingAng:" + String.valueOf(newSetpoint));
                } else {
                    wingAngTextField.setValue(null);
                    wingAngTextField.setText("");
                    JOptionPane.showMessageDialog(this,
                            "Input is invalid. (Set a value ranging from -30 to 30)",
                            "Input error",
                            JOptionPane.ERROR_MESSAGE);
                }

            } else {
                System.out.println("Invalid input entered.");
            }
        } catch (IOException ex) {
            System.out.println("IOException: " + ex.getMessage());
        } catch (ParseException ex) {
            System.out.println("ParseException: " + ex.getMessage());
        }
    }//GEN-LAST:event_wingAngTextFieldActionPerformed

    Action exitFullscreenAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            exitFullscreenButton.doClick();
        }
    };

//    Action sendInputAction = new AbstractAction()
//    {
//        public void actionPerformed(ActionEvent e)
//        {
//            if (depthInputTextField.isFocusOwner())
//            {
//                //sendButton.doClick();
//            }
//
//        }
//    };
    /**
     * Checks if the given string is numeric
     *
     * @param string the given string to check
     * @return true if the given string is numeric, false if not
     */
    public static boolean isNumeric(String string) {
        boolean numeric = true;
        try {
            Double num = Double.parseDouble(string);
        } catch (NumberFormatException e) {
            numeric = false;
        }
        return numeric;
    }

    /**
     * Checks if the given string is integer
     *
     * @param str the given string to check
     * @return true if the given string is integer, false if not
     */
    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        if (str.isEmpty()) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (1 == str.length()) {
                return false;
            }
            i = 1;
        }
        for (; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton InputControllerButton;
    private javax.swing.JPanel actuatorPanel1;
    private javax.swing.JPanel actuatorPanel2;
    private javax.swing.JLabel actuatorSBPosLabel1;
    private javax.swing.JPanel background;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JMenuItem calibrateMenuItem;
    private javax.swing.JPanel cameraControlPanel;
    private javax.swing.JLabel cameraHeader;
    private javax.swing.JLabel cameraOffsetLabel;
    private javax.swing.JFormattedTextField cameraOffsetTextField;
    private javax.swing.JPanel cameraPanel;
    private javax.swing.JPanel cameraPanel1;
    private javax.swing.JSlider cameraPitchSlider;
    private javax.swing.JPanel controlPanel;
    private javax.swing.JFormattedTextField delayTextField;
    private javax.swing.JLabel depthHeader;
    private javax.swing.JRadioButton depthModeButton;
    private javax.swing.JPanel depthPanel;
    private javax.swing.JLabel emergencyHeader;
    private javax.swing.JPanel emergencyPanel;
    private javax.swing.JButton emergencyStopButton;
    private javax.swing.JButton exitFullscreenButton;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JFrame fullscreen;
    private javax.swing.JButton fullscreenButton;
    private javax.swing.JLabel headingLabel;
    private javax.swing.JButton helpFrameOKbutton;
    private javax.swing.JFrame helpframe;
    private javax.swing.JLabel humidityLabel;
    private javax.swing.JLabel i2cErrorLabel;
    private javax.swing.JLabel imageNumberLabel;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JLabel insideTempLabel;
    private javax.swing.JDesktopPane jDesktopPane1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuItem jMenuAbout;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuCalibrate;
    private javax.swing.JMenu jMenuConnect;
    private javax.swing.JMenuItem jMenuEchosounder;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuIOController;
    private javax.swing.JMenuItem jMenuItemConnect;
    private javax.swing.JMenuItem jMenuItemDisconnect;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemStartLogging;
    private javax.swing.JMenuItem jMenuItemStopLogging;
    private javax.swing.JMenu jMenuLogger;
    private javax.swing.JMenuItem jMenuOptions;
    private javax.swing.JMenu jMenuPing;
    private javax.swing.JMenu jMenuRovReady;
    private javax.swing.JMenu jMenuTools;
    private javax.swing.JMenu jMenuVoltage;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JLabel latitudeLabel;
    private javax.swing.JLabel leakLabel;
    private javax.swing.JLabel lightHeader;
    private javax.swing.JPanel lightPanel;
    private javax.swing.JSlider lightSlider;
    private javax.swing.JToggleButton lightSwitch;
    private javax.swing.JLabel lightSwitch_lbl;
    private javax.swing.JToggleButton lockButton;
    private javax.swing.JLabel longitudeLabel;
    private javax.swing.JToggleButton manualControlButton;
    private javax.swing.JLabel outsideTempLabel;
    private javax.swing.JToggleButton photoModeButton;
    private javax.swing.JLabel photoModeDelayLabel;
    private javax.swing.JLabel photoModeDelay_FB_Label;
    private javax.swing.JLabel pitchLabel;
    private javax.swing.JLabel pressureLabel;
    private javax.swing.JButton resetManualControlButton;
    private javax.swing.JLabel rollLabel;
    private javax.swing.JLabel rovDepthLabel;
    private javax.swing.JLabel seafloorDepthBoatLabel;
    private javax.swing.JLabel seafloorDepthRovLabel;
    private javax.swing.JRadioButton seafloorModeButton;
    private javax.swing.JLabel setpointLabel;
    private javax.swing.JLabel stepperHeader1;
    private javax.swing.JLabel stepperHeader2;
    private javax.swing.JFormattedTextField targetDistanceTextField;
    private javax.swing.JLabel warningLabel1;
    private javax.swing.JLabel warningLabel2;
    private javax.swing.JLabel winAngLabel;
    private javax.swing.JPanel window;
    private javax.swing.JProgressBar wingAngPSPosBar;
    private javax.swing.JFormattedTextField wingAngTextField;
    private javax.swing.JLabel wingAnglePSPosLabel;
    private javax.swing.JProgressBar wingAngleSBPosBar;
    private javax.swing.JLabel wingAngleSBPosLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Runs the ROV frame thread.
     */
    @Override
    public void run() {
        try {
            videoImage = ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/TowedROV.jpg"));
            data.setVideoImage(videoImage);
        } catch (IOException ex) {
            Logger.getLogger(NTNUSubseaGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.showImage(videoImage);
        this.setVisible(true); //To change body of generated methods, choose Tools | Templates.
        this.showImage(videoImage);

//        try {
//            Thread.sleep(10);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(NTNUSubseaGUI.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    /**
     * Updates the GUI by observing the shared resource Data class.
     *
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {
        //actuatorDutyCycleBar1.setValue(data.getBarValue());
        //System.out.println(data.getPitchAngle());
        System.out.println("12345678");
        if (data.getVideoImage() != null) {
            this.showImage(data.getVideoImage());
        }
        if (this.sounderThread == null) {
            this.sounderThread = new Thread(this.sounder);
        }
        if (data.isEmergencyMode() && !this.sounderThread.isAlive()) {

            targetDistanceTextField.setText("0.00");
            setpointLabel.setText("EMERGENCY STOP: " + targetDistanceTextField.getText() + "m");
            setpointLabel.setBackground(new Color(255, 0, 0));
            manualControlButton.doClick();
            try {
                this.client_ROV.sendCommand("cmd_targetMode:2");
                this.client_ROV.sendCommand("cmd_angle:30");
            } catch (IOException ex) {
                System.out.println("IOException in emergencyStopButtonActionPerformed: " + ex.getMessage());
            }

            if (!this.sounder.isAlive()) {
                this.sounderThread = new Thread(this.sounder);
                this.sounderThread.setName("Sounder");
            }
            this.sounderThread.start();
        }

        rollLabel.setText("Roll Angle: " + data.getRollAngle());
        pitchLabel.setText("Pitch Angle: " + data.getPitchAngle());

        seafloorDepthBoatLabel.setText("Beneath Boat: " + String.valueOf(df2.format(data.getDepthBeneathBoat())) + "m");
        seafloorDepthRovLabel.setText("Beneath ROV: " + data.getDepthBeneathRov() + "m");
        rovDepthLabel.setText("ROV Depth: " + data.getDepth() + "m");
        System.out.println("dsfk12345sdkf");
        headingLabel.setText("Heading: " + data.getGPSAngle());
        longitudeLabel.setText("Longitude: " + data.getLongitude());
        latitudeLabel.setText("Latitude: " + data.getLatitude());

        wingAnglePSPosLabel.setText("Port Angle: " + data.getWingAnglePort());
        wingAngleSBPosLabel.setText("SB Angle: " + data.getWingAngleSb());
        wingAngPSPosBar.setValue((int) data.getWingAnglePort());
        wingAngleSBPosBar.setValue((int) data.getWingAngleSb());

        if (data.isI2cError()) {
            i2cErrorLabel.setText("I²C: ERROR!");
            i2cErrorLabel.setBackground(Color.red);
            data.setEmergencyMode(true);
        } else {
            i2cErrorLabel.setText("I²C: OK");
        }
        System.out.print(data.getLeakStatus());
        if (data.getLeakStatus()) {

            leakLabel.setText("LEAK DETECTED!");
            leakLabel.setBackground(Color.red);
            data.setEmergencyMode(true);
        } else {
            leakLabel.setText("No leak detected");
            leakLabel.setBackground(new Color(39, 46, 54));
        }
        outsideTempLabel.setText("Outside Temp: " + data.getOutsideTemp() + " C");
        insideTempLabel.setText("Inside Temp: " + data.getInsideTemp() + "C");
        humidityLabel.setText("Rel. Humidity: " + data.getHumidity() + "%");
        System.out.println("dsfksndfmskdmfksdkf");
        pressureLabel.setText("Pressure: " + (data.getPressure() + " mBar"));
        rovDepthLabel.setText("ROV Depth: " + data.getRovDepth() + "m");

        if (data.isRovReady()) {
            try {
                jMenuRovReady.setText("ROV Ready!");
                jMenuRovReady.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/Calibrated.gif"))));
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        } else {
            try {
                jMenuRovReady.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }

        if (data.getVoltage() < 28.00 && data.getVoltage() > 25.00) {
            try {
                jMenuVoltage.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
            data.setEmergencyMode(true);
        } else if (data.getVoltage() > 28.00) {
            jMenuVoltage.setText("Voltage: " + data.getVoltage() + " V");
            try {
                jMenuVoltage.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/Calibrated.gif"))));
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }

        if (this.client_Pinger.isConnected() && (data.getRovPing() == 0.00)) {
            data.setEmergencyMode(true);
            jMenuPing.setText("Ping (ROV): Lost connection...");
            try {
                jMenuPing.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/NotCalibrated.gif"))));
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        } else if (this.client_Pinger.isConnected() && (data.getRovPing() != 999.99)) {
            jMenuPing.setText("Ping (ROV): " + String.valueOf(data.getRovPing()) + " ms");
            try {
                jMenuPing.setIcon(new ImageIcon(ImageIO.read(getClass().getResource("/ntnusubsea/gui/Images/Calibrated.gif"))));
            } catch (IOException ex) {
                System.out.println("Error: " + ex.getMessage());
            }
        }

        photoModeDelay_FB_Label.setText(String.valueOf(df2.format(data.getPhotoModeDelay_FB())) + " s");
        imageNumberLabel.setText(data.getImageNumber() + "");

//        actuatorControlPS.setValue(data.getFb_actuatorPSPos);
//        actuatorControlSB.setValue(data.getFb_actuatorSBPos);
//        actuatorPosLabel.setText("<html>PS: " + String.valueOf(actuatorControlPS.getValue()) + "<br/><br/>SB: " + String.valueOf(actuatorControlSB.getValue()));
    }
}
