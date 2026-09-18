package nodebox.graphics;

import com.google.common.collect.ImmutableList;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Advanced stroke styling engine providing dash arrays, dash phase animation,
 * line caps, line joins, and automatic directional arrowheads at endpoints.
 */
public final class StrokeStyler {

    private StrokeStyler() {
    }

    public static Geometry apply(IGeometry shape, double dashLength, double gapLength, double dashPhase,
                                 String cap, String join, boolean startArrow, boolean endArrow, double arrowSize) {
        Geometry geo = new Geometry();
        if (shape == null) return geo;

        List<Path> paths = shape instanceof Path ? ImmutableList.of((Path) shape) : ((Geometry) shape).getPaths();
        if (paths.isEmpty()) return geo;

        int capStyle = BasicStroke.CAP_BUTT;
        if ("round".equalsIgnoreCase(cap)) capStyle = BasicStroke.CAP_ROUND;
        else if ("square".equalsIgnoreCase(cap)) capStyle = BasicStroke.CAP_SQUARE;

        int joinStyle = BasicStroke.JOIN_MITER;
        if ("round".equalsIgnoreCase(join)) joinStyle = BasicStroke.JOIN_ROUND;
        else if ("bevel".equalsIgnoreCase(join)) joinStyle = BasicStroke.JOIN_BEVEL;

        float[] dashArray = null;
        if (dashLength > 0.0 && gapLength > 0.0) {
            dashArray = new float[]{(float) dashLength, (float) gapLength};
        }

        double sz = arrowSize > 0.0 ? arrowSize : 10.0;

        for (Path origPath : paths) {
            double strokeW = origPath.getStrokeWidth() > 0 ? origPath.getStrokeWidth() : 1.0;
            Color strokeColor = origPath.getStroke() != null ? origPath.getStroke() : Color.BLACK;

            if (dashArray != null) {
                BasicStroke bs = new BasicStroke((float) strokeW, capStyle, joinStyle, 10.0f, dashArray, (float) dashPhase);
                Shape dashedShape = bs.createStrokedShape(origPath.getGeneralPath());
                Path stroked = new Path(dashedShape);
                stroked.setFill(strokeColor);
                stroked.setStroke(null);
                geo.add(stroked);
            } else {
                Path clone = origPath.clone();
                clone.setStroke(strokeColor);
                clone.setStrokeWidth(strokeW);
                geo.add(clone);
            }

            // Add arrowheads if requested and path has points
            List<Point> pts = origPath.getPoints();
            if (pts.size() >= 2 && !origPath.isClosed()) {
                if (startArrow) {
                    Point p0 = pts.get(0);
                    Point p1 = pts.get(1);
                    double angle = Math.atan2(p0.y - p1.y, p0.x - p1.x);
                    geo.add(createArrowhead(p0.x, p0.y, angle, sz, strokeColor));
                }
                if (endArrow) {
                    Point pn = pts.get(pts.size() - 1);
                    Point prev = pts.get(pts.size() - 2);
                    double angle = Math.atan2(pn.y - prev.y, pn.x - prev.x);
                    geo.add(createArrowhead(pn.x, pn.y, angle, sz, strokeColor));
                }
            }
        }

        return geo;
    }

    private static Path createArrowhead(double tipX, double tipY, double angle, double size, Color color) {
        Path arrow = new Path();
        double wingAngle = Math.PI / 6.0; // 30 degrees
        double leftAngle = angle + Math.PI - wingAngle;
        double rightAngle = angle + Math.PI + wingAngle;

        double x1 = tipX + size * Math.cos(leftAngle);
        double y1 = tipY + size * Math.sin(leftAngle);
        double x2 = tipX + size * Math.cos(rightAngle);
        double y2 = tipY + size * Math.sin(rightAngle);

        arrow.moveto(tipX, tipY);
        arrow.lineto(x1, y1);
        arrow.lineto(x2, y2);
        arrow.close();

        arrow.setFill(color);
        arrow.setStroke(null);
        return arrow;
    }
}
