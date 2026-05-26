package com.dgx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.*;

public class Simulation extends JFrame {
    int anzFz = 10; // number of cars (Anzahl Fahrzeuge)
    boolean isConsuming = false; // state when the vehicles are consuming the target
    boolean isDispersing = false; // state when the vehicles are done consuming the target
    long consumptionStartTime = 0; // timer after when the vehicles start consume the target
    long dispersalStartTime = 0; // timer for the dispersion of the vehicle

    Canvas myCanvas; // The Canvas (die Leinwand)
    static int sleep = 8; // delay in frame
    static double pix = 0.3; // the scaling factor
    double[] currentTarget = null;

    int numObstacles = 0;// position of the current target

    ArrayList<Vehicle> allVehicles = new ArrayList<>(); // Array of vehicles
    ArrayList<Obstacle> allObstacles = new ArrayList<>(); // Array of Obstacles

    Simulation() {
        setTitle("Die Schwarmintelligenz");

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

                int numObstacles = Integer.parseInt(reader.readLine());

                System.out.println("Number of Obstacles: "+numObstacles);

                for (int i = 0; i < numObstacles; i++) {

                    double[] obs_pos = new double[2];
                    String line = reader.readLine();

                    String[] parts = line.split(" ");

                    obs_pos[0] = Integer.parseInt(parts[0]);
                    obs_pos[1] = Integer.parseInt(parts[1]);

                    double obs_width = Double.parseDouble(parts[2]);
                    double obs_height = Double.parseDouble(parts[3]);

                    allObstacles.add(new Obstacle(obs_pos, obs_width, obs_height));
                }

                reader.close();

                System.out.println("Obstacles Position Extracted");

                allObstacles.forEach(arr -> System.out.print(Arrays.toString(arr.position)+"\t"));

            } else {
                System.out.println("No text file for obstacles found!");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("\nObstacles Generated");

        int width = 1500;
        int height = 1500;

        myCanvas = new Canvas(allVehicles, pix, allObstacles, width, height);

        add(myCanvas);

        setSize(1500, 1000 );

        setVisible(true);

        spawnNextTarget();

        myCanvas.updateTarget(currentTarget, isConsuming);

        new Timer(sleep, e -> {
            checkTargetStatus();
            myCanvas.updateTarget(currentTarget, isConsuming);
            for (Vehicle v : allVehicles) {
//                System.out.println(currentTarget[0]+","+currentTarget[1]);
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

            // 2. Check if it's inside an obstacle
            for (Obstacle obs : allObstacles) {
                double d = Math.sqrt(Math.pow(currentTarget[0] - obs.position[0], 2) +
                        Math.pow(currentTarget[1] - obs.position[1], 2));

                // Check if distance is less than half-width (10) + small buffer[cite: 4]
                if (d < 15) {
                    invalidLocation = true;
                    break;
                }
            }

            if (attempts > 100) break;

        } while (invalidLocation);

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