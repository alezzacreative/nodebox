package nodebox.graphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedural shape morpher that resamples two arbitrary vector paths or geometries
 * and smoothly tweens between their vertices with parameter t in [0.0, 1.0].
 */
public final class ShapeMorpher {

    private ShapeMorpher() {
    }

    /**
     * Morph smoothly between shapeA and shapeB at parameter t.
     *
     * @param shapeA   starting shape (at t=0)
     * @param shapeB   ending shape (at t=1)
     * @param progress interpolation parameter 0.0 to 1.0
     * @param samples  number of sampled perimeter vertices
     * @return interpolated path
     */
    public static Path morph(IGeometry shapeA, IGeometry shapeB, double progress, long samples) {
        if (shapeA == null && shapeB == null) return new Path();
        if (shapeA == null) return toSinglePath(shapeB);
        if (shapeB == null) return toSinglePath(shapeA);

        double t = Math.max(0.0, Math.min(1.0, progress));
        if (t <= 0.0) return toSinglePath(shapeA);
        if (t >= 1.0) return toSinglePath(shapeB);

        int n = (int) Math.max(12, Math.min(2000, samples));
        Path pA = toSinglePath(shapeA);
        Path pB = toSinglePath(shapeB);

        Point[] ptsA = pA.makePoints(n, false);
        Point[] ptsB = pB.makePoints(n, false);

        if (ptsA == null || ptsA.length == 0) return pB;
        if (ptsB == null || ptsB.length == 0) return pA;

        int count = Math.min(ptsA.length, ptsB.length);
        List<Point> morphedPoints = new ArrayList<Point>(count);

        for (int i = 0; i < count; i++) {
            Point a = ptsA[i];
            Point b = ptsB[i];
            double x = a.x * (1.0 - t) + b.x * t;
            double y = a.y * (1.0 - t) + b.y * t;
            morphedPoints.add(new Point(x, y));
        }

        // Build path through morphed vertices
        Path result = new Path();
        if (!morphedPoints.isEmpty()) {
            Point first = morphedPoints.get(0);
            result.moveto(first.x, first.y);
            for (int i = 1; i < morphedPoints.size(); i++) {
                Point pt = morphedPoints.get(i);
                result.lineto(pt.x, pt.y);
            }
            if (pA.isClosed() || pB.isClosed()) {
                result.close();
            }
        }

        // Interpolate colors
        Color fillA = pA.getFill();
        Color fillB = pB.getFill();
        if (fillA != null && fillB != null) {
            result.setFill(ColorHarmony.blend(fillA, fillB, "normal", t));
        } else if (fillA != null) {
            result.setFill(fillA);
        } else if (fillB != null) {
            result.setFill(fillB);
        }

        Color strokeA = pA.getStroke();
        Color strokeB = pB.getStroke();
        if (strokeA != null && strokeB != null) {
            result.setStroke(ColorHarmony.blend(strokeA, strokeB, "normal", t));
        } else if (strokeA != null) {
            result.setStroke(strokeA);
        } else if (strokeB != null) {
            result.setStroke(strokeB);
        }

        double swA = pA.getStrokeWidth();
        double swB = pB.getStrokeWidth();
        result.setStrokeWidth(swA * (1.0 - t) + swB * t);

        return result;
    }

    private static Path toSinglePath(IGeometry geom) {
        if (geom instanceof Path) {
            return (Path) geom;
        } else if (geom instanceof Geometry) {
            Geometry g = (Geometry) geom;
            Path combined = new Path();
            for (Path p : g.getPaths()) {
                combined.extend(p);
            }
            if (!g.getPaths().isEmpty()) {
                Path first = g.getPaths().get(0);
                combined.setFill(first.getFill());
                combined.setStroke(first.getStroke());
                combined.setStrokeWidth(first.getStrokeWidth());
            }
            return combined;
        }
        return new Path();
    }
}
