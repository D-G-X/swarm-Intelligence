package com.dgx;

public class BlackHole {
    private double hole_radius;
    private String hole_name;

    double[] position;

    // constructor with blackhole positions, radius and name
    BlackHole(double[] position, double hole_radius, String hole_name) {
        this.position = position;
        this.hole_radius = hole_radius;
        this.hole_name = hole_name;
    }

    // constructor with only position of the blackhole and default radius
    BlackHole(double[] position) {
        this.position = position;
        this.hole_radius = 40;
        this.hole_name = "Schwarzes Loch";
    }

    public String getHole_name() {
        return hole_name;
    }

    public void setHole_name(String hole_name) {
        this.hole_name = hole_name;
    }

    public double getHole_radius() {
        return hole_radius;
    }
    public void setHole_radius(double hole_radius) {
        this.hole_radius = hole_radius;
    }
}
