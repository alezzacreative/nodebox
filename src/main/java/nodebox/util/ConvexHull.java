package nodebox.util;

import nodebox.graphics.Path;
import nodebox.graphics.Point;

import java.util.*;

/**
 * 2D Convex Hull computation using Andrew's Monotone Chain algorithm.
 * Runs in O(n log n) time.
 */
public final class ConvexHull {

    private static double cross(Point o, Point a, Point b) {
        return (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x);
    }

    public static List<Point> computeHullPoints(List<Point> points) {
        if (points == null || points.size() < 3) {
            return points != null ? new ArrayList<Point>(points) : Collections.<Point>emptyList();
        }

        List<Point> pts = new ArrayList<Point>(points);
        Collections.sort(pts, new Comparator<Point>() {
            public int compare(Point a, Point b) {
                if (a.x != b.x) return Double.compare(a.x, b.x);
                return Double.compare(a.y, b.y);
            }
        });

        int n = pts.size();
        List<Point> lower = new ArrayList<Point>();
        for (int i = 0; i < n; i++) {
            Point p = pts.get(i);
            while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0) {
                lower.remove(lower.size() - 1);
            }
            lower.add(p);
        }

        List<Point> upper = new ArrayList<Point>();
        for (int i = n - 1; i >= 0; i--) {
            Point p = pts.get(i);
            while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0) {
                upper.remove(upper.size() - 1);
            }
            upper.add(p);
        }

        lower.remove(lower.size() - 1);
        upper.remove(upper.size() - 1);

        List<Point> hull = new ArrayList<Point>(lower.size() + upper.size());
        hull.addAll(lower);
        hull.addAll(upper);
        return hull;
    }

    public static Path computeHullPath(List<Point> points) {
        List<Point> hull = computeHullPoints(points);
        if (hull.isEmpty()) return new Path();

        Path path = new Path();
        path.moveto(hull.get(0).x, hull.get(0).y);
        for (int i = 1; i < hull.size(); i++) {
            path.lineto(hull.get(i).x, hull.get(i).y);
        }
        path.close();
        return path;
    }
}
