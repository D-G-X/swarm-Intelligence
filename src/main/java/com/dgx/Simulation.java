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
import java.awt.GridLayout;
import java.awt.FlowLayout;

public class Simulation extends JFrame {

    static final double SPAWN_POINT_RADIUS = 40.0;

    int anzFz = 30; // number of cars (Anzahl Fahrzeuge)
    boolean isConsuming = false; // state when the vehicles are consuming the target
    boolean isDispersing = false; // state when the vehicles are done consuming the target
    long consumptionStartTime = 0; // timer after when the vehicles start consume the target
    long dispersalStartTime = 0; // timer for the dispersion of the vehicle
    double targetDetectionRadius = 15.0; // radius used to detect the target
    long targetSearchStartTime = 0; // time when the current target spawned
    long targetSearchElapsedMillis = 0; // live search timer for the current target
    long lastCaptureMillis = -1; // last time the swarm took to find a target

    Canvas myCanvas; // The Canvas (die Leinwand)
    static final int WIDTH = 1475;
    static final int HEIGHT = 800;
    final int WORLD_MARGIN = 10;
    final int WORLD_BORDER_WIDTH = 2;
    static int sleep = 8; // delay in frame
    static double pix = 0.4; // the scaling factor
    double[] currentTarget = null;

    int numObstacles = 0;// position of the current target

    ArrayList<Vehicle> allVehicles = new ArrayList<>(); // Array of vehicles
    ArrayList<Obstacle> allObstacles = new ArrayList<>(); // Array of Obstacles
    ArrayList<BlackHole> allBlackHoles = new ArrayList<>(); // Array of Black Holes

    Simulation() {
        setTitle("Die Schwarmintelligenz");
        System.out.println("\"Die Schwarmintelligenz\"");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

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
                    String obs_name = parts[4];

                    // 2. Define your new centered world boundary thresholds for 1500x1500px window
                    double maxX = WIDTH - obs_width;
                    double maxY = HEIGHT - obs_height;

                    obs_pos[0] = Math.max(WORLD_MARGIN, Math.min(parsedX, maxX));
                    obs_pos[1] = Math.max(WORLD_MARGIN, Math.min(parsedY, maxY));

                    allObstacles.add(new Obstacle(obs_pos, obs_width, obs_height, obs_name));
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

        // Extract Black Holes
        System.out.println("Extracting Black Holes Positions");
        try{
            InputStream bhInput = getClass().getClassLoader().getResourceAsStream("blackholes.txt");
            if (bhInput != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(bhInput));
                int numBH = Integer.parseInt(reader.readLine());
                System.out.println("Number of BlackHoles: "+numBH);
                for (int i = 0; i < numBH; i++) {
                    double[] bh_pos = new double[2];
                    String line = reader.readLine();
                    String[] parts = line.split(" ");
                    double parsedX = Integer.parseInt(parts[0]);
                    double parsedY = Integer.parseInt(parts[1]);
                    double bh_radius = Double.parseDouble(parts[2]);
                    String bh_name = parts.length > 3 ? parts[3] : "";

                    double maxX = WIDTH;
                    double maxY = HEIGHT;

                    bh_pos[0] = Math.max(WORLD_MARGIN, Math.min(parsedX, maxX));
                    bh_pos[1] = Math.max(WORLD_MARGIN, Math.min(parsedY, maxY));

                    allBlackHoles.add(new BlackHole(bh_pos, bh_radius, bh_name));
                }
                reader.close();
                System.out.println("Black Holes Position Extracted");
            } else {
                System.out.println("No text file for blackholes found!");
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        System.out.println("Generating Vehicles");

        for (int k = 0; k < anzFz; k++) {
            Vehicle car = new Vehicle();
            car.type = 1; // type 1 has visible boundary
            placeVehicleInSpawnCircle(car);
            allVehicles.add(car);
        }

        System.out.println("Vehicles Generated");

        myCanvas = new Canvas(allVehicles, pix, allObstacles, allBlackHoles, WIDTH, HEIGHT);

        myCanvas.setWorld_margin(WORLD_MARGIN);
        myCanvas.setWorld_border_thickness(WORLD_BORDER_WIDTH);

        // Layout: canvas center, controls at bottom
        getContentPane().setLayout(new BorderLayout());
        add(myCanvas, BorderLayout.CENTER);

        // Control panel arranged as two clean rows
        JPanel controlPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Control Panel"),
            BorderFactory.createEmptyBorder(8, 8, 10, 8)));

        JPanel sliderRow = new JPanel(new GridLayout(1, 7, 6, 0));
        JPanel toggleRow = new JPanel(new GridLayout(1, 4, 6, 0));

        controlPanel.add(sliderRow);
        controlPanel.add(toggleRow);

        java.util.function.BiFunction<JLabel, JSlider, JPanel> controlTile = (label, slider) -> {
            JPanel tile = new JPanel(new BorderLayout(4, 4));
            tile.add(label, BorderLayout.NORTH);
            tile.add(slider, BorderLayout.CENTER);
            return tile;
        };

        final JLabel lblSearchTime = new JLabel("Search time: 0.0 s");
        final JLabel lblLastCapture = new JLabel("Last capture: --");
        Runnable refreshTimerLabels = () -> {
            lblSearchTime.setText(String.format("Search time: %.1f s", targetSearchElapsedMillis / 1000.0));
            lblLastCapture.setText(lastCaptureMillis >= 0
                ? String.format("Last capture: %.1f s", lastCaptureMillis / 1000.0)
                : "Last capture: --");
        };

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
        sliderRow.add(controlTile.apply(lblRadius, sliderRadius));

        // 2) Avoidance multiplier slider (0.0 - 10.0 mapped to 0 - 100)
//        JLabel lblMult = new JLabel("AvoidMult: " + Vehicle.AVOIDANCE_MULTIPLIER);
//        JSlider sliderMult = new JSlider(0, 100, (int)Math.round(Vehicle.AVOIDANCE_MULTIPLIER * 10));
//        sliderMult.setMajorTickSpacing(25);
//        sliderMult.addChangeListener(new ChangeListener() {
//            public void stateChanged(ChangeEvent e) {
//                Vehicle.AVOIDANCE_MULTIPLIER = sliderMult.getValue() / 10.0;
//                lblMult.setText("AvoidMult: " + String.format("%.1f", Vehicle.AVOIDANCE_MULTIPLIER));
//                myCanvas.repaint();
//            }
//        });
//        controlPanel.add(controlTile.apply(lblMult, sliderMult));

        // 3) Obstacle weight slider (0.0 - 2.0 mapped to 0 - 200)
        JLabel lblWeight = new JLabel("F_OBS: " + Vehicle.OBS_WEIGHT);
        JSlider sliderWeight = new JSlider(0, 200, (int)Math.round(Vehicle.OBS_WEIGHT * 100));
        sliderWeight.setMajorTickSpacing(50);
        sliderWeight.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Vehicle.OBS_WEIGHT = sliderWeight.getValue() / 100.0;
                lblWeight.setText("F_OBS: " + String.format("%.2f", Vehicle.OBS_WEIGHT));
                myCanvas.repaint();
            }
        });
        sliderRow.add(controlTile.apply(lblWeight, sliderWeight));

        // 4) Cohesion weight slider (0.0 - 2.0 mapped to 0 - 200)
        JLabel lblZus = new JLabel("F_zus: " + String.format("%.2f", Vehicle.F_ZUS_WEIGHT));
        JSlider sliderZus = new JSlider(0, 200, (int)Math.round(Vehicle.F_ZUS_WEIGHT * 100));
        sliderZus.setMajorTickSpacing(50);
        sliderZus.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Vehicle.F_ZUS_WEIGHT = sliderZus.getValue() / 100.0;
                lblZus.setText("F_zus: " + String.format("%.2f", Vehicle.F_ZUS_WEIGHT));
                myCanvas.repaint();
            }
        });
        sliderRow.add(controlTile.apply(lblZus, sliderZus));

        // 5) Separation weight slider (0.0 - 2.0 mapped to 0 - 200)
        JLabel lblSep = new JLabel("F_sep: " + String.format("%.2f", Vehicle.F_SEP_WEIGHT));
        JSlider sliderSep = new JSlider(0, 200, (int)Math.round(Vehicle.F_SEP_WEIGHT * 100));
        sliderSep.setMajorTickSpacing(50);
        sliderSep.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Vehicle.F_SEP_WEIGHT = sliderSep.getValue() / 100.0;
                lblSep.setText("F_sep: " + String.format("%.2f", Vehicle.F_SEP_WEIGHT));
                myCanvas.repaint();
            }
        });
        sliderRow.add(controlTile.apply(lblSep, sliderSep));

        // 6) Alignment weight slider (0.0 - 2.0 mapped to 0 - 200)
        JLabel lblAus = new JLabel("F_aus: " + String.format("%.2f", Vehicle.F_AUS_WEIGHT));
        JSlider sliderAus = new JSlider(0, 200, (int)Math.round(Vehicle.F_AUS_WEIGHT * 100));
        sliderAus.setMajorTickSpacing(50);
        sliderAus.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Vehicle.F_AUS_WEIGHT = sliderAus.getValue() / 100.0;
                lblAus.setText("F_aus: " + String.format("%.2f", Vehicle.F_AUS_WEIGHT));
                myCanvas.repaint();
            }
        });
        sliderRow.add(controlTile.apply(lblAus, sliderAus));

        // 7) Target detection radius slider (1 - 50)
        JLabel lblDetect = new JLabel("Target DetectRadius: " + String.format("%.1f", targetDetectionRadius));
        JSlider sliderDetect = new JSlider(1, 50, (int)Math.round(targetDetectionRadius));
        sliderDetect.setMajorTickSpacing(10);
        sliderDetect.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                targetDetectionRadius = sliderDetect.getValue();
                lblDetect.setText("Target DetectRadius: " + String.format("%.1f", targetDetectionRadius));
                myCanvas.repaint();
            }
        });
        sliderRow.add(controlTile.apply(lblDetect, sliderDetect));

        // 8) Toggle obstacle avoidance radius visualization
        JCheckBox chkRadius = new JCheckBox("Show obstacle radius", false);
        chkRadius.addActionListener(e -> {
            myCanvas.setShowObstacleRadius(chkRadius.isSelected());
            myCanvas.repaint();
        });
        JPanel toggleCell1 = new JPanel(new GridLayout(2, 1, 0, 4));
        toggleCell1.add(chkRadius);

        // 9) Toggle black hole radius visualization
        JCheckBox chkBlackHoleRadius = new JCheckBox("Show black hole radius", false);
        chkBlackHoleRadius.addActionListener(e -> {
            myCanvas.setShowBlackHoleRadius(chkBlackHoleRadius.isSelected());
            myCanvas.repaint();
        });
        toggleCell1.add(chkBlackHoleRadius);
        toggleRow.add(toggleCell1);

        // 9b) Toggle Q-Table grid overlay
        JCheckBox chkGrid = new JCheckBox("Show Q-Grid", false);
        chkGrid.addActionListener(e -> {
            myCanvas.setShowGrid(chkGrid.isSelected());
            myCanvas.repaint();
        });
        JPanel toggleCell2 = new JPanel(new GridLayout(2, 1, 0, 4));
        toggleCell2.add(chkGrid);

        // 9c) Toggle Q-values overlay
        JCheckBox chkQVals = new JCheckBox("Show Q-Values", false);
        chkQVals.addActionListener(e -> {
            myCanvas.setShowQValues(chkQVals.isSelected());
            myCanvas.repaint();
        });
        toggleCell2.add(chkQVals);
        toggleRow.add(toggleCell2);

        // 10) Toggle target detection radius visualization + show type1 circles
        JCheckBox chkTargetRadius = new JCheckBox("Show target radius", true);
        chkTargetRadius.addActionListener(e -> {
            myCanvas.setShowTargetDetectionRadius(chkTargetRadius.isSelected());
            myCanvas.repaint();
        });

        JCheckBox chkType1Circle = new JCheckBox("Show type1 circle", false);
        chkType1Circle.addActionListener(e -> {
            myCanvas.setShowType1Circle(chkType1Circle.isSelected());
            myCanvas.repaint();
        });

        JPanel toggleCell3 = new JPanel(new GridLayout(2, 1, 0, 4));
        toggleCell3.add(chkTargetRadius);
        toggleCell3.add(chkType1Circle);

        JPanel timerTile = new JPanel(new GridLayout(2, 1, 0, 2));
        timerTile.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        timerTile.add(lblSearchTime);
        timerTile.add(lblLastCapture);
        toggleRow.add(toggleCell3);
        toggleRow.add(timerTile);

        // 11) Manual target regeneration button
        JButton btnNewTarget = new JButton("New Target");
        btnNewTarget.addActionListener(e -> {
            isDispersing = false;
            spawnNextTarget();
            myCanvas.updateTarget(currentTarget, isConsuming, targetDetectionRadius, targetSearchElapsedMillis, lastCaptureMillis);
            refreshTimerLabels.run();
            myCanvas.repaint();
        });
        JPanel buttonTile = new JPanel(new BorderLayout(4, 4));
        buttonTile.add(btnNewTarget, BorderLayout.CENTER);
        sliderRow.add(buttonTile);

        // initialize canvas toggles to match checkbox defaults
        myCanvas.setShowTargetDetectionRadius(chkTargetRadius.isSelected());
        myCanvas.setShowType1Circle(chkType1Circle.isSelected());
        myCanvas.setShowTimer(true);
        refreshTimerLabels.run();

        add(controlPanel, BorderLayout.SOUTH);

        setSize(WIDTH, HEIGHT);

        setVisible(true);

        spawnNextTarget();

        myCanvas.updateTarget(currentTarget, isConsuming, targetDetectionRadius, targetSearchElapsedMillis, lastCaptureMillis);
        refreshTimerLabels.run();

        new Timer(sleep, e -> {
            checkTargetStatus();
            myCanvas.updateTarget(currentTarget, isConsuming, targetDetectionRadius, targetSearchElapsedMillis, lastCaptureMillis);
            refreshTimerLabels.run();

            // Dynamic Swarm Simulation Loop coupled with Q-Learning Brain Updates
            for (Vehicle v : allVehicles) {

                // 1. Capture original continuous state position coordinates
                double oldX = v.pos[0];
                double oldY = v.pos[1];

                // 2. Query the Q-Learning engine for high-level direction guidance
                int action = QEngine.chooseAction(oldX, oldY);

                // 3. Execute vehicle kinematics movement update
                v.move(allVehicles, allObstacles, currentTarget, isConsuming, isDispersing);

                // 4. Evaluate Environment Intersections & Distribute Rewards/Punishments
                double reward = -1.0; // Baseline execution step penalty cost
                boolean terminalStateReached = false;
                boolean hitBlackHole = false;

                // Check Black Hole Collision Intersections
                for (BlackHole bh : allBlackHoles) {
                    double dx = v.pos[0] - bh.position[0];
                    double dy = v.pos[1] - bh.position[1];
                    double distSq = (dx * dx) + (dy * dy);
                    double killRadius = bh.getHole_radius();

                    if (distSq <= (killRadius * killRadius)) {
                        reward = -100.0; // Heavy penalty for dying
                        hitBlackHole = true;
                        terminalStateReached = true;
                        break;
                    }
                }

                // Check Obstacle Intersections
                if (!terminalStateReached) {
                    for (Obstacle obs : allObstacles) {
                        double ox = obs.position[0];
                        double oy = obs.position[1];
                        double ow = obs.getObstacle_width();
                        double oh = obs.getObstacle_height();

                        if (v.pos[0] >= ox && v.pos[0] <= ox + ow &&
                                v.pos[1] >= oy && v.pos[1] <= oy + oh) {
                            reward = -10.0; // Penalty for touching an obstacle
                            v.pos[0] = oldX;
                            v.pos[1] = oldY;
                            v.vel[0] = -v.vel[0] * 0.8;
                            v.vel[1] = -v.vel[1] * 0.8;
                            break;
                        }
                    }
                }

                // Check Target Acquisition Intersection (If not already consumed)
                if (!terminalStateReached && currentTarget != null && !isConsuming) {
                    double dx = v.pos[0] - currentTarget[0];
                    double dy = v.pos[1] - currentTarget[1];
                    double dist = Math.sqrt((dx * dx) + (dy * dy));

                    if (dist < targetDetectionRadius) {
                        reward = 100.0; // Large positive reward for target capture
                        terminalStateReached = true;
                    }
                }

                // 5. Update the crowdsourced collective swarm intelligence Q-Table
                if (currentTarget != null) {
                    QEngine.updateQ(oldX, oldY, action, reward, v.pos[0], v.pos[1], currentTarget[0], currentTarget[1]);
                } else {
                    QEngine.updateQ(oldX, oldY, action, reward, v.pos[0], v.pos[1]);
                }

                // 6. Handle Respawn Operations
                if (hitBlackHole) {
                    placeVehicleInSpawnCircle(v); // Reset position coordinates seamlessly
                }
            }

            repaint();
        }).start();
    }

    void spawnNextTarget() {
        currentTarget = new double[2];
        targetSearchStartTime = System.currentTimeMillis();
        targetSearchElapsedMillis = 0;
        boolean invalidLocation;
        int attempts = 0;

        // Define the same boundaries the vehicles use in position_Box()
        double minX = WORLD_MARGIN + 5;
        double maxX = (WIDTH * pix) - (WORLD_MARGIN + 5);
        double minY = WORLD_MARGIN + 5;
        double maxY = (HEIGHT * pix) - (WORLD_MARGIN + 5);

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

            // 3. Keep the target out of black hole gravity zones plus a 20 unit safety buffer
            for (BlackHole bh : allBlackHoles) {
                if (isInsideBlackHoleBuffer(currentTarget, bh, 20.0)) {
                    invalidLocation = true;
                    break;
                }
            }

            if (attempts > 100) break;

        } while (invalidLocation);

        System.out.println("New Target Position:\t"+currentTarget[0]+","+currentTarget[1]);

        isConsuming = false;
    }

    private boolean isInsideBlackHoleBuffer(double[] target, BlackHole blackHole, double extraBuffer) {
        double dx = target[0] - blackHole.position[0];
        double dy = target[1] - blackHole.position[1];
        double radius = blackHole.getHole_radius() + extraBuffer;
        return (dx * dx) + (dy * dy) <= radius * radius;
    }

    private void placeVehicleInSpawnCircle(Vehicle car) {
        final double spawnRadius = SPAWN_POINT_RADIUS;
        final double spawnCenterX = (WIDTH * pix) - WORLD_MARGIN - spawnRadius;
        final double spawnCenterY = WORLD_MARGIN + spawnRadius;

        boolean invalidLocation;
        int attempts = 0;

        do {
            invalidLocation = false;
            attempts++;

            double angle = 2.0 * Math.PI * Math.random();
            double distance = spawnRadius * Math.sqrt(Math.random());

            car.pos[0] = spawnCenterX + Math.cos(angle) * distance;
            car.pos[1] = spawnCenterY + Math.sin(angle) * distance;

            for (Obstacle obs : allObstacles) {
                double ox = obs.position[0];
                double oy = obs.position[1];
                double ow = obs.getObstacle_width();
                double oh = obs.getObstacle_height();

                boolean insideX = car.pos[0] >= ox && car.pos[0] <= (ox + ow);
                boolean insideY = car.pos[1] >= oy && car.pos[1] <= (oy + oh);

                if (insideX && insideY) {
                    invalidLocation = true;
                    break;
                }
            }

            if (attempts > 100) {
                break;
            }
        } while (invalidLocation);

        double angle = 2.0 * Math.PI * Math.random();
        car.vel[0] = Math.cos(angle) * car.max_vel;
        car.vel[1] = Math.sin(angle) * car.max_vel;
    }

    void checkTargetStatus() {
        if (currentTarget == null) return;

        if (!isConsuming) {
            targetSearchElapsedMillis = System.currentTimeMillis() - targetSearchStartTime;
        } else {
            targetSearchElapsedMillis = 0;
        }

        if (!isConsuming) {
            // If we are currently dispersing, check if 2 seconds have passed to stop
            if (isDispersing && System.currentTimeMillis() - dispersalStartTime > 2000) {
                isDispersing = false;
            }

            double nearestDistance = Double.MAX_VALUE;
            for (Vehicle v : allVehicles) {
                double d = Math.sqrt(Math.pow(v.pos[0] - currentTarget[0], 2) +
                        Math.pow(v.pos[1] - currentTarget[1], 2));
                if (d < nearestDistance) nearestDistance = d;
            }

            if (nearestDistance < targetDetectionRadius) {
                isConsuming = true;
                lastCaptureMillis = targetSearchElapsedMillis;
                targetSearchElapsedMillis = 0;
                consumptionStartTime = System.currentTimeMillis();
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