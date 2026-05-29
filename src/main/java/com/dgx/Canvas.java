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
    ArrayList<BlackHole> allBlackHoles;

    // New fields to track the target state
    double[] currentTarget;
    boolean isConsuming;
    boolean showObstacleRadius;
    boolean showGrid;
    boolean showQValues;
    boolean showQShade;
    boolean showBlackHoleRadius;
    boolean showTargetDetectionRadius;
    double targetDetectionRadius;
    boolean showType1Circle;
    boolean showTimer;
    long targetSearchElapsedMillis;
    long lastCaptureMillis;

    private int world_margin;
    private int world_border_thickness;

    double[] canvas_dimensions = new double[2];

    Canvas(ArrayList<Vehicle> allVehicles, double pix, ArrayList<Obstacle> obstacles, ArrayList<BlackHole> blackHoles, int width, int height) {
        this.allVehicles = allVehicles;
        this.pix = pix;
        this.allObstacles = obstacles;
        this.allBlackHoles = blackHoles;
        this.setBackground(Color.lightGray);
        this.canvas_dimensions[0] = width;
        this.canvas_dimensions[1] = height;
        setSize(width, height);
    }

    // Method to update target data from the Simulation loop
    public void updateTarget(double[] target, boolean consuming, double targetDetectionRadius, long targetSearchElapsedMillis, long lastCaptureMillis) {
        this.currentTarget = target;
        this.isConsuming = consuming;
        this.targetDetectionRadius = targetDetectionRadius;
        this.targetSearchElapsedMillis = targetSearchElapsedMillis;
        this.lastCaptureMillis = lastCaptureMillis;
    }

    public void setShowObstacleRadius(boolean showObstacleRadius) {
        this.showObstacleRadius = showObstacleRadius;
    }

    public void setShowBlackHoleRadius(boolean showBlackHoleRadius) {
        this.showBlackHoleRadius = showBlackHoleRadius;
    }

    public void setShowTargetDetectionRadius(boolean showTargetDetectionRadius) {
        this.showTargetDetectionRadius = showTargetDetectionRadius;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void setShowQValues(boolean showQValues) {
        this.showQValues = showQValues;
    }

    public void setShowQShade(boolean showQShade) {
        this.showQShade = showQShade;
    }

    public void setShowType1Circle(boolean showType1Circle) {
        this.showType1Circle = showType1Circle;
    }

    public void setShowTimer(boolean showTimer) {
        this.showTimer = showTimer;
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

        // 1b. Paint the initial vehicle spawn circle in the top-right corner.
        double spawnRadiusWorld = Simulation.SPAWN_POINT_RADIUS;
        double spawnCenterWorldX = (canvas_dimensions[0] * Simulation.pix) - world_margin - spawnRadiusWorld;
        double spawnCenterWorldY = world_margin + spawnRadiusWorld;
        double spawnRadiusPx = spawnRadiusWorld / pix;
        double spawnCenterPxX = spawnCenterWorldX / pix;
        double spawnCenterPxY = spawnCenterWorldY / pix;

        Graphics2D spawnGraphics = (Graphics2D) g2d.create();
        float[] spawnDashPattern = {4.0f, 6.0f};
        spawnGraphics.setColor(new Color(220, 0, 0));
        spawnGraphics.setStroke(new java.awt.BasicStroke(
            1.6f,
            java.awt.BasicStroke.CAP_BUTT,
            java.awt.BasicStroke.JOIN_MITER,
            10.0f,
            spawnDashPattern,
            0.0f
        ));
        spawnGraphics.draw(new java.awt.geom.Ellipse2D.Double(
            spawnCenterPxX - spawnRadiusPx,
            spawnCenterPxY - spawnRadiusPx,
            spawnRadiusPx * 2.0,
            spawnRadiusPx * 2.0
        ));

        spawnGraphics.setFont(spawnGraphics.getFont().deriveFont(java.awt.Font.BOLD, 12.0f));
        java.awt.FontMetrics spawnMetrics = spawnGraphics.getFontMetrics();
        String spawnLabelTop = "SPAWN POINT";
        String spawnLabelBottom = "(Spawnpunkt)";
        int topLabelWidth = spawnMetrics.stringWidth(spawnLabelTop);
        int bottomLabelWidth = spawnMetrics.stringWidth(spawnLabelBottom);
        int labelAscent = spawnMetrics.getAscent();
        int lineGap = spawnMetrics.getHeight() - labelAscent;
        spawnGraphics.setColor(new Color(160, 0, 0));
        spawnGraphics.drawString(
            spawnLabelTop,
            (float)(spawnCenterPxX - topLabelWidth / 2.0),
            (float)(spawnCenterPxY - 2.0)
        );
        spawnGraphics.drawString(
            spawnLabelBottom,
            (float)(spawnCenterPxX - bottomLabelWidth / 2.0),
            (float)(spawnCenterPxY + labelAscent + lineGap)
        );

        spawnGraphics.dispose();

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

            String obstacleName = obs.getObstacle_name();
            if (obstacleName != null && !obstacleName.isBlank()) {
                Graphics2D labelGraphics = (Graphics2D) g2d.create();
                labelGraphics.setColor(new Color(90, 60, 0));

                double maxFontSize = Math.max(10.0, Math.min(18.0, h * 0.35));
                labelGraphics.setFont(labelGraphics.getFont().deriveFont(java.awt.Font.BOLD, (float) maxFontSize));
                java.awt.FontMetrics metrics = labelGraphics.getFontMetrics();

                String labelText = obstacleName;
                while (metrics.stringWidth(labelText) > (w - 8.0) && labelText.length() > 1) {
                    labelText = labelText.substring(0, labelText.length() - 1);
                }

                int textWidth = metrics.stringWidth(labelText);
                int textHeight = metrics.getAscent() - metrics.getDescent();
                float textX = (float) (x + (w - textWidth) / 2.0);
                float textY = (float) (y + (h + textHeight) / 2.0);
                labelGraphics.drawString(labelText, textX, textY);
                labelGraphics.dispose();
            }

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

        // 5. Paint Black Holes
        if (allBlackHoles != null) {
            for (BlackHole bh : allBlackHoles) {
                double cx = bh.position[0] / pix;
                double cy = bh.position[1] / pix;
                double r = bh.getHole_radius() / pix;

                Graphics2D bhG = (Graphics2D) g2d.create();
                java.awt.geom.Ellipse2D holeShape = new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, r * 2.0, r * 2.0);
                bhG.setPaint(new java.awt.RadialGradientPaint(
                        new java.awt.geom.Point2D.Double(cx, cy),
                        (float) r,
                    new float[]{0.0f, 0.45f, 1.0f},
                    new Color[]{Color.BLACK, new Color(55, 55, 55), new Color(130, 130, 130)}
                ));
                bhG.fill(holeShape);

                bhG.setColor(new Color(170, 170, 170, 180));
                bhG.draw(holeShape);

                if (showBlackHoleRadius) {
                    double radiusWorld = bh.getHole_radius() + 20.0;
                    double radiusPx = radiusWorld / pix;
                    double diameterPx = radiusPx * 2.0;

                    Graphics2D radiusGraphics = (Graphics2D) bhG.create();
                    float[] dashPattern = {8.0f, 8.0f};
                    radiusGraphics.setColor(new Color(0, 0, 0, 180));
                    radiusGraphics.setStroke(new java.awt.BasicStroke(
                        1.5f,
                        java.awt.BasicStroke.CAP_BUTT,
                        java.awt.BasicStroke.JOIN_MITER,
                        10.0f,
                        dashPattern,
                        0.0f
                    ));
                    radiusGraphics.draw(new java.awt.geom.Ellipse2D.Double(
                        cx - radiusPx,
                        cy - radiusPx,
                        diameterPx,
                        diameterPx
                    ));
                    radiusGraphics.dispose();
                }

                // Draw name centered inside the black hole (reduced size)
                String name = bh.getHole_name();
                if (name != null && !name.isBlank()) {
                    float fontSize = Math.max(8f, (float)(r * 0.25));
                    bhG.setFont(bhG.getFont().deriveFont(java.awt.Font.BOLD, fontSize));
                    java.awt.FontMetrics fm = bhG.getFontMetrics();
                    int w = fm.stringWidth(name);
                    bhG.setColor(Color.WHITE);
                    float textX = (float)(cx - w / 2.0);
                    float textY = (float)(cy + (fm.getAscent() - fm.getDescent()) / 2.0);
                    bhG.drawString(name, textX, textY);
                }

                bhG.dispose();
            }
        }

        if (showQShade) {
            double minQ = Double.POSITIVE_INFINITY;
            double maxQ = Double.NEGATIVE_INFINITY;
            for (int gx = 0; gx < QEngine.Q.length; gx++) {
                for (int gy = 0; gy < QEngine.Q[0].length; gy++) {
                    double cellBest = Double.NEGATIVE_INFINITY;
                    for (int a = 0; a < QEngine.Q[0][0].length; a++) {
                        cellBest = Math.max(cellBest, QEngine.Q[gx][gy][a]);
                    }
                    minQ = Math.min(minQ, cellBest);
                    maxQ = Math.max(maxQ, cellBest);
                }
            }

            double range = maxQ - minQ;
            if (range > 1e-9) {
                Graphics2D shadeG = (Graphics2D) g2d.create();
                int cellPx = Math.max(1, (int)Math.round(QEngine.CELL_SIZE / pix));
                if (cellPx >= 4) {
                    for (int gx = 0; gx < QEngine.Q.length; gx++) {
                        for (int gy = 0; gy < QEngine.Q[0].length; gy++) {
                            double cellBest = Double.NEGATIVE_INFINITY;
                            for (int a = 0; a < QEngine.Q[0][0].length; a++) {
                                cellBest = Math.max(cellBest, QEngine.Q[gx][gy][a]);
                            }

                            double normalized = (cellBest - minQ) / range;
                            normalized = Math.max(0.0, Math.min(1.0, normalized));

                            int red = (int) Math.round(255 * (1.0 - normalized));
                            int green = (int) Math.round(255 * normalized);
                            int alpha = 28 + (int) Math.round(24 * normalized);

                            shadeG.setColor(new Color(red, green, 80, alpha));
                            int x = (int) Math.round((gx * QEngine.CELL_SIZE) / pix);
                            int y = (int) Math.round((gy * QEngine.CELL_SIZE) / pix);
                            int w = Math.min(cellPx, getWidth() - x);
                            int h = Math.min(cellPx, getHeight() - y);
                            if (w > 0 && h > 0) {
                                shadeG.fillRect(x, y, w, h);
                            }
                        }
                    }
                }
                shadeG.dispose();
            }
        }

        // 6. Top-layer overlays: grid and Q-values (drawn after obstacles & black holes)
        if (showGrid) {
            Graphics2D gridG = (Graphics2D) g2d.create();
            gridG.setColor(new java.awt.Color(0, 0, 0, 120));
            int gridStepPx = Math.max(1, (int)Math.round(QEngine.CELL_SIZE / pix));
            int w = getWidth();
            int h = getHeight();
            for (int gx = 0; gx < w; gx += gridStepPx) {
                gridG.drawLine(gx, 0, gx, h);
            }
            for (int gy = 0; gy < h; gy += gridStepPx) {
                gridG.drawLine(0, gy, w, gy);
            }
            gridG.dispose();
        }

        if (showQValues) {
            Graphics2D qg = (Graphics2D) g2d.create();
            qg.setColor(new java.awt.Color(0, 0, 0, 200));
            int cellPx = Math.max(1, (int)Math.round(QEngine.CELL_SIZE / pix));
            if (cellPx >= 8) { // only render when readable
                java.awt.Font f = qg.getFont().deriveFont(9.0f);
                qg.setFont(f);
                for (int gx = 0; gx < QEngine.Q.length; gx++) {
                    for (int gy = 0; gy < QEngine.Q[0].length; gy++) {
                        double maxQ = Double.NEGATIVE_INFINITY;
                        for (int a = 0; a < QEngine.Q[0][0].length; a++) {
                            maxQ = Math.max(maxQ, QEngine.Q[gx][gy][a]);
                        }
                        int cx = (int)((gx * QEngine.CELL_SIZE + QEngine.CELL_SIZE/2.0) / pix);
                        int cy = (int)((gy * QEngine.CELL_SIZE + QEngine.CELL_SIZE/2.0) / pix);
                        String s = String.format("%.1f", maxQ);
                        java.awt.FontMetrics fm = qg.getFontMetrics();
                        int sw = fm.stringWidth(s);
                        int sh = fm.getAscent();
                        // background for readability
                        qg.setColor(new java.awt.Color(255, 255, 255, 200));
                        qg.fillRect(cx - sw/2 - 2, cy - sh + 1, sw + 4, sh + 2);
                        qg.setColor(new java.awt.Color(0, 0, 0, 220));
                        qg.drawString(s, cx - sw/2, cy + 1);
                    }
                }
            }
            qg.dispose();
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