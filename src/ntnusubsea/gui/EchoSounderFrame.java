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

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Frame to display a graph panel
 *
 * @author Towed ROV 2019
 * https://ntnuopen.ntnu.no/ntnu-xmlui/handle/11250/2564356
 */
public class EchoSounderFrame extends javax.swing.JFrame implements Runnable, Observer {

    private Data data;
    private XYPlot plot;

    /**
     * Creates new form SonarFrame
     *
     * @param data Data containing shared variables
     */
    public EchoSounderFrame(Data data) {
        super("XY Line Chart Example with JFreechart");
        initComponents();
        this.data = data;
        JPanel chartPanel = createChartPanel();

        //setSize(640, 480);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);

        chartPanel.setSize(jPanel1.getWidth(), jPanel1.getHeight());
        chartPanel.setVisible(true);
        jPanel1.add(chartPanel, BorderLayout.CENTER);
        this.add(jPanel1);
        this.pack();
    }

    /**
     * Creates the chart panel.
     *
     * @return the chart panel
     */
    private JPanel createChartPanel() {
        String chartTitle = "ROV Depth Chart";
        String xAxisLabel = "Time (s)";
        String yAxisLabel = "Depth";
        XYDataset dataset = createDatasetLive();
        JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
                xAxisLabel, yAxisLabel, dataset);
        plot = chart.getXYPlot();
        NumberAxis yaxis = (NumberAxis) plot.getRangeAxis();
        yaxis.setRange(-3.0, 50.0);
        plot.getRangeAxis().setInverted(true);

        plot.setBackgroundPaint(Color.DARK_GRAY);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        return new ChartPanel(chart);
    }

    /**
     * Creates the dataset from a dataset file.
     *
     * @return the dataset from a dataset file
     */
    private XYDataset createDatasetFromFile() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("Seafloor");
        XYSeries series2 = new XYSeries("ROV Depth");
        XYSeries series3 = new XYSeries("Surface");

        try {
            InputStream depthtoseafloorList = new FileInputStream(new File("C:\\depthtoseafloor.txt"));
            InputStream rovdepthList = new FileInputStream(new File("C:\\rovdepth.txt"));
            BufferedReader reader1 = new BufferedReader(new InputStreamReader(depthtoseafloorList));
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(rovdepthList));

            String line = "";
            int i = 0;
            double t = 0.0;
            while ((line = reader1.readLine()) != null) {
                series1.add(t, Double.parseDouble(line));
                series3.add(t, 0.01);
                i++;
                t = t + 0.1;
            }
            i = 0;
            t = 0.0;
            while ((line = reader2.readLine()) != null) {
                series2.add(t, Double.parseDouble(line) * -1);
                i++;
                t = t + 0.1;
            }

        } catch (Exception e) {
            System.out.println("error");
        }

        dataset.addSeries(series1);
        dataset.addSeries(series2);
        dataset.addSeries(series3);

        return dataset;
    }

    /**
     * Creates the dataset from the depth data.
     *
     * @return the dataset from the depth data.
     */
    private XYDataset createDatasetLive() {
        double targetDistance;
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series1 = new XYSeries("ROV Depth");
        XYSeries series2 = new XYSeries("Boat Seafloor");
        XYSeries series3 = new XYSeries("Surface");
        XYSeries series4 = new XYSeries("Target Distance");
        XYSeries series5 = new XYSeries("ROV Seafloor");

        Iterator it = data.rovDepthDataList.iterator();
        while (it.hasNext()) {
            String s = it.next().toString();
            String[] data = s.split(":");
            series1.add(Double.parseDouble(data[0]), Double.parseDouble(data[1]));

        }

        Iterator it2 = data.depthBeneathBoatDataList.iterator();
        while (it2.hasNext()) {
            String s = it2.next().toString();
            String[] data = s.split(":");
            series2.add(Double.parseDouble(data[0]), Double.parseDouble(data[1]));
            series3.add(Double.parseDouble(data[0]), 0.01);
        }

        Iterator it3 = data.targetDistanceDataList.iterator();
        while (it3.hasNext()) {
            String s = it3.next().toString();
            String[] data = s.split(":");
            series4.add(Double.parseDouble(data[0]), Double.parseDouble(data[1]));
        }
        Iterator it4 = data.rovDepthBeneathDataList.iterator();
        while (it4.hasNext()) {
            String s = it4.next().toString();
            String[] data = s.split(":");
            series5.add(Double.parseDouble(data[0]), Double.parseDouble(data[1]));
        }

        dataset.addSeries(series1);
        dataset.addSeries(series2);
        dataset.addSeries(series3);
        dataset.addSeries(series4);
        dataset.addSeries(series5);
        return dataset;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Echo sounder");
        setBackground(new java.awt.Color(39, 44, 50));
        setMinimumSize(new java.awt.Dimension(1200, 600));
        setSize(new java.awt.Dimension(1200, 600));

        jPanel1.setBackground(new java.awt.Color(39, 44, 50));
        jPanel1.setForeground(new java.awt.Color(39, 44, 50));
        jPanel1.setMinimumSize(new java.awt.Dimension(1200, 600));
        jPanel1.setPreferredSize(new java.awt.Dimension(1200, 600));
        jPanel1.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                jPanel1AncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        jPanel1.setLayout(new java.awt.BorderLayout());
        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jMenu1.setText("File");

        jMenuItem1.setText("Exit");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Tools");

        jMenuItem2.setText("Calibrate");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        this.dispose();
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        String cableLength = (String) JOptionPane.showInputDialog(this, "Enten current cable length (Meters)", "Calibration", JOptionPane.PLAIN_MESSAGE, null, null, "100.000");
        try {
            System.out.println(Float.valueOf(cableLength));
        } catch (NumberFormatException | NullPointerException ex) {
            System.out.println("Invalid or no input");
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jPanel1AncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_jPanel1AncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_jPanel1AncestorAdded

    /**
     * Runs the EchoSounderFrame thread.
     */
    @Override
    public void run() {

        DecimalFormat df = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.US));
        df.setMaximumFractionDigits(2);
        double time = 0.0;
        double time2 = time + data.getTimeBetweenBoatAndRov();
        String rovDepthValue = "0.0";
        String rovDepthBeneath = "0.0";
        String depthBeneathBoatValue = "0.0";
        String targetDistance = "0.0";
        double counter = 0;
        double counter2 = 25.0;
        double amount = 1.0;
        double amount2 = 0.1;

        while (true) {
            data.updateRovDepthDataList(String.valueOf(df.format(time)), rovDepthValue);
            data.updateRovDepthBeneathDataList(String.valueOf(df.format(time)), rovDepthBeneath);
            data.updateDepthBeneathBoatDataList(String.valueOf(df.format(time2)), depthBeneathBoatValue);
            data.updateTargetDistanceDataList(String.valueOf(df.format(time2)), targetDistance);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(EchoSounderFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
            time = time + 0.1;
            time2 = time + data.getTimeBetweenBoatAndRov();
//            rovDepthValue = String.valueOf(df.format(-data.getRovDepth()));
//            depthBeneathBoatValue = String.valueOf(df.format(-data.getDepthBeneathBoat()));
            targetDistance = String.valueOf(df.format(data.getTargetDistance()));

            //-------------------------------------------------------------------------
            // COMMENT OUT THIS SECTION AND UNCOMMENT THE TWO LINES ABOVE TO GET REAL DATA FROM THE ROV
            counter = (counter + amount) * 1.05;
            counter2 = counter2 + amount2;

            depthBeneathBoatValue = String.valueOf(df.format(counter2));
            rovDepthValue = String.valueOf(df.format(counter));
            rovDepthBeneath = String.valueOf(df.format(counter + 20));

            if (counter >= 22.0) {
                counter = 20.0;
            }
            if (counter >= 20.0) {
                amount = +1.0;
            } else if (counter <= 0.0) {
                amount = -1.0;
            }

            if (counter2 >= 27.0) {
                counter2 = 25.0;
            }
            if (counter2 >= 26.0) {
                amount2 = 0.1;
            } else if (counter2 <= 24.0) {
                amount2 = -0.1;
            }
            // -----------------------------------------------------------------------

            try {

                XYDataset tt = createDatasetLive();
                this.plot.setDataset(tt);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    // End of variables declaration//GEN-END:variables

    /**
     * This is from last year. Not used.
     *
     * @param o
     * @param arg
     */
    @Override
    public void update(Observable o, Object arg) {
//        int depth = Math.round(data.getDepth() * 100);
//         refreshGraph(0, depth);
    }
}
