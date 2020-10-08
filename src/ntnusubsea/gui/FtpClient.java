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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Creates an instance of the FtpClient class. It has the responsibility of
 * retrieving the images from the camera RPi.
 * @author Towed ROV 2019 https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class FtpClient implements Runnable {

    private String server;
    private int port;
    private String user;
    private String password;
    private FTPClient ftp;

    /**
     * Constructor used to create the FtpClient.
     *
     * @param IP The IP of the FTP server to connect to.
     */
    public FtpClient(String IP) {
        this.server = IP; //The IP address for the camera RPi.
        this.port = 21; // The FTP port
        this.user = "pi";
        this.password = "";

    }

    /**
     * Runs the FtpClient thread.
     */
    @Override
    public void run() {
        //this.open();
        while (true) {
            // be alive
        }
    }

    /**
     * Opens a connection to the FTP server.
     */
    public void open() {
        try {
            ftp = new FTPClient();

            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));

            ftp.connect(server, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                throw new IOException("Exception in connecting to FTP Server");
            }

            ftp.login(user, password);
        } catch (IOException ex) {
            System.out.println("IOException in FtpClient.open(): " + ex.getMessage());
        }

    }

    /**
     * Returns a list of all the files in the given path.
     *
     * @param path the path to list from
     * @return a list of all the files in the given path.
     */
    public Collection<String> getFileList(String path) {
        Collection<String> fileList = null;
        try {
            FTPFile[] files = ftp.listFiles(path);
            fileList = Arrays.stream(files)
                    .map(FTPFile::getName)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return fileList;
    }

    /**
     * Downloads every file from the given folder path.
     *
     * @param source the source
     * @param destination the destination path of the image
     * @param folderPath the folder path of the images
     */
    public void downloadFile(String source, String destination, String folderPath) {
        try {
            Path destinationPath = Paths.get(folderPath);
            if (Files.notExists(destinationPath)) {
                boolean success = (new File(folderPath)).mkdirs();
                if (!success) {
                    System.out.println("Directory creation failed!");
                } else {
                    System.out.println("Directory created at " + destination);
                }
            }
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            FileOutputStream out = new FileOutputStream(destination);
            ftp.retrieveFile(source, out);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Disconnects from the FTP server.
     *
     */
    public void disconnect() {
        if (ftp.isConnected()) {
            try {
                ftp.disconnect();
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    /**
     * Closes the FTP connection.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        try {
            ftp.disconnect();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
