package nodebox.graphics;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

/**
 * Constructive Solid Geometry (CSG) 2D boolean engine for vector shapes.
 * Supports Union, Difference (Subtract), Intersection, and XOR (Exclude).
 */
public final class VectorBooleans {

    public enum Operation {
        UNION,
        DIFFERENCE,
        INTERSECTION,
        XOR
    }

    private VectorBooleans() {
    }

    /**
     * Perform a 2D boolean operation between two shapes.
     *
     * @param shape1    the primary shape
     * @param shape2    the secondary shape (or cutter)
     * @param operation union, difference (or subtract), intersection, or xor
     * @return resulting geometry
     */
    public static IGeometry combine(IGeometry shape1, IGeometry shape2, String operation) {
        if (shape1 == null && shape2 == null) return new Path();
        if (shape1 == null) return shape2;
        if (shape2 == null) return shape1;

        Operation op = parseOperation(operation);
        Area a1 = toArea(shape1);
        Area a2 = toArea(shape2);

        switch (op) {
            case UNION:
                a1.add(a2);
                break;
            case DIFFERENCE:
                a1.subtract(a2);
                break;
            case INTERSECTION:
                a1.intersect(a2);
                break;
            case XOR:
                a1.exclusiveOr(a2);
                break;
        }

        if (a1.isEmpty()) {
            return new Path();
        }

        Path result = new Path(a1);
        // Inherit styling from shape1
        if (shape1 instanceof Path) {
            Path p1 = (Path) shape1;
            result.setFill(p1.getFill());
            result.setStroke(p1.getStroke());
            result.setStrokeWidth(p1.getStrokeWidth());
        } else if (shape1 instanceof Geometry) {
            Geometry g1 = (Geometry) shape1;
            if (!g1.getPaths().isEmpty()) {
                Path first = g1.getPaths().get(0);
                result.setFill(first.getFill());
                result.setStroke(first.getStroke());
                result.setStrokeWidth(first.getStrokeWidth());
            }
        }
        return result;
    }

    private static Operation parseOperation(String op) {
        if (op == null) return Operation.UNION;
        String s = op.trim().toLowerCase();
        if (s.contains("sub") || s.contains("diff")) {
            return Operation.DIFFERENCE;
        } else if (s.contains("inter")) {
            return Operation.INTERSECTION;
        } else if (s.contains("xor") || s.contains("excl")) {
            return Operation.XOR;
        }
        return Operation.UNION;
    }

    private static Area toArea(IGeometry geom) {
        Area area = new Area();
        if (geom instanceof Path) {
            Path p = (Path) geom;
            area.add(new Area(p.getGeneralPath()));
        } else if (geom instanceof Geometry) {
            Geometry g = (Geometry) geom;
            for (Path p : g.getPaths()) {
                area.add(new Area(p.getGeneralPath()));
            }
        }
        return area;
    }
}
