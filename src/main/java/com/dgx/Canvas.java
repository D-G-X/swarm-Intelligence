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
    boolean showObstacleRadius;
    boolean showTargetDetectionRadius;
    double targetDetectionRadius;
    boolean showType1Circle;

    private int world_margin;
    private int world_border_thickness;

    double[] canvas_dimensions = new double[2];

    Canvas(ArrayList<Vehicle> allVehicles, double pix, ArrayList<Obstacle> obstacles, int width, int height) {
        this.allVehicles = allVehicles;
        this.pix = pix;
        this.allObstacles = obstacles;
        this.setBackground(Color.lightGray);
        this.canvas_dimensions[0] = width;
        this.canvas_dimensions[1] = height;
        setSize(width, height);
    }

    // Method to update target data from the Simulation loop
    public void updateTarget(double[] target, boolean consuming, double targetDetectionRadius) {
        this.currentTarget = target;
        this.isConsuming = consuming;
        this.targetDetectionRadius = targetDetectionRadius;
    }

    public void setShowObstacleRadius(boolean showObstacleRadius) {
        this.showObstacleRadius = showObstacleRadius;
    }

    public void setShowTargetDetectionRadius(boolean showTargetDetectionRadius) {
        this.showTargetDetectionRadius = showTargetDetectionRadius;
    }

    public void setShowType1Circle(boolean showType1Circle) {
        this.showType1Circle = showType1Circle;
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 1. Draw the World Border (Matches vehicle physical boundaries)
        int minPixelX = (int)(world_margin / pix);
        int minPixelY = (int)(world_margin / pix);
        int maxPixelX = (int)(canvas_dimensions[0] * Simulation.pix / pix);
        int maxPixelY = (int)(canvas_dimensions[1] * Simulation.pix / pix);

        int borderWidth = maxPixelX - minPixelX;
        int borderHeight = maxPixelY - minPixelY;

        // Choose how round you want the outer arena to look (e.g., 30 pixels)
        double worldCornerRounding = 30.0;

        // Create a rounded rectangle for the global boundary area
        java.awt.geom.RoundRectangle2D roundedWorld = new java.awt.geom.RoundRectangle2D.Double(
                minPixelX, minPixelY, borderWidth, borderHeight, worldCornerRounding, worldCornerRounding
        );

        // Draw a soft light-gray filled background inside the active simulation zone
        g2d.setColor(new Color(245, 245, 245));
        g2d.fill(roundedWorld);

        // Draw the thick dark frame line
        g2d.setColor(Color.DARK_GRAY);
        g2d.setStroke(new java.awt.BasicStroke(world_border_thickness));
        g2d.draw(roundedWorld);

        // 2. Paint spawnability overlay (green = allowed, red = blocked)
        Graphics2D overlay = (Graphics2D) g2d.create();
        int stepPx = 12; // grid cell size in pixels (tune for speed/clarity)
        double buffer = 5.0; // buffer used when checking obstacle containment (world units)
        int panelW = getWidth();
        int panelH = getHeight();
        for (int px = 0; px < panelW; px += stepPx) {
            for (int py = 0; py < panelH; py += stepPx) {
                double worldX = px * pix;
                double worldY = py * pix;
                boolean blocked = false;
                for (Obstacle obs : allObstacles) {
                    double ox = obs.position[0];
                    double oy = obs.position[1];
                    double ow = obs.getObstacle_width();
                    double oh = obs.getObstacle_height();
                    boolean insideX = worldX >= (ox - buffer) && worldX <= (ox + ow + buffer);
                    boolean insideY = worldY >= (oy - buffer) && worldY <= (oy + oh + buffer);
                    if (insideX && insideY) { blocked = true; break; }
                }
                if (blocked) overlay.setColor(new java.awt.Color(255, 0, 0, 40));
                else overlay.setColor(new java.awt.Color(0, 255, 0, 20));
                int w = Math.min(stepPx, panelW - px);
                int h = Math.min(stepPx, panelH - py);
                overlay.fillRect(px, py, w, h);
            }
        }
        overlay.dispose();

        // 3. Paint Target
        if (currentTarget != null) {
            int tx = (int)(currentTarget[0] / pix);
            int ty = (int)(currentTarget[1] / pix);
            int size = 20;

            g2d.setColor(isConsuming ? Color.GREEN : Color.RED);
            g2d.fillOval(tx - size / 2, ty - size / 2, size, size);

            if (showTargetDetectionRadius) {
                double radiusPx = targetDetectionRadius / pix;
                double diameterPx = radiusPx * 2.0;

                Graphics2D targetRadiusGraphics = (Graphics2D) g2d.create();
                float[] dashPattern = {6.0f, 6.0f};
                targetRadiusGraphics.setColor(new Color(0, 0, 0, 180));
                targetRadiusGraphics.setStroke(new java.awt.BasicStroke(
                        1.5f,
                        java.awt.BasicStroke.CAP_BUTT,
                        java.awt.BasicStroke.JOIN_MITER,
                        10.0f,
                        dashPattern,
                        0.0f
                ));
                targetRadiusGraphics.draw(new java.awt.geom.Ellipse2D.Double(
                        tx - radiusPx,
                        ty - radiusPx,
                        diameterPx,
                        diameterPx
                ));
                targetRadiusGraphics.dispose();
            }
        }

        // 3. Paint Vehicles
        for (Vehicle fz : allVehicles) {
            Polygon q = kfzInPolygon(fz);
            g2d.setColor(Color.BLACK);
            g2d.draw(q);

            if (fz.type == 1 && showType1Circle) {
                int seite = (int)(fz.rad_zus / pix);
                g2d.drawOval((int)(fz.pos[0] / pix) - seite, (int)(fz.pos[1] / pix) - seite, 2 * seite, 2 * seite);
                seite = (int)(fz.rad_sep / pix);
                g2d.drawOval((int)(fz.pos[0] / pix) - seite, (int)(fz.pos[1] / pix) - seite, 2 * seite, 2 * seite);
            }
        }

        // 4. Paint Obstacles
        for (Obstacle obs : allObstacles) {
            // Scale everything to pixel positions
            double x = obs.position[0] / pix;
            double y = obs.position[1] / pix;
            double w = obs.getObstacle_width() / pix;
            double h = obs.getObstacle_height() / pix;

            double cornerRounding = 15.0;

            java.awt.geom.RoundRectangle2D roundedBox = new java.awt.geom.RoundRectangle2D.Double(
                    x, y, w, h, cornerRounding, cornerRounding
            );

            g2d.setColor(new Color(255, 236, 153));
            g2d.fill(roundedBox);
            g2d.draw(roundedBox);

            if (showObstacleRadius) {
                double centerX = x + (w / 2.0);
                double centerY = y + (h / 2.0);
                double radiusWorld = Vehicle.BASE_AVOIDANCE_RADIUS + Math.max(obs.getObstacle_width() / 2.0, obs.getObstacle_height() / 2.0);
                double radiusPx = radiusWorld / pix;
                double diameterPx = radiusPx * 2.0;

                Graphics2D radiusGraphics = (Graphics2D) g2d.create();
                float[] dashPattern = {8.0f, 8.0f};
                radiusGraphics.setColor(Color.BLACK);
                radiusGraphics.setStroke(new java.awt.BasicStroke(
                        1.5f,
                        java.awt.BasicStroke.CAP_BUTT,
                        java.awt.BasicStroke.JOIN_MITER,
                        10.0f,
                        dashPattern,
                        0.0f
                ));
                radiusGraphics.draw(new java.awt.geom.Ellipse2D.Double(
                        centerX - radiusPx,
                        centerY - radiusPx,
                        diameterPx,
                        diameterPx
                ));
                radiusGraphics.dispose();
            }
        }
    }

    public int getWorld_border_thickness() {
        return world_border_thickness;
    }

    public void setWorld_border_thickness(int world_border_thickness) {
        this.world_border_thickness = world_border_thickness;
    }

    public int getWorld_margin() {
        return world_margin;
    }

    public void setWorld_margin(int world_margin) {
        this.world_margin = world_margin;
    }
}