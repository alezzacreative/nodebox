package nodebox.util;

import nodebox.graphics.Contour;
import nodebox.graphics.Geometry;
import nodebox.graphics.Path;
import nodebox.graphics.Point;

import java.util.ArrayList;
import java.util.List;

public final class CornerRounding {

    public enum CornerType {
        ROUND,
        CHAMFER,
        SCOOP,
        DOGBONE,
        SQUIRCLE;

        public static CornerType fromString(String name) {
            if (name == null) return ROUND;
            String lower = name.trim().toLowerCase();
            switch (lower) {
                case "chamfer":
                case "bevel":
                    return CHAMFER;
                case "scoop":
                case "concave":
                case "inset":
                    return SCOOP;
                case "dogbone":
                case "cnc":
                case "tbone":
                    return DOGBONE;
                case "squircle":
                case "superellipse":
                case "g2":
                    return SQUIRCLE;
                case "round":
                case "fillet":
                default:
                    return ROUND;
            }
        }
    }

    private CornerRounding() {
    }

    /**
     * Round corners of all paths within a Geometry object.
     *
     * @param shape     the input geometry
     * @param radius    corner radius in pixels
     * @param typeName  corner style ("round", "chamfer", "scoop", "dogbone", "squircle")
     * @param threshold maximum interior angle in degrees to round (e.g. 175)
     * @param clamp     whether to limit radius to prevent overlapping adjacent corners
     * @return a new Geometry with styled corners
     */
    public static Geometry roundCorners(Geometry shape, double radius, String typeName, double threshold, boolean clamp) {
        if (shape == null) return null;
        if (radius <= 0) return shape.clone();

        CornerType cornerType = CornerType.fromString(typeName);
        Geometry result = new Geometry();

        for (Path p : shape.getPaths()) {
            Path roundedPath = roundPath(p, radius, cornerType, threshold, clamp);
            result.add(roundedPath);
        }

        return result;
    }

    /**
     * Round corners of a single Path.
     */
    public static Path roundPath(Path path, double radius, CornerType type, double threshold, boolean clamp) {
        if (path == null) return null;
        if (radius <= 0) return path.clone();

        Path newPath = new Path();
        newPath.setFillColor(path.getFillColor());
        newPath.setStrokeColor(path.getStrokeColor());
        newPath.setStrokeWidth(path.getStrokeWidth());

        for (Contour contour : path.getContours()) {
            Contour roundedContour = roundContour(contour, radius, type, threshold, clamp);
            if (roundedContour != null && roundedContour.getPointCount() > 0) {
                newPath.add(roundedContour);
            }
        }

        return newPath;
    }

    /**
     * Round corners of a Contour.
     */
    public static Contour roundContour(Contour contour, double radius, CornerType type, double threshold, boolean clamp) {
        if (contour == null || contour.getPointCount() < 3) {
            return contour != null ? new Contour(contour) : null;
        }

        List<Point> points = contour.getPoints();
        boolean closed = contour.isClosed();

        // Check if the contour contains any curve segments
        boolean hasCurves = false;
        for (Point pt : points) {
            if (pt.isCurveTo() || pt.getType() == Point.CURVE_DATA) {
                hasCurves = true;
                break;
            }
        }

        if (hasCurves) {
            return roundMixedContour(contour, radius, type, threshold, clamp);
        } else {
            return roundPolygonalContour(points, closed, radius, type, threshold, clamp);
        }
    }

    /**
     * Rounds a purely polygonal (straight line-to-line) contour.
     */
    private static Contour roundPolygonalContour(List<Point> points, boolean closed, double radius, CornerType type, double threshold, boolean clamp) {
        int n = points.size();
        if (n < 3) return new Contour(points, closed);

        Path tempPath = new Path();
        boolean started = false;

        // Process each vertex
        for (int i = 0; i < n; i++) {
            Point cur = points.get(i);

            // For open contours, endpoints are not rounded
            if (!closed && (i == 0 || i == n - 1)) {
                if (i == 0) {
                    tempPath.moveto(cur.x, cur.y);
                    started = true;
                } else {
                    tempPath.lineto(cur.x, cur.y);
                }
                continue;
            }

            Point prev = points.get((i - 1 + n) % n);
            Point next = points.get((i + 1) % n);

            double d1x = prev.x - cur.x;
            double d1y = prev.y - cur.y;
            double d2x = next.x - cur.x;
            double d2y = next.y - cur.y;

            double len1 = Math.hypot(d1x, d1y);
            double len2 = Math.hypot(d2x, d2y);

            if (len1 < 1e-5 || len2 < 1e-5) {
                if (!started) {
                    tempPath.moveto(cur.x, cur.y);
                    started = true;
                } else {
                    tempPath.lineto(cur.x, cur.y);
                }
                continue;
            }

            double u1x = d1x / len1;
            double u1y = d1y / len1;
            double u2x = d2x / len2;
            double u2y = d2y / len2;

            double dot = Math.max(-1.0, Math.min(1.0, u1x * u2x + u1y * u2y));
            double angleRad = Math.acos(dot);
            double angleDeg = Math.toDegrees(angleRad);

            // If angle exceeds threshold (nearly collinear) or is too sharp (< 2 deg), don't round
            if (angleDeg > threshold || angleDeg < 2.0) {
                if (!started) {
                    tempPath.moveto(cur.x, cur.y);
                    started = true;
                } else {
                    tempPath.lineto(cur.x, cur.y);
                }
                continue;
            }

            // Fillet tangent calculation
            double halfAngle = angleRad / 2.0;
            double tanHalf = Math.tan(halfAngle);
            if (tanHalf < 1e-5) {
                if (!started) {
                    tempPath.moveto(cur.x, cur.y);
                    started = true;
                } else {
                    tempPath.lineto(cur.x, cur.y);
                }
                continue;
            }

            double t = radius / tanHalf;
            double maxT = Math.min(len1 * 0.49, len2 * 0.49);
            double effectiveRadius = radius;

            if (clamp && t > maxT) {
                t = maxT;
                effectiveRadius = t * tanHalf;
            }

            // Calculate tangent points
            double p1x = cur.x + u1x * t;
            double p1y = cur.y + u1y * t;
            double p2x = cur.x + u2x * t;
            double p2y = cur.y + u2y * t;

            if (!started) {
                tempPath.moveto(p1x, p1y);
                started = true;
            } else {
                tempPath.lineto(p1x, p1y);
            }

            // Append styled corner profile from (p1x, p1y) to (p2x, p2y)
            appendCornerProfile(tempPath, cur, p1x, p1y, p2x, p2y, u1x, u1y, u2x, u2y, angleRad, effectiveRadius, t, type);
        }

        if (closed) {
            tempPath.close();
        }

        if (tempPath.getContours().isEmpty()) {
            return new Contour(points, closed);
        }
        return tempPath.getContours().get(0);
    }

    /**
     * Appends the specific corner geometry to the path.
     */
    private static void appendCornerProfile(Path path, Point cur,
                                          double p1x, double p1y, double p2x, double p2y,
                                          double u1x, double u1y, double u2x, double u2y,
                                          double angleRad, double effectiveRadius, double t,
                                          CornerType type) {
        switch (type) {
            case CHAMFER:
                // Straight diagonal cut
                path.lineto(p2x, p2y);
                break;

            case SCOOP: {
                // Inverted arc curving into the interior of the shape
                double bx = u1x + u2x;
                double by = u1y + u2y;
                double blen = Math.hypot(bx, by);
                if (blen > 1e-5) {
                    bx /= blen;
                    by /= blen;
                }
                double depth = t * 0.55;
                double c1x = p1x + bx * (depth * 1.33);
                double c1y = p1y + by * (depth * 1.33);
                double c2x = p2x + bx * (depth * 1.33);
                double c2y = p2y + by * (depth * 1.33);
                path.curveto(c1x, c1y, c2x, c2y, p2x, p2y);
                break;
            }

            case DOGBONE: {
                // Outward circular relief lobe centered past the vertex
                double bx = u1x + u2x;
                double by = u1y + u2y;
                double blen = Math.hypot(bx, by);
                if (blen > 1e-5) {
                    bx /= blen;
                    by /= blen;
                }
                double outX = -bx;
                double outY = -by;
                double apexX = cur.x + outX * (effectiveRadius * 0.75);
                double apexY = cur.y + outY * (effectiveRadius * 0.75);

                double c1x = p1x + outX * (effectiveRadius * 0.5);
                double c1y = p1y + outY * (effectiveRadius * 0.5);
                double c2x = apexX + u1x * (effectiveRadius * 0.3);
                double c2y = apexY + u1y * (effectiveRadius * 0.3);

                double c3x = apexX + u2x * (effectiveRadius * 0.3);
                double c3y = apexY + u2y * (effectiveRadius * 0.3);
                double c4x = p2x + outX * (effectiveRadius * 0.5);
                double c4y = p2y + outY * (effectiveRadius * 0.5);

                path.curveto(c1x, c1y, c2x, c2y, apexX, apexY);
                path.curveto(c3x, c3y, c4x, c4y, p2x, p2y);
                break;
            }

            case SQUIRCLE: {
                // Extended G2 continuous superellipse transition
                double h = t * 0.65;
                double c1x = p1x + (-u1x) * h;
                double c1y = p1y + (-u1y) * h;
                double c2x = p2x + (-u2x) * h;
                double c2y = p2y + (-u2y) * h;
                path.curveto(c1x, c1y, c2x, c2y, p2x, p2y);
                break;
            }

            case ROUND:
            default: {
                // Standard cubic Bézier circular fillet
                double beta = Math.PI - angleRad;
                double h = (4.0 / 3.0) * Math.tan(beta / 4.0) * effectiveRadius;

                // Control points along tangent lines toward corner vertex cur
                double c1x = p1x + (-u1x) * h;
                double c1y = p1y + (-u1y) * h;
                double c2x = p2x + (-u2x) * h;
                double c2y = p2y + (-u2y) * h;

                path.curveto(c1x, c1y, c2x, c2y, p2x, p2y);
                break;
            }
        }
    }

    /**
     * Handles mixed contours that have both straight lines and Bézier curves (e.g. text outlines).
     * Extracts straight-edge corners and applies corner styles while preserving existing curve segments.
     */
    private static Contour roundMixedContour(Contour contour, double radius, CornerType type, double threshold, boolean clamp) {
        List<Point> pts = contour.getPoints();
        boolean closed = contour.isClosed();

        class Segment {
            final int type;
            final Point start;
            final Point c1, c2;
            final Point end;

            Segment(Point start, Point end) {
                this.type = Point.LINE_TO;
                this.start = start;
                this.end = end;
                this.c1 = null;
                this.c2 = null;
            }

            Segment(Point start, Point c1, Point c2, Point end) {
                this.type = Point.CURVE_TO;
                this.start = start;
                this.c1 = c1;
                this.c2 = c2;
                this.end = end;
            }
        }

        List<Segment> segments = new ArrayList<Segment>();
        int i = 1;
        while (i < pts.size()) {
            Point pt = pts.get(i);
            if (pt.isLineTo()) {
                segments.add(new Segment(pts.get(i - 1), pt));
                i++;
            } else if (pt.isCurveTo()) {
                Point p0 = pts.get(i - 3);
                Point c1 = pts.get(i - 2);
                Point c2 = pts.get(i - 1);
                segments.add(new Segment(p0, c1, c2, pt));
                i++;
            } else {
                i++;
            }
        }

        if (closed && !pts.isEmpty()) {
            Point lastPt = pts.get(pts.size() - 1);
            Point firstPt = pts.get(0);
            if (Math.hypot(lastPt.x - firstPt.x, lastPt.y - firstPt.y) > 1e-4) {
                segments.add(new Segment(lastPt, firstPt));
            }
        }

        if (segments.isEmpty()) {
            return new Contour(pts, closed);
        }

        Path newPath = new Path();
        boolean started = false;

        for (int s = 0; s < segments.size(); s++) {
            Segment seg = segments.get(s);
            Segment nextSeg = segments.get((s + 1) % segments.size());

            if (!started) {
                newPath.moveto(seg.start.x, seg.start.y);
                started = true;
            }

            boolean canRound = (closed || s < segments.size() - 1)
                    && seg.type == Point.LINE_TO
                    && nextSeg.type == Point.LINE_TO;

            if (canRound) {
                Point cur = seg.end;
                Point prev = seg.start;
                Point next = nextSeg.end;

                double d1x = prev.x - cur.x;
                double d1y = prev.y - cur.y;
                double d2x = next.x - cur.x;
                double d2y = next.y - cur.y;

                double len1 = Math.hypot(d1x, d1y);
                double len2 = Math.hypot(d2x, d2y);

                if (len1 > 1e-5 && len2 > 1e-5) {
                    double u1x = d1x / len1;
                    double u1y = d1y / len1;
                    double u2x = d2x / len2;
                    double u2y = d2y / len2;

                    double dot = Math.max(-1.0, Math.min(1.0, u1x * u2x + u1y * u2y));
                    double angleRad = Math.acos(dot);
                    double angleDeg = Math.toDegrees(angleRad);

                    if (angleDeg <= threshold && angleDeg >= 2.0) {
                        double halfAngle = angleRad / 2.0;
                        double tanHalf = Math.tan(halfAngle);
                        if (tanHalf > 1e-5) {
                            double t = radius / tanHalf;
                            double maxT = Math.min(len1 * 0.49, len2 * 0.49);
                            double effectiveRadius = radius;
                            if (clamp && t > maxT) {
                                t = maxT;
                                effectiveRadius = t * tanHalf;
                            }

                            double p1x = cur.x + u1x * t;
                            double p1y = cur.y + u1y * t;
                            double p2x = cur.x + u2x * t;
                            double p2y = cur.y + u2y * t;

                            newPath.lineto(p1x, p1y);
                            appendCornerProfile(newPath, cur, p1x, p1y, p2x, p2y, u1x, u1y, u2x, u2y, angleRad, effectiveRadius, t, type);
                            continue;
                        }
                    }
                }
            }

            if (seg.type == Point.LINE_TO) {
                newPath.lineto(seg.end.x, seg.end.y);
            } else {
                newPath.curveto(seg.c1.x, seg.c1.y, seg.c2.x, seg.c2.y, seg.end.x, seg.end.y);
            }
        }

        if (closed) {
            newPath.close();
        }

        if (newPath.getContours().isEmpty()) {
            return new Contour(pts, closed);
        }
        return newPath.getContours().get(0);
    }
}
