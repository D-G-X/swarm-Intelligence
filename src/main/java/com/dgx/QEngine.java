package com.dgx;

import java.util.Random;

public class QEngine {
    public static final double CELL_SIZE = 40.0;
    private static final double HALF_WORLD_WIDTH = (Simulation.WIDTH * Simulation.pix) / 2.0;
    private static final double HALF_WORLD_HEIGHT = (Simulation.HEIGHT * Simulation.pix) / 2.0;
    public static int Q_WIDTH = (int) Math.ceil((Simulation.WIDTH * Simulation.pix * 2.0) / CELL_SIZE) + 1;
    public static int Q_HEIGHT = (int) Math.ceil((Simulation.HEIGHT * Simulation.pix * 2.0) / CELL_SIZE) + 1;
    public static final int ACTIONS = 4; // Up, Down, Left, Right

    public static double[][][] Q = new double[Q_WIDTH][Q_HEIGHT][ACTIONS];

    public static final double ALPHA = 0.1;
    public static final double GAMMA = 0.9;
    public static final double EPSILON = 0.25;
    public static final double VISIT_BONUS = 1.0;
    public static final Random rand = new Random();

    public static int[][] visitCounts = new int[Q_WIDTH][Q_HEIGHT];

    public static int toGridX(double x) {
        int gx = (int) Math.floor((x + HALF_WORLD_WIDTH) / CELL_SIZE);
        return Math.max(0, Math.min(gx, Q_WIDTH-1));
    }

    public static int toGridY(double y) {
        int gy = (int) Math.floor((y + HALF_WORLD_HEIGHT) / CELL_SIZE);
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
        int ox = toGridX(oldX);
        int oy = toGridY(oldY);
        int nx = toGridX(newX);
        int ny = toGridY(newY);

        visitCounts[nx][ny]++;
        double explorationBonus = VISIT_BONUS / Math.sqrt(visitCounts[nx][ny]);
        double shapedReward = reward + explorationBonus;

        double oldQ =  Q[ox][oy][action];

        double maxNextQ = Q[nx][ny][0];
        for (int i = 0; i < ACTIONS; i++) {
            maxNextQ = Math.max(maxNextQ, Q[nx][ny][i]);
        }

        Q[ox][oy][action] = oldQ + ALPHA *(shapedReward + GAMMA * maxNextQ - oldQ);
    }

}
