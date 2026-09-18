package nodebox.graphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class CirclePacker {

    public static class PackedCircle {
        public double x, y, r;
        public int originalIndex;

        public PackedCircle(double x, double y, double r, int index) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.originalIndex = index;
        }
    }

    public static List<Path> pack(List<Double> radii, Point center, double containerRadius, int iterations, int seed) {
        List<Path> paths = new ArrayList<Path>();
        if (radii == null || radii.isEmpty()) return paths;

        double cx = center != null ? center.getX() : 0;
        double cy = center != null ? center.getY() : 0;
        int maxIter = Math.max(10, Math.min(1000, iterations));
        Random rng = new Random(seed);

        List<PackedCircle> circles = new ArrayList<PackedCircle>();
        for (int i = 0; i < radii.size(); i++) {
            double r = Math.max(1.0, radii.get(i) != null ? radii.get(i) : 10.0);
            circles.add(new PackedCircle(0, 0, r, i));
        }

        // Sort descending by radius
        Collections.sort(circles, new Comparator<PackedCircle>() {
            @Override
            public int compare(PackedCircle a, PackedCircle b) {
                return Double.compare(b.r, a.r);
            }
        });

        // Initialize positions along a spiral
        double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
        for (int i = 0; i < circles.size(); i++) {
            PackedCircle c = circles.get(i);
            double dist = Math.sqrt(i + 1) * (c.r * 0.8) + rng.nextDouble() * 2.0;
            double a = i * goldenAngle;
            c.x = cx + Math.cos(a) * dist;
            c.y = cy + Math.sin(a) * dist;
        }

        // Iterative relaxation
        double damping = 0.85;
        for (int iter = 0; iter < maxIter; iter++) {
            double stepRatio = 1.0 - (double) iter / maxIter;

            // 1. Repel overlapping circles
            for (int i = 0; i < circles.size(); i++) {
                PackedCircle c1 = circles.get(i);
                for (int j = i + 1; j < circles.size(); j++) {
                    PackedCircle c2 = circles.get(j);

                    double dx = c2.x - c1.x;
                    double dy = c2.y - c1.y;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    double minD = c1.r + c2.r;

                    if (d < minD) {
                        double overlap = minD - d;
                        if (d < 0.0001) {
                            // Jitter if identical
                            dx = (rng.nextDouble() - 0.5) * 0.01;
                            dy = (rng.nextDouble() - 0.5) * 0.01;
                            d = Math.sqrt(dx * dx + dy * dy);
                        }
                        double nx = dx / d;
                        double ny = dy / d;
                        double push = overlap * 0.5 * stepRatio;

                        c1.x -= nx * push;
                        c1.y -= ny * push;
                        c2.x += nx * push;
                        c2.y += ny * push;
                    }
                }
            }

            // 2. Gravitational pull toward center
            for (PackedCircle c : circles) {
                double dx = cx - c.x;
                double dy = cy - c.y;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 0.01) {
                    double pull = 0.03 * stepRatio;
                    c.x += (dx / d) * pull * d;
                    c.y += (dy / d) * pull * d;
                }

                // 3. Container boundary constraint
                if (containerRadius > 0) {
                    double distToCenter = Math.sqrt((c.x - cx) * (c.x - cx) + (c.y - cy) * (c.y - cy));
                    double maxAllowed = containerRadius - c.r;
                    if (maxAllowed > 0 && distToCenter > maxAllowed) {
                        double factor = maxAllowed / distToCenter;
                        c.x = cx + (c.x - cx) * factor;
                        c.y = cy + (c.y - cy) * factor;
                    }
                }
            }
        }

        // Restore original order
        Collections.sort(circles, new Comparator<PackedCircle>() {
            @Override
            public int compare(PackedCircle a, PackedCircle b) {
                return Integer.compare(a.originalIndex, b.originalIndex);
            }
        });

        // Convert to Path circles
        for (PackedCircle c : circles) {
            Path p = new Path();
            p.ellipse(c.x, c.y, c.r * 2.0, c.r * 2.0);
            paths.add(p);
        }

        return paths;
    }
}
