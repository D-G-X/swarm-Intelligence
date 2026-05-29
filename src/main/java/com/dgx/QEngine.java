package com.dgx;

import java.util.Random;

public class QEngine {
    public static final double CELL_SIZE = 10.0;
    private static final double HALF_WORLD_WIDTH = (Simulation.WIDTH * Simulation.pix) / 2.0;
    private static final double HALF_WORLD_HEIGHT = (Simulation.HEIGHT * Simulation.pix) / 2.0;
    public static int Q_WIDTH = (int) Math.ceil((Simulation.WIDTH * Simulation.pix * 2.0) / CELL_SIZE) + 1;
    public static int Q_HEIGHT = (int) Math.ceil((Simulation.HEIGHT * Simulation.pix * 2.0) / CELL_SIZE) + 1;
    public static final int ACTIONS = 4; // Up, Down, Left, Right

    public static double[][][] Q = new double[Q_WIDTH][Q_HEIGHT][ACTIONS];

    public static final double ALPHA = 0.1;
    public static final double GAMMA = 0.9;
    public static final double EPSILON = 0.25;
    public static final Random rand = new Random();

    public static int toGridX(double x) {
        return toGridX(x, 0.0);
    }

    public static int toGridX(double x, double targetX) {
        // Encode the vehicle position relative to the current target.
        int gx = (int) Math.floor(((x - targetX) + HALF_WORLD_WIDTH) / CELL_SIZE);
        return Math.max(0, Math.min(gx, Q_WIDTH-1));
    }

    public static int toGridY(double y) {
        return toGridY(y, 0.0);
    }

    public static int toGridY(double y, double targetY) {
        // Encode the vehicle position relative to the current target.
        int gy = (int) Math.floor(((y - targetY) + HALF_WORLD_HEIGHT) / CELL_SIZE);
        return Math.max(0, Math.min(gy, Q_HEIGHT-1));
    }

    public static int chooseAction(double x, double y) {
        return chooseAction(x, y, 0.0, 0.0);
    }

    public static int chooseAction(double x, double y, double targetX, double targetY) {
        if (rand.nextDouble() < EPSILON) {
            return rand.nextInt(ACTIONS);
        }
        int gx = toGridX(x, targetX);
        int gy = toGridY(y, targetY);

        double bestValue = -1e9;
        int bestAction = 0;
        for (int i = 0; i < ACTIONS; i++) {
            if (Q[gx][gy][i] > bestValue) {
                bestValue = Q[gx][gy][i];
                bestAction = i;
            }
        }
        return bestAction;
    }

    public static void updateQ(double oldX, double oldY, int action, double reward, double newX, double newY) {
        updateQ(oldX, oldY, action, reward, newX, newY, 0.0, 0.0);
    }

    public static void updateQ(double oldX, double oldY, int action, double reward, double newX, double newY, double targetX, double targetY) {
        int ox = toGridX(oldX, targetX);
        int oy = toGridY(oldY, targetY);
        int nx = toGridX(newX, targetX);
        int ny = toGridY(newY, targetY);

        double oldQ =  Q[ox][oy][action];

        double maxNextQ = Q[nx][ny][0];
        for (int i = 0; i < ACTIONS; i++) {
            maxNextQ = Math.max(maxNextQ, Q[nx][ny][i]);
        }

        Q[ox][oy][action] = oldQ + ALPHA *(reward + GAMMA * maxNextQ - oldQ);
    }

}
