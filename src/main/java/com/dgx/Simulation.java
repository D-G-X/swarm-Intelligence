package com.dgx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public class Simulation extends JFrame {

    int anzFz = 30; // number of cars (Anzahl Fahrzeuge)
    boolean isConsuming = false; // state when the vehicles are consuming the target
    boolean isDispersing = false; // state when the vehicles are done consuming the target
    long consumptionStartTime = 0; // timer after when the vehicles start consume the target
    long dispersalStartTime = 0; // timer for the dispersion of the vehicle

    Canvas myCanvas; // The Canvas (die Leinwand)
    final int WIDTH = 1475;
    final int HEIGHT = 800;
    final int WORLD_MARGIN = 10;
    final int WORLD_BORDER_WIDTH = 2;
    static int sleep = 8; // delay in frame
    static double pix = 0.4; // the scaling factor
    double[] currentTarget = null;

    int numObstacles = 0;// position of the current target

    ArrayList<Vehicle> allVehicles = new ArrayList<>(); // Array of vehicles
    ArrayList<Obstacle> allObstacles = new ArrayList<>(); // Array of Obstacles

    Simulation() {
        setTitle("Die Schwarmintelligenz");
        System.out.println("\"Die Schwarmintelligenz\"");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        System.out.println("Generating Vehicles");

        for (int k = 0; k < anzFz; k++) {
            Vehicle car = new Vehicle();
            // if (k < 0) car.type = 1; // type 1 has visible boundary
            allVehicles.add(car);
        }

        System.out.println("Vehicles Generated");

        System.out.println("Extracting Obstacles Positions");

        try{
            InputStream input = getClass().getClassLoader().getResourceAsStream("obstacles.txt");

            if (input != null) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(input));

                numObstacles = Integer.parseInt(reader.readLine());

                System.out.println("Number of Obstacles: "+numObstacles);

                for (int i = 0; i < numObstacles; i++) {
                    double[] obs_pos = new double[2];
                    String line = reader.readLine();
                    String[] parts = line.split(" ");

                    // 1. Parse raw positions and dimensions from the file
                    double parsedX = Integer.parseInt(parts[0]);
                    double parsedY = Integer.parseInt(parts[1]);
                    double obs_width = Double.parseDouble(parts[2]);
                    double obs_height = Double.parseDouble(parts[3]);

                    // 2. Define your new centered world boundary thresholds for 1500x1500px window
                    double maxX = WIDTH - obs_width;
                    double maxY = HEIGHT - obs_height;

                    obs_pos[0] = Math.max(WORLD_MARGIN, Math.min(parsedX, maxX));
                    obs_pos[1] = Math.max(WORLD_MARGIN, Math.min(parsedY, maxY));

                    allObstacles.add(new Obstacle(obs_pos, obs_width, obs_height));
                }


                reader.close();

                System.out.println("Obstacles Position Extracted");

                allObstacles.forEach(arr -> System.out.print(Arrays.toString(arr.position)+"\t"));

            } else {
                System.out.println("No text file for obstacles found!");
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("\nObstacles Generated");

        myCanvas = new Canvas(allVehicles, pix, allObstacles, WIDTH, HEIGHT);

        myCanvas.setWorld_margin(WORLD_MARGIN);
        myCanvas.setWorld_border_thickness(WORLD_BORDER_WIDTH);

        // Layout: canvas center, controls at bottom
        getContentPane().setLayout(new BorderLayout());
        add(myCanvas, BorderLayout.CENTER);

        // Control panel with sliders for avoidance tuning
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 1) Base avoidance radius slider (0 - 200)
        JLabel lblRadius = new JLabel("AvoidRadius: " + (int)Vehicle.BASE_AVOIDANCE_RADIUS);
        JSlider sliderRadius = new JSlider(0, 200, (int)Math.round(Vehicle.BASE_AVOIDANCE_RADIUS));
        sliderRadius.setMajorTickSpacing(50);
        sliderRadius.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Vehicle.BASE_AVOIDANCE_RADIUS = sliderRadius.getValue();
                lblRadius.setText("AvoidRadius: " + sliderRadius.getValue());
                myCanvas.repaint();
            }
        });
        controlPanel.add(lblRadius);
        controlPanel.add(sliderRadius);

        // 2) Avoidance multiplier slider (0.0 - 10.0 mapped to 0 - 100)
        JLabel lblMult = new JLabel("AvoidMult: " + Vehicle.AVOIDANCE_MULTIPLIER);
        JSlider sliderMult = new JSlider(0, 100, (int)Math.round(Vehicle.AVOIDANCE_MULTIPLIER * 10));
        sliderMult.setMajorTickSpacing(25);
        sliderMult.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Vehicle.AVOIDANCE_MULTIPLIER = sliderMult.getValue() / 10.0;
                lblMult.setText("AvoidMult: " + String.format("%.1f", Vehicle.AVOIDANCE_MULTIPLIER));
                myCanvas.repaint();
            }
        });
        controlPanel.add(lblMult);
        controlPanel.add(sliderMult);

        // 3) Obstacle weight slider (0.0 - 2.0 mapped to 0 - 200)
        JLabel lblWeight = new JLabel("ObsWeight: " + Vehicle.OBS_WEIGHT);
        JSlider sliderWeight = new JSlider(0, 200, (int)Math.round(Vehicle.OBS_WEIGHT * 100));
        sliderWeight.setMajorTickSpacing(50);
        sliderWeight.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Vehicle.OBS_WEIGHT = sliderWeight.getValue() / 100.0;
                lblWeight.setText("ObsWeight: " + String.format("%.2f", Vehicle.OBS_WEIGHT));
                myCanvas.repaint();
            }
        });
        controlPanel.add(lblWeight);
        controlPanel.add(sliderWeight);

        // 4) Toggle obstacle avoidance radius visualization
        JCheckBox chkRadius = new JCheckBox("Show obstacle radius", false);
        chkRadius.addActionListener(e -> {
            myCanvas.setShowObstacleRadius(chkRadius.isSelected());
            myCanvas.repaint();
        });
        controlPanel.add(chkRadius);

        add(controlPanel, BorderLayout.SOUTH);

        setSize(WIDTH, HEIGHT);

        setVisible(true);

        spawnNextTarget();

        myCanvas.updateTarget(currentTarget, isConsuming);

        new Timer(sleep, e -> {
            checkTargetStatus();
            myCanvas.updateTarget(currentTarget, isConsuming);

            for (Vehicle v : allVehicles) {
                v.move(allVehicles, allObstacles, currentTarget, isConsuming, isDispersing);
            }

            repaint();
        }).start();
    }

    void spawnNextTarget() {
        currentTarget = new double[2];
        boolean invalidLocation;
        int attempts = 0;

        // Define the same boundaries the vehicles use in position_Box()
        double minX = 15; // Slightly inside the 10-unit left wall
        double maxX = (1000 * pix) - 15;
        double minY = 15; // Slightly inside the 10-unit top wall
        double maxY = (700 * pix) - 15;

        do {
            invalidLocation = false;
            attempts++;

            // 1. Generate position strictly within the vehicle's "position_Box" limits
            currentTarget[0] = Math.random() * (maxX - minX) + minX;
            currentTarget[1] = Math.random() * (maxY - minY) + minY;

            // 2. Check if it's inside or too close to any obstacle rectangle
            for (Obstacle obs : allObstacles) {
                double ox = obs.position[0];
                double oy = obs.position[1];
                double ow = obs.getObstacle_width();
                double oh = obs.getObstacle_height();

                // small buffer to avoid spawning exactly on the obstacle edge
                double buffer = 5.0;

                boolean insideX = currentTarget[0] >= (ox - buffer) && currentTarget[0] <= (ox + ow + buffer);
                boolean insideY = currentTarget[1] >= (oy - buffer) && currentTarget[1] <= (oy + oh + buffer);

                if (insideX && insideY) {
                    invalidLocation = true;
                    break;
                }
            }

            if (attempts > 100) break;

        } while (invalidLocation);

        System.out.println("New Target Position:\t"+currentTarget[0]+","+currentTarget[1]);

        isConsuming = false;
    }

    void checkTargetStatus() {
        if (currentTarget == null) return;

        if (!isConsuming) {
            // If we are currently dispersing, check if 2 seconds have passed to stop
            if (isDispersing && System.currentTimeMillis() - dispersalStartTime > 2000) {
                isDispersing = false;
            }

            for (Vehicle v : allVehicles) {
                double d = Math.sqrt(Math.pow(v.pos[0] - currentTarget[0], 2) +
                        Math.pow(v.pos[1] - currentTarget[1], 2));
                if (d < 5) {
                    isConsuming = true;
                    consumptionStartTime = System.currentTimeMillis();
                    break;
                }
            }
        } else {
            if (System.currentTimeMillis() - consumptionStartTime > 3000) {
                // Target finished! Start dispersing before spawning next target
                isDispersing = true;
                dispersalStartTime = System.currentTimeMillis();
                spawnNextTarget();
            }
        }
    }

    public static void main(String[] args) {
        new Simulation();
    }
}