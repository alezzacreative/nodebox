package nodebox.util;

import nodebox.graphics.Path;
import nodebox.graphics.Point;

import java.util.*;

/**
 * Bowyer-Watson 2D Delaunay Triangulation and Dual Voronoi Diagram Generator.
 */
public final class DelaunayVoronoi {

    public static class Edge {
        public final Point p1;
        public final Point p2;

        public Edge(Point p1, Point p2) {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge)) return false;
            Edge edge = (Edge) o;
            return (p1.equals(edge.p1) && p2.equals(edge.p2)) || (p1.equals(edge.p2) && p2.equals(edge.p1));
        }

        @Override
        public int hashCode() {
            return p1.hashCode() ^ p2.hashCode();
        }
    }

    public static class Triangle {
        public final Point a, b, c;
        public final Point circumcenter;
        public final double radiusSq;

        public Triangle(Point a, Point b, Point c) {
            this.a = a;
            this.b = b;
            this.c = c;

            // Calculate circumcircle
            double d = 2 * (a.x * (b.y - c.y) + b.x * (c.y - a.y) + c.x * (a.y - b.y));
            if (Math.abs(d) < 1e-9) {
                this.circumcenter = new Point((a.x + b.x + c.x) / 3.0, (a.y + b.y + c.y) / 3.0);
                this.radiusSq = Double.MAX_VALUE;
            } else {
                double aSq = a.x * a.x + a.y * a.y;
                double bSq = b.x * b.x + b.y * b.y;
                double cSq = c.x * c.x + c.y * c.y;
                double ux = (aSq * (b.y - c.y) + bSq * (c.y - a.y) + cSq * (a.y - b.y)) / d;
                double uy = (aSq * (c.x - b.x) + bSq * (a.x - c.x) + cSq * (b.x - a.x)) / d;
                this.circumcenter = new Point(ux, uy);
                double dx = a.x - ux;
                double dy = a.y - uy;
                this.radiusSq = dx * dx + dy * dy;
            }
        }

        public boolean inCircumcircle(Point p) {
            double dx = p.x - circumcenter.x;
            double dy = p.y - circumcenter.y;
            return (dx * dx + dy * dy) <= (radiusSq + 1e-7);
        }

        public boolean hasVertex(Point p) {
            return a.equals(p) || b.equals(p) || c.equals(p);
        }
    }

    /**
     * Compute Delaunay Triangulation for a list of 2D points.
     */
    public static List<Triangle> triangulate(List<Point> points) {
        if (points == null || points.size() < 3) return Collections.emptyList();

        // Remove duplicate points
        List<Point> pts = new ArrayList<Point>();
        Set<Point> seen = new HashSet<Point>();
        for (Point p : points) {
            if (seen.add(p)) {
                pts.add(p);
            }
        }
        if (pts.size() < 3) return Collections.emptyList();

        // Find bounding box
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Point p : pts) {
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }

        double dx = maxX - minX;
        double dy = maxY - minY;
        double deltaMax = Math.max(dx, dy) * 20.0;
        if (deltaMax == 0) deltaMax = 100.0;
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;

        // Super-triangle vertices
        Point stA = new Point(midX - deltaMax, midY - deltaMax);
        Point stB = new Point(midX, midY + deltaMax * 2.0);
        Point stC = new Point(midX + deltaMax * 2.0, midY - deltaMax);

        List<Triangle> triangulation = new ArrayList<Triangle>();
        triangulation.add(new Triangle(stA, stB, stC));

        for (Point p : pts) {
            List<Triangle> badTriangles = new ArrayList<Triangle>();
            for (Triangle t : triangulation) {
                if (t.inCircumcircle(p)) {
                    badTriangles.add(t);
                }
            }

            // Find boundary of polygon formed by bad triangles
            Map<Edge, Integer> edgeCount = new HashMap<Edge, Integer>();
            for (Triangle t : badTriangles) {
                Edge[] edges = new Edge[]{new Edge(t.a, t.b), new Edge(t.b, t.c), new Edge(t.c, t.a)};
                for (Edge e : edges) {
                    Integer count = edgeCount.get(e);
                    edgeCount.put(e, count == null ? 1 : count + 1);
                }
            }

            triangulation.removeAll(badTriangles);

            for (Map.Entry<Edge, Integer> entry : edgeCount.entrySet()) {
                if (entry.getValue() == 1) { // boundary edge
                    triangulation.add(new Triangle(entry.getKey().p1, entry.getKey().p2, p));
                }
            }
        }

        // Remove triangles that share vertices with super-triangle
        List<Triangle> result = new ArrayList<Triangle>();
        for (Triangle t : triangulation) {
            if (!t.hasVertex(stA) && !t.hasVertex(stB) && !t.hasVertex(stC)) {
                result.add(t);
            }
        }
        return result;
    }

    /**
     * Converts Delaunay triangles into closed triangular Path objects.
     */
    public static List<Path> trianglesToPaths(List<Triangle> triangles) {
        List<Path> paths = new ArrayList<Path>(triangles.size());
        for (Triangle t : triangles) {
            Path p = new Path();
            p.moveto(t.a.x, t.a.y);
            p.lineto(t.b.x, t.b.y);
            p.lineto(t.c.x, t.c.y);
            p.close();
            paths.add(p);
        }
        return paths;
    }

    /**
     * Generates bounded Voronoi cells from points, with optional inset padding.
     */
    public static List<Path> voronoiCells(List<Point> points, double width, double height, double inset) {
        if (points == null || points.size() < 3) return Collections.emptyList();

        double halfW = width > 0 ? width / 2.0 : 500.0;
        double halfH = height > 0 ? height / 2.0 : 500.0;
        double marginW = halfW * 2.5;
        double marginH = halfH * 2.5;

        List<Point> augmentedPoints = new ArrayList<Point>(points);
        augmentedPoints.add(new Point(-marginW, -marginH));
        augmentedPoints.add(new Point(0, -marginH));
        augmentedPoints.add(new Point(marginW, -marginH));
        augmentedPoints.add(new Point(marginW, 0));
        augmentedPoints.add(new Point(marginW, marginH));
        augmentedPoints.add(new Point(0, marginH));
        augmentedPoints.add(new Point(-marginW, marginH));
        augmentedPoints.add(new Point(-marginW, 0));

        List<Triangle> triangles = triangulate(augmentedPoints);
        if (triangles.isEmpty()) return Collections.emptyList();

        // Map each point to the triangles that contain it
        Map<Point, List<Triangle>> pointTriangles = new HashMap<Point, List<Triangle>>();
        for (Triangle t : triangles) {
            Point[] verts = new Point[]{t.a, t.b, t.c};
            for (Point v : verts) {
                List<Triangle> list = pointTriangles.get(v);
                if (list == null) {
                    list = new ArrayList<Triangle>();
                    pointTriangles.put(v, list);
                }
                list.add(t);
            }
        }

        Set<Point> originalSet = new HashSet<Point>(points);
        List<Path> cells = new ArrayList<Path>();

        for (Map.Entry<Point, List<Triangle>> entry : pointTriangles.entrySet()) {
            final Point centerPt = entry.getKey();
            if (!originalSet.contains(centerPt)) continue;

            List<Triangle> triList = entry.getValue();
            if (triList.size() < 3) continue;

            // Sort circumcenters angularly around centerPt
            Collections.sort(triList, new Comparator<Triangle>() {
                public int compare(Triangle t1, Triangle t2) {
                    double a1 = Math.atan2(t1.circumcenter.y - centerPt.y, t1.circumcenter.x - centerPt.x);
                    double a2 = Math.atan2(t2.circumcenter.y - centerPt.y, t2.circumcenter.x - centerPt.x);
                    return Double.compare(a1, a2);
                }
            });

            List<Point> polyVerts = new ArrayList<Point>();
            for (Triangle t : triList) {
                double cx = Math.max(-halfW, Math.min(halfW, t.circumcenter.x));
                double cy = Math.max(-halfH, Math.min(halfH, t.circumcenter.y));
                Point cp = new Point(cx, cy);
                if (polyVerts.isEmpty() || Math.hypot(polyVerts.get(polyVerts.size() - 1).x - cp.x, polyVerts.get(polyVerts.size() - 1).y - cp.y) > 0.1) {
                    polyVerts.add(cp);
                }
            }

            // Apply inset if requested
            if (inset > 0 && polyVerts.size() >= 3) {
                List<Point> insetVerts = new ArrayList<Point>();
                for (Point v : polyVerts) {
                    double dx = v.x - centerPt.x;
                    double dy = v.y - centerPt.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > 1e-4) {
                        double factor = inset <= 1.0 ? Math.max(0.0, 1.0 - inset) : Math.max(0.05, (dist - inset) / dist);
                        insetVerts.add(new Point(centerPt.x + dx * factor, centerPt.y + dy * factor));
                    } else {
                        insetVerts.add(v);
                    }
                }
                polyVerts = insetVerts;
            }

            if (polyVerts.size() >= 3) {
                Path cell = new Path();
                cell.moveto(polyVerts.get(0).x, polyVerts.get(0).y);
                for (int i = 1; i < polyVerts.size(); i++) {
                    cell.lineto(polyVerts.get(i).x, polyVerts.get(i).y);
                }
                cell.close();
                cells.add(cell);
            }
        }

        return cells;
    }
}
