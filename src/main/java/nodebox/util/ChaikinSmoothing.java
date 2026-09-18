package nodebox.util;

import nodebox.graphics.Contour;
import nodebox.graphics.Geometry;
import nodebox.graphics.Path;
import nodebox.graphics.Point;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chaikin's algorithm for curve and polygon smoothing.
 */
public final class ChaikinSmoothing {

    public static List<Point> smoothPoints(List<Point> points, boolean closed, int iterations, double tension) {
        if (points == null || points.size() < 3) {
            return points != null ? new ArrayList<Point>(points) : Collections.<Point>emptyList();
        }

        iterations = Math.max(1, Math.min(iterations, 8));
        double r = Math.max(0.05, Math.min(tension, 0.45));

        List<Point> current = new ArrayList<Point>(points);

        for (int it = 0; it < iterations; it++) {
            List<Point> next = new ArrayList<Point>();
            int n = current.size();

            if (closed) {
                for (int i = 0; i < n; i++) {
                    Point p0 = current.get(i);
                    Point p1 = current.get((i + 1) % n);

                    double qx = (1.0 - r) * p0.x + r * p1.x;
                    double qy = (1.0 - r) * p0.y + r * p1.y;
                    double rx = r * p0.x + (1.0 - r) * p1.x;
                    double ry = r * p0.y + (1.0 - r) * p1.y;

                    next.add(new Point(qx, qy));
                    next.add(new Point(rx, ry));
                }
            } else {
                next.add(current.get(0));
                for (int i = 0; i < n - 1; i++) {
                    Point p0 = current.get(i);
                    Point p1 = current.get(i + 1);

                    double qx = (1.0 - r) * p0.x + r * p1.x;
                    double qy = (1.0 - r) * p0.y + r * p1.y;
                    double rx = r * p0.x + (1.0 - r) * p1.x;
                    double ry = r * p0.y + (1.0 - r) * p1.y;

                    next.add(new Point(qx, qy));
                    next.add(new Point(rx, ry));
                }
                next.add(current.get(n - 1));
            }
            current = next;
        }

        return current;
    }

    public static Path smoothPath(Path path, int iterations, double tension) {
        if (path == null) return null;
        Path result = new Path(path, false);
        for (Contour c : path.getContours()) {
            List<Point> pts = c.getPoints();
            if (pts.size() < 3) {
                result.add(c);
                continue;
            }
            List<Point> smoothed = smoothPoints(pts, c.isClosed(), iterations, tension);
            Path subPath = new Path();
            subPath.moveto(smoothed.get(0).x, smoothed.get(0).y);
            for (int i = 1; i < smoothed.size(); i++) {
                subPath.lineto(smoothed.get(i).x, smoothed.get(i).y);
            }
            if (c.isClosed()) {
                subPath.close();
            }
            result.extend(subPath);
        }
        return result;
    }

    public static Geometry smoothGeometry(Geometry geometry, int iterations, double tension) {
        if (geometry == null) return null;
        Geometry result = new Geometry();
        for (Path p : geometry.getPaths()) {
            result.add(smoothPath(p, iterations, tension));
        }
        return result;
    }
}
