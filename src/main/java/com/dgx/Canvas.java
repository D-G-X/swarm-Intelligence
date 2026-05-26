package com.dgx;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import javax.swing.JPanel;

public class Canvas extends JPanel {

    ArrayList<Vehicle> allVehicles;
    double pix;
    ArrayList<Obstacle> allObstacles;

    // New fields to track the target state
    double[] currentTarget;
    boolean isConsuming;

    double[] canvas_dimensions = new double[2];

    Canvas(ArrayList<Vehicle> allVehicles, double pix, ArrayList<Obstacle> obstacles, int width, int height) {
        this.allVehicles = allVehicles;
        this.pix = pix;
        this.allObstacles = obstacles;
        this.setBackground(Color.lightGray);
        this.canvas_dimensions[0] = width;
        this.canvas_dimensions[1] = height;
        setSize(1000, 1000);
    }

    // Method to update target data from the Simulation loop
    public void updateTarget(double[] target, boolean consuming) {
        this.currentTarget = target;
        this.isConsuming = consuming;
    }

    public Polygon kfzInPolygon(Vehicle fz) {
        Polygon q = new Polygon();
        int l = (int)(fz.FZL / pix);
        int b = (int)(fz.FZB / pix);
        int x = (int)(fz.pos[0] / pix);
        int y = (int)(fz.pos[1] / pix);
        int dia = (int)(Math.sqrt(Math.pow(l / 2, 2) + Math.pow(b / 2, 2)));
        double t = VectorCalculation.angle(fz.vel);
        double phi1 = Math.atan(fz.FZB / fz.FZL);
        double phi2 = Math.PI - phi1;
        double phi3 = Math.PI + phi1;
        double phi4 = 2 * Math.PI - phi1;

        q.addPoint((int)(x + (dia * Math.cos(t + phi1))), (int)(y + (dia * Math.sin(t + phi1))));
        q.addPoint((int)(x + (dia * Math.cos(t + phi2))), (int)(y + (dia * Math.sin(t + phi2))));
        q.addPoint((int)(x + (dia * Math.cos(t + phi3))), (int)(y + (dia * Math.sin(t + phi3))));
        q.addPoint((int)(x + (dia * Math.cos(t + phi4))), (int)(y + (dia * Math.sin(t + phi4))));
        return q;
    }

    public Polygon kfzInPolygonObs(Obstacle obs) {
        Polygon q = new Polygon();
        int w = (int)(obs.getObstacle_width() / pix);
        int h = (int)(obs.getObstacle_height() / pix);
        int x = (int)(obs.position[0] / pix);
        int y = (int)(obs.position[1] / pix);

        // Define the corners sequentially starting from the top-left (x, y)
        q.addPoint(x, y);         // 1. Top-Left: (0, 0)
        q.addPoint(x, y + h);     // 2. Bottom-Left: (0, 0 + height)
        q.addPoint(x + w, y + h); // 3. Bottom-Right: (0 + width, 0 + height)
        q.addPoint(x + w, y);     // 4. Top-Right: (0 + width, 0)

        return q;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 1. Draw the World Border (Matches vehicle physical boundaries)
        int minPixelX = (int)(10 / pix);
        int minPixelY = (int)(10 / pix);
        int maxPixelX = (int)(canvas_dimensions[0] * Simulation.pix / pix);
        int maxPixelY = (int)(canvas_dimensions[1] * Simulation.pix / pix);

        int borderWidth = maxPixelX - minPixelX;
        int borderHeight = maxPixelY - minPixelY;

        // Draw a light gray filled background for the active simulation zone (Optional)
        g2d.setColor(new Color(245, 245, 245));
        g2d.fillRect(minPixelX, minPixelY, borderWidth, borderHeight);

        // Draw a thick dark border line
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new java.awt.BasicStroke(3)); // Gives the line some weight
        g2d.drawRect(minPixelX, minPixelY, borderWidth, borderHeight);

        // Reset stroke back to default so other elements draw normally
        g2d.setStroke(new java.awt.BasicStroke(1));

        // 2. Paint Target
        if (currentTarget != null) {
            int tx = (int)(currentTarget[0] / pix);
            int ty = (int)(currentTarget[1] / pix);
            int size = 10;

            g2d.setColor(isConsuming ? Color.GREEN : Color.RED);
            g2d.fillOval(tx - size / 2, ty - size / 2, size, size);
        }

        // 3. Paint Vehicles
        for (Vehicle fz : allVehicles) {
            Polygon q = kfzInPolygon(fz);
            g2d.setColor(Color.BLACK);
            g2d.draw(q);

            if (fz.type == 1) {
                int seite = (int)(fz.rad_zus / pix);
                g2d.drawOval((int)(fz.pos[0] / pix) - seite, (int)(fz.pos[1] / pix) - seite, 2 * seite, 2 * seite);
                seite = (int)(fz.rad_sep / pix);
                g2d.drawOval((int)(fz.pos[0] / pix) - seite, (int)(fz.pos[1] / pix) - seite, 2 * seite, 2 * seite);
            }
        }

        // 4. Paint Obstacles
        for (Obstacle obs : allObstacles) {
            Polygon q = kfzInPolygonObs(obs);
            g2d.setColor(Color.GRAY);
            g2d.fill(q);
            g2d.setColor(Color.BLACK);
            g2d.draw(q);
        }
    }
}