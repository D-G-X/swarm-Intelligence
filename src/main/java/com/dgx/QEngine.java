package com.dgx;

import java.util.Random;

public class QEngine {
    public static final double CELL_SIZE = 25.0;
    public static int Q_WIDTH = (int) Math.ceil((Simulation.WIDTH*Simulation.pix)/CELL_SIZE);
    public static int Q_HEIGHT = (int) Math.ceil((Simulation.HEIGHT*Simulation.pix)/CELL_SIZE);
    public static final int ACTIONS = 4; // Up, Down, Left, Right

    public static double[][][] Q = new double[Q_WIDTH][Q_HEIGHT][ACTIONS];

    public static final double ALPHA = 0.1;
    public static final double GAMMA = 0.9;
    public static final double EPSILON = 0.15;
    public static final Random rand = new Random();

    public static int toGridX(double x) {
        // Convert Continuous coordinate to QTable array index for X axis
        int gx = (int)(x/CELL_SIZE);
        return Math.max(0, Math.min(gx, Q_WIDTH-1));
    }

    public static int toGridY(double y) {
        // Convert Continuous coordinate to QTable array index for Y axis
        int gy = (int)(y/CELL_SIZE);
        return Math.max(0, Math.min(gy, Q_HEIGHT-1));
    }

    public static int chooseAction(double x, double y) {
        if (rand.nextDouble() < EPSILON) {
            return rand.nextInt(ACTIONS);
        }
        int gx = toGridX(x);
        int gy = toGridY(y);

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
        int ox =  toGridX(oldX);
        int oy = toGridY(oldY);
        int ny = toGridY(newY);
        int nx = toGridX(newX);

        double oldQ =  Q[ox][oy][action];

        double maxNextQ = Q[nx][ny][action];
        for (int i = 0; i < ACTIONS; i++) {
            maxNextQ = Math.max(maxNextQ, Q[nx][ny][action]);
        }

        Q[ox][oy][action] = oldQ + ALPHA *(reward + GAMMA * maxNextQ - oldQ);
    }

}
