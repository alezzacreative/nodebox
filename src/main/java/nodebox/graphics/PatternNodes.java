package nodebox.graphics;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public final class PatternNodes {

    private PatternNodes() {
    }

    /**
     * Wrap and duplicate vector geometry across tile boundaries to create
     * seamless repeating patterns for textiles and wallpaper.
     */
    public static Geometry seamlessTile(IGeometry shape, double width, double height, boolean clip, Point center) {
        if (shape == null) return new Geometry();
        Geometry source = shape instanceof Geometry ? (Geometry) shape : ((Path) shape).asGeometry();
        double w = Math.max(1.0, width);
        double h = Math.max(1.0, height);
        Point c = center != null ? center : Point.ZERO;

        double minX = c.x - w / 2.0;
        double maxX = c.x + w / 2.0;
        double minY = c.y - h / 2.0;
        double maxY = c.y + h / 2.0;

        Geometry result = new Geometry();

        for (Path p : source.getPaths()) {
            result.add(p.clone());

            Rect bounds = p.getBounds();
            if (bounds == null) continue;

            boolean crossLeft = bounds.getX() < minX;
            boolean crossRight = bounds.getX() + bounds.getWidth() > maxX;
            boolean crossTop = bounds.getY() < minY;
            boolean crossBottom = bounds.getY() + bounds.getHeight() > maxY;

            // Edge wraps
            if (crossLeft) {
                result.add(Transform.translated(w, 0).map(p));
            }
            if (crossRight) {
                result.add(Transform.translated(-w, 0).map(p));
            }
            if (crossTop) {
                result.add(Transform.translated(0, h).map(p));
            }
            if (crossBottom) {
                result.add(Transform.translated(0, -h).map(p));
            }

            // Corner wraps
            if (crossLeft && crossTop) {
                result.add(Transform.translated(w, h).map(p));
            }
            if (crossRight && crossTop) {
                result.add(Transform.translated(-w, h).map(p));
            }
            if (crossLeft && crossBottom) {
                result.add(Transform.translated(w, -h).map(p));
            }
            if (crossRight && crossBottom) {
                result.add(Transform.translated(-w, -h).map(p));
            }
        }

        if (clip) {
            Path cutter = new Path();
            cutter.rect(c.x, c.y, w, h);
            Geometry clipped = new Geometry();
            for (Path p : result.getPaths()) {
                IGeometry inter = VectorBooleans.combine(p, cutter, "intersection");
                if (inter instanceof Path) {
                    Path ip = (Path) inter;
                    if (ip.getPointCount() > 0) clipped.add(ip);
                } else if (inter instanceof Geometry) {
                    clipped.extend((Geometry) inter);
                }
            }
            return clipped;
        }

        return result;
    }

    /**
     * Generate an interlocking hexagonal honeycomb lattice of polygon cells.
     */
    public static Geometry hexGrid(long columns, long rows, double radius, double spacing, String orientation, Point center) {
        int cols = (int) Math.max(1, columns);
        int rws = (int) Math.max(1, rows);
        double r = Math.max(1.0, radius);
        double sp = Math.max(0.0, spacing);
        Point c = center != null ? center : Point.ZERO;
        boolean flat = "flat".equalsIgnoreCase(orientation);

        Geometry result = new Geometry();

        double stepX, stepY;
        if (!flat) {
            // Pointy top
            stepX = Math.sqrt(3.0) * (r + sp);
            stepY = 1.5 * (r + sp);
        } else {
            // Flat top
            stepX = 1.5 * (r + sp);
            stepY = Math.sqrt(3.0) * (r + sp);
        }

        double totalW = (cols - 1) * stepX;
        double totalH = (rws - 1) * stepY;

        for (int row = 0; row < rws; row++) {
            double rowOffset = (!flat && (row % 2 != 0)) ? stepX / 2.0 : 0.0;
            double py = c.y - totalH / 2.0 + row * stepY;

            for (int col = 0; col < cols; col++) {
                double colOffset = (flat && (col % 2 != 0)) ? stepY / 2.0 : 0.0;
                double px = c.x - totalW / 2.0 + col * stepX + rowOffset;
                double cy = py + colOffset;

                Path hex = new Path();
                double startAngle = flat ? 0.0 : 30.0;
                for (int i = 0; i < 6; i++) {
                    double rad = Math.toRadians(startAngle + i * 60.0);
                    double x = px + r * Math.cos(rad);
                    double y = cy + r * Math.sin(rad);
                    if (i == 0) hex.moveto(x, y);
                    else hex.lineto(x, y);
                }
                hex.close();
                result.add(hex);
            }
        }

        return result;
    }

    /**
     * Generate N-fold radial and bilateral mirror symmetry (Kaleidoscope / Rosette).
     */
    public static Geometry kaleidoscope(IGeometry shape, long segments, Point center, boolean mirror) {
        if (shape == null) return new Geometry();
        Geometry source = shape instanceof Geometry ? (Geometry) shape : ((Path) shape).asGeometry();
        int n = (int) Math.max(2, Math.min(segments, 128));
        Point c = center != null ? center : Point.ZERO;
        double angleStep = 360.0 / n;

        Geometry result = new Geometry();

        for (int i = 0; i < n; i++) {
            double a = i * angleStep;
            boolean shouldFlip = mirror && (i % 2 != 0);

            Transform t = new Transform();
            t.translate(c.x, c.y);
            t.rotate(a);
            if (shouldFlip) {
                t.scale(-1.0, 1.0);
            }
            t.translate(-c.x, -c.y);

            for (Path p : source.getPaths()) {
                result.add(t.map(p));
            }
        }

        return result;
    }

    /**
     * Deform vector vertices along mathematical waves (sine, triangle, or radial ripples).
     */
    public static Geometry waveWarp(IGeometry shape, String axis, String waveType, double amplitude, double frequency, double phase) {
        if (shape == null) return new Geometry();
        Geometry source = shape instanceof Geometry ? (Geometry) shape : ((Path) shape).asGeometry();
        final String ax = axis != null ? axis.toLowerCase() : "horizontal";
        final String wt = waveType != null ? waveType.toLowerCase() : "sine";
        final double amp = amplitude;
        final double freq = frequency;
        final double ph = Math.toRadians(phase);

        Geometry result = new Geometry();

        for (Path p : source.getPaths()) {
            Path warped = new Path(p, false);
            for (Contour contour : p.getContours()) {
                Contour newContour = new Contour();
                newContour.setClosed(contour.isClosed());
                for (Point pt : contour.getPoints()) {
                    double nx = pt.x;
                    double ny = pt.y;

                    if ("vertical".equals(ax)) {
                        double v = pt.y * freq + ph;
                        double d = calcWave(wt, v) * amp;
                        nx += d;
                    } else if ("radial".equals(ax)) {
                        double r = Math.hypot(pt.x, pt.y);
                        double ang = Math.atan2(pt.y, pt.x);
                        double d = calcWave(wt, r * freq + ph) * amp;
                        double newR = Math.max(0.0, r + d);
                        nx = newR * Math.cos(ang);
                        ny = newR * Math.sin(ang);
                    } else {
                        // horizontal (default)
                        double v = pt.x * freq + ph;
                        double d = calcWave(wt, v) * amp;
                        ny += d;
                    }
                    newContour.addPoint(new Point(nx, ny, pt.type));
                }
                warped.add(newContour);
            }
            result.add(warped);
        }

        return result;
    }

    private static double calcWave(String waveType, double v) {
        if ("triangle".equals(waveType)) {
            // Periodic triangle wave -1 to 1
            double norm = (v / Math.PI) % 2.0;
            if (norm < 0) norm += 2.0;
            return 2.0 * Math.abs(norm - 1.0) - 1.0;
        } else if ("square".equals(waveType)) {
            return Math.sin(v) >= 0 ? 1.0 : -1.0;
        }
        // Default sine wave
        return Math.sin(v);
    }

    /**
     * Radial lens distortion deformer (spherical bulge outward or pinch inward).
     */
    public static Geometry bulgePinch(IGeometry shape, Point center, double radius, double strength) {
        if (shape == null) return new Geometry();
        Geometry source = shape instanceof Geometry ? (Geometry) shape : ((Path) shape).asGeometry();
        final Point c = center != null ? center : Point.ZERO;
        final double r = Math.max(1.0, radius);
        final double str = Math.max(-1.0, Math.min(2.0, strength));

        Geometry result = new Geometry();

        for (Path p : source.getPaths()) {
            Path warped = new Path(p, false);
            for (Contour contour : p.getContours()) {
                Contour newContour = new Contour();
                newContour.setClosed(contour.isClosed());
                for (Point pt : contour.getPoints()) {
                    double dx = pt.x - c.x;
                    double dy = pt.y - c.y;
                    double dist = Math.hypot(dx, dy);

                    double nx = pt.x;
                    double ny = pt.y;

                    if (dist < r && dist > 1e-4) {
                        double u = dist / r; // 0 to 1
                        double factor;
                        if (str >= 0) {
                            // Bulge (magnify out)
                            factor = 1.0 + str * (1.0 - u * u);
                        } else {
                            // Pinch (pull in)
                            factor = 1.0 / (1.0 - str * (1.0 - u * u));
                        }
                        nx = c.x + dx * factor;
                        ny = c.y + dy * factor;
                    }
                    newContour.addPoint(new Point(nx, ny, pt.type));
                }
                warped.add(newContour);
            }
            result.add(warped);
        }

        return result;
    }

    /**
     * Generate parametric alternating stripes, checkerboard tiles, or radial sunburst rays.
     */
    public static Geometry stripes(double width, double height, long count, double ratio, double angle, String type) {
        double w = Math.max(1.0, width);
        double h = Math.max(1.0, height);
        int n = (int) Math.max(1, count);
        double rat = Math.max(0.01, Math.min(0.99, ratio));
        String t = type != null ? type.toLowerCase() : "linear";

        Geometry result = new Geometry();

        if ("radial".equals(t)) {
            // Sunburst rays
            double stepDeg = 360.0 / n;
            double raySpan = stepDeg * rat;
            double rayLen = Math.hypot(w, h);
            for (int i = 0; i < n; i++) {
                double a1 = Math.toRadians(i * stepDeg);
                double a2 = Math.toRadians(i * stepDeg + raySpan);
                Path ray = new Path();
                ray.moveto(0, 0);
                ray.lineto(rayLen * Math.cos(a1), rayLen * Math.sin(a1));
                ray.lineto(rayLen * Math.cos(a2), rayLen * Math.sin(a2));
                ray.close();
                result.add(ray);
            }
        } else if ("checker".equals(t)) {
            // Checkerboard grid
            double cellW = w / n;
            double cellH = h / n;
            for (int r = 0; r < n; r++) {
                double py = -h / 2.0 + (r + 0.5) * cellH;
                for (int c = 0; c < n; c++) {
                    if ((r + c) % 2 == 0) {
                        double px = -w / 2.0 + (c + 0.5) * cellW;
                        Path sq = new Path();
                        sq.rect(px, py, cellW * rat, cellH * rat);
                        result.add(sq);
                    }
                }
            }
        } else {
            // Linear stripes
            double stripeSpan = w / n;
            double stripeWidth = stripeSpan * rat;
            for (int i = 0; i < n; i++) {
                double x = -w / 2.0 + (i + 0.5) * stripeSpan;
                Path stripe = new Path();
                stripe.rect(x, 0, stripeWidth, h);
                result.add(stripe);
            }
        }

        if (Math.abs(angle) > 1e-3) {
            Transform rot = Transform.rotated(angle);
            Geometry rotated = new Geometry();
            for (Path p : result.getPaths()) {
                rotated.add(rot.map(p));
            }
            return rotated;
        }

        return result;
    }
}