package nodebox.graphics;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class Metaballs {

    public static class Circle {
        public double x, y, r;

        public Circle(double x, double y, double r) {
            this.x = x;
            this.y = y;
            this.r = Math.max(0.1, r);
        }
    }

    public static IGeometry createMetaballs(List<Circle> circles, double threshold, double handleFactor, boolean unite) {
        if (circles == null || circles.isEmpty()) return new Geometry();

        Area area = new Area();
        List<Path> bridges = new ArrayList<Path>();

        // Add base circles
        for (Circle c : circles) {
            Ellipse2D.Double circleShape = new Ellipse2D.Double(c.x - c.r, c.y - c.r, c.r * 2, c.r * 2);
            if (unite) {
                area.add(new Area(circleShape));
            } else {
                bridges.add(new Path(circleShape));
            }
        }

        double hf = Math.max(0.01, Math.min(2.0, handleFactor));
        double maxDistExtra = Math.max(0.0, threshold);

        // Compute bridges between every pair
        for (int i = 0; i < circles.size(); i++) {
            Circle c1 = circles.get(i);
            for (int j = i + 1; j < circles.size(); j++) {
                Circle c2 = circles.get(j);

                double dx = c2.x - c1.x;
                double dy = c2.y - c1.y;
                double d = Math.sqrt(dx * dx + dy * dy);

                if (d <= Math.abs(c1.r - c2.r) || d >= c1.r + c2.r + maxDistExtra || d <= 0.0001) {
                    continue;
                }

                double angle = Math.atan2(dy, dx);
                double u1, u2;

                if (d < c1.r + c2.r) {
                    // Overlapping
                    u1 = Math.acos(Math.max(-1.0, Math.min(1.0, (c1.r - c2.r) / d)));
                    u2 = Math.acos(Math.max(-1.0, Math.min(1.0, (c2.r - c1.r) / d)));
                } else {
                    double v = (d - (c1.r + c2.r)) / Math.max(0.001, maxDistExtra);
                    double spread = (Math.PI / 2.0) * (1.0 - Math.min(1.0, v));
                    u1 = spread;
                    u2 = spread;
                }

                // Angles on circles
                double a1a = angle + u1;
                double a1b = angle - u1;
                double a2a = angle + Math.PI - u2;
                double a2b = angle - Math.PI + u2;

                // Points on circle boundaries
                double p1ax = c1.x + Math.cos(a1a) * c1.r;
                double p1ay = c1.y + Math.sin(a1a) * c1.r;
                double p1bx = c1.x + Math.cos(a1b) * c1.r;
                double p1by = c1.y + Math.sin(a1b) * c1.r;

                double p2ax = c2.x + Math.cos(a2a) * c2.r;
                double p2ay = c2.y + Math.sin(a2a) * c2.r;
                double p2bx = c2.x + Math.cos(a2b) * c2.r;
                double p2by = c2.y + Math.sin(a2b) * c2.r;

                double dTotal = c1.r + c2.r;
                double dFactor = Math.min(1.0, 2.0 * d / dTotal);
                double hl1 = c1.r * hf * dFactor;
                double hl2 = c2.r * hf * dFactor;

                // Control handles along tangent directions
                double h1ax = p1ax - Math.sin(a1a) * hl1;
                double h1ay = p1ay + Math.cos(a1a) * hl1;

                double h2ax = p2ax + Math.sin(a2a) * hl2;
                double h2ay = p2ay - Math.cos(a2a) * hl2;

                double h2bx = p2bx - Math.sin(a2b) * hl2;
                double h2by = p2by + Math.cos(a2b) * hl2;

                double h1bx = p1bx + Math.sin(a1b) * hl1;
                double h1by = p1by - Math.cos(a1b) * hl1;

                GeneralPath bridge = new GeneralPath();
                bridge.moveTo(p1ax, p1ay);
                bridge.curveTo(h1ax, h1ay, h2ax, h2ay, p2ax, p2ay);
                bridge.lineTo(p2bx, p2by);
                bridge.curveTo(h2bx, h2by, h1bx, h1by, p1bx, p1by);
                bridge.closePath();

                if (unite) {
                    area.add(new Area(bridge));
                } else {
                    bridges.add(new Path(bridge));
                }
            }
        }

        if (unite) {
            return new Path(area);
        } else {
            Geometry geo = new Geometry();
            for (Path p : bridges) {
                geo.add(p);
            }
            return geo;
        }
    }
}
