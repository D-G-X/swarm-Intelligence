package com.dgx;

public class Obstacle {
    private double obstacle_width;
    private double obstacle_height;
    private String obstacle_name;

    double[] position;

    // constructor with obstacle positions, width and height
    Obstacle(double[] position, double obstacle_width, double obstacle_height, String obstacle_name) {
        this.position = position;
        this.obstacle_width = obstacle_width;
        this.obstacle_height = obstacle_height;
        this.obstacle_name = obstacle_name;
    }

    // constructor with only position of the obstacle and default width and height
    Obstacle(double[] position) {
        this.position = position;
        this.obstacle_width = 40;
        this.obstacle_height = 40;
        this.obstacle_name = "";
    }

    public double getObstacle_width() {
        return obstacle_width;
    }

    public void setObstacle_width(double obstacle_width) {
        this.obstacle_width = obstacle_width;
    }

    public double getObstacle_height() {
        return obstacle_height;
    }

    public void setObstacle_height(double obstacle_height) {
        this.obstacle_height = obstacle_height;
    }

    public String getObstacle_name() {
        return obstacle_name;
    }

    public void setObstacle_name(String obstacle_name) {
        this.obstacle_name = obstacle_name;
    }
}
