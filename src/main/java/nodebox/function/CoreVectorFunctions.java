package nodebox.function;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import nodebox.graphics.*;
import nodebox.handle.*;
import nodebox.util.ChaikinSmoothing;
import nodebox.util.ConvexHull;
import nodebox.util.CornerRounding;
import nodebox.util.DelaunayVoronoi;
import nodebox.util.Duplicator;
import nodebox.util.MathUtils;
import nodebox.util.SimplexNoise;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.google.common.base.Preconditions.checkNotNull;
import static nodebox.function.MathFunctions.coordinates;

/**
 * Core vector function library.
 */
public class CoreVectorFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("corevector", CoreVectorFunctions.class,
                "generator", "filter",
                "align", "arc", "centroid", "colorize", "connect", "copy", "doNothing", "ellipse", "fit", "fitTo",
                "freehand", "grid", "group", "line", "lineAngle", "link", "makePoint", "point", "pointOnPath", "rect",
                "snap", "skew", "toPoints", "ungroup", "textpath",
                "delaunay", "voronoi", "convexHull", "smooth", "flowField", "lsystem",
                "attractor", "offsetPath", "traceContours",
                "typeset", "fitText", "textOnPath", "halftone", "truchetTiles", "guilloche", "metaballs",
                "modularGrid", "alignDistribute", "packCircles", "roughen", "longShadow", "extrude3d",
                "booleanOperation", "strokeStyle", "morph", "artboard",
                "roundCorners", "duplicator",
                "fourPointHandle", "freehandHandle", "lineAngleHandle", "lineHandle", "pointHandle", "snapHandle",
                "translateHandle");
    }

    /**
     * Example function that generates a path.
     * This is here so people can view and change the existing code.
     *
     * @return An example Path.
     */
    public static Path generator() {
        Path p = new Path();
        p.rect(0, 0, 100, 100);
        return p;
    }

    /**
     * Example function that rotates the given shape.
     *
     * @param geometry The input geometry.
     * @return The new, rotated geometry.
     */
    public static Geometry filter(Geometry geometry) {
        if (geometry == null) return null;
        Transform t = new Transform();
        t.rotate(45);
        return t.map(geometry);
    }

    /**
     * Align a shape in relation to the origin.
     *
     * @param geometry The input geometry.
     * @param position The point to align to.
     * @param hAlign   The horizontal align mode. Either "left", "right" or "center".
     * @param vAlign   The vertical align mode. Either "top", "bottom" or "middle".
     * @return The aligned Geometry. The original geometry is left intact.
     */
    public static AbstractGeometry align(AbstractGeometry geometry, Point position, String hAlign, String vAlign) {
        if (geometry == null) return null;
        double x = position.x;
        double y = position.y;
        Rect bounds = geometry.getBounds();
        double dx, dy;
        if (hAlign.equals("left")) {
            dx = x - bounds.x;
        } else if (hAlign.equals("right")) {
            dx = x - bounds.x - bounds.width;
        } else if (hAlign.equals("center")) {
            dx = x - bounds.x - bounds.width / 2;
        } else {
            dx = 0;
        }
        if (vAlign.equals("top")) {
            dy = y - bounds.y;
        } else if (vAlign.equals("bottom")) {
            dy = y - bounds.y - bounds.height;
        } else if (vAlign.equals("middle")) {
            dy = y - bounds.y - bounds.height / 2;
        } else {
            dy = 0;
        }

        Transform t = Transform.translated(dx, dy);
        if (geometry instanceof Path) {
            return t.map((Path) geometry);
        } else if (geometry instanceof Geometry) {
            return t.map((Geometry) geometry);
        } else {
            throw new IllegalArgumentException("Unknown geometry type " + geometry.getClass().getName());
        }
    }

    /**
     * Create an arc at the given position.
     * <p/>
     * Arcs rotate in the opposite direction from Java's Arc2D to be compatible with our transform functions.
     *
     * @param position   The position of the arc.
     * @param width      The arc width.
     * @param height     The arc height.
     * @param startAngle The start angle.
     * @param degrees    The amount of degrees.
     * @param arcType    The type of arc. Either "chord", "pie", or "open"
     * @return The new arc.
     */
    public static Path arc(Point position, double width, double height, double startAngle, double degrees, String arcType) {

        int awtType;
        if (arcType.equals("chord")) {
            awtType = Arc2D.CHORD;
        } else if (arcType.equals("pie")) {
            awtType = Arc2D.PIE;
        } else {
            awtType = Arc2D.OPEN;
        }
        return new Path(new Arc2D.Double(position.x - width / 2, position.y - height / 2, width, height,
                -startAngle, -degrees, awtType));
    }

    /**
     * Calculate the geometric center of a shape.
     *
     * @param shape The input shape.
     * @return a Point at the center of the input shape.
     */
    public static Point centroid(IGeometry shape) {
        if (shape == null) return Point.ZERO;
        Rect bounds = shape.getBounds();
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    /**
     * Change the color of a shape.
     *
     * @param shape       The input shape.
     * @param fill        The new fill color.
     * @param stroke      The new stroke color.
     * @param strokeWidth The new stroke width.
     * @return The new colored shape.
     */
    public static Colorizable colorize(Colorizable shape, Color fill, Color stroke, double strokeWidth) {
        if (shape == null) return null;
        Colorizable newShape = shape.clone();
        newShape.setFill(fill);
        if (strokeWidth > 0) {
            newShape.setStrokeColor(stroke);
            newShape.setStrokeWidth(strokeWidth);
        } else {
            newShape.setStrokeColor(null);
            newShape.setStrokeWidth(0);
        }
        return newShape;
    }

    /**
     * Connects all given points, in order, as a new path.
     *
     * @param points A list of points.
     * @param closed If true, close the path contour.
     * @return A new path with all points connected.
     */
    public static Path connect(List<Point> points, boolean closed) {
        if (points == null) return null;
        Path p = new Path();
        for (Point pt : points) {
            p.addPoint(pt);
        }
        if (closed)
            p.close();
        p.setFill(null);
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(1);
        return p;
    }


    public static List<IGeometry> copy(IGeometry shape, long copies, String order, Point translate, double rotate, Point scale) {
        ImmutableList.Builder<IGeometry> builder = ImmutableList.builder();
        Geometry geo = new Geometry();
        double tx = 0;
        double ty = 0;
        double r = 0;
        double sx = 1.0;
        double sy = 1.0;
        char[] cOrder = order.toCharArray();

        for (long i = 0; i < copies; i++) {
            Transform t = new Transform();

            // Each letter of the order describes an operation.
            for (char op : cOrder) {
                if (op == 't') {
                    t.translate(tx, ty);
                } else if (op == 'r') {
                    t.rotate(r);
                } else if (op == 's') {
                    t.scale(sx, sy);
                }
            }

            builder.add(t.map(shape));

            tx += translate.x;
            ty += translate.y;
            r += rotate;
            sx += scale.x / 100 - 1;
            sy += scale.y / 100 - 1;
        }
        return builder.build();
    }

    /**
     * Return the given object back, as-is.
     * <p/>
     * This function is used in nodes for organizational purposes.
     *
     * @param object The input object.
     * @return The unchanged input object.
     */
    public static Object doNothing(Object object) {
        return object;
    }

    /**
     * Create an ellipse at the given position.
     *
     * @param position The center position of the ellipse.
     * @param width    The ellipse width.
     * @param height   The ellipse height.
     * @return The new ellipse, as a Path.
     */
    public static Path ellipse(Point position, double width, double height) {
        Path p = new Path();
        p.ellipse(position.x, position.y, width, height);
        return p;
    }

    /**
     * Fit a shape within the given bounds.
     *
     * @param shape           The shape to fit.
     * @param position        The center of the target shape.
     * @param width           The width of the target bounds.
     * @param height          The height of the target shape.
     * @param keepProportions If true, the shape will not be stretched or squashed.
     * @return A new shape that fits within the given bounds.
     */
    public static IGeometry fit(IGeometry shape, Point position, double width, double height, boolean keepProportions) {
        if (shape == null) return null;

        Rect bounds = shape.getBounds();

        // Make sure bw and bh aren't infinitely small numbers.
        // This will lead to incorrect transformations with for examples lines.
        double bw = bounds.width > 0.000000000001 ? bounds.width : 0;
        double bh = bounds.height > 0.000000000001 ? bounds.height : 0;

        Transform t = new Transform();
        t.translate(position.x, position.y);
        double sx, sy;
        if (keepProportions) {
            // don't scale widths or heights that are equal to zero.
            sx = bw > 0 ? width / bw : Float.MAX_VALUE;
            sy = bh > 0 ? height / bh : Float.MAX_VALUE;
            sx = sy = Math.min(sx, sy);
        } else {
            sx = bw > 0 ? width / bw : 1;
            sy = bh > 0 ? height / bh : 1;
        }
        t.scale(sx, sy);
        t.translate(-bw / 2 - bounds.x, -bh / 2 - bounds.y);
        return t.map(shape);
    }

    /**
     * Fit a shape to another given shape.
     *
     * @param shape           The shape to fit.
     * @param bounding        The bounding, or target shape.
     * @param keepProportions If true, the shape will not be stretched or squashed.
     * @return A new shape that fits within the given bounding shape.
     */
    public static IGeometry fitTo(IGeometry shape, IGeometry bounding, boolean keepProportions) {
        if (shape == null) return null;
        if (bounding == null) return shape;

        Rect bounds = bounding.getBounds();
        return fit(shape, bounds.getCentroid(), bounds.width, bounds.height, keepProportions);
    }

    private final static Splitter PATH_SPLITTER = Splitter.on("M").omitEmptyStrings();
    private final static Splitter CONTOUR_SPLITTER = Splitter.on(" ").omitEmptyStrings();
    private final static Splitter POINT_SPLITTER = Splitter.on(",");

    /**
     * Create a new, open path with the given path string.
     * <p/>
     * The path string is composed of contour strings, starting with "M". Points are separated by a a space, e.g.:
     * <p/>
     * "M0,0 100,0 100,100 0,100M10,20 30,40 50,60"
     *
     * @param pathString The string to parse
     * @return a new Path.
     */
    public static Path freehand(String pathString) {
        if (pathString == null) return new Path();
        Path p = parsePath(pathString);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(1);
        return p;
    }

    /**
     * Create a grid of (rows * columns) points.
     * <p/>
     * The total width and height of the grid are given, and the
     * spacing between rows and columns is calculated.
     *
     * @param columns  The number of columns.
     * @param rows     The number of rows.
     * @param width    The total width of the grid.
     * @param height   The total height of the grid.
     * @param position The center position of the grid.
     * @return A list of Points.
     */
    public static List<Point> grid(long columns, long rows, double width, double height, Point position) {
        double columnSize, left, rowSize, top;
        if (columns > 1) {
            columnSize = width / (columns - 1);
            left = position.x - width / 2;
        } else {
            columnSize = left = position.x;
        }
        if (rows > 1) {
            rowSize = height / (rows - 1);
            top = position.y - height / 2;
        } else {
            rowSize = top = position.y;
        }

        ImmutableList.Builder<Point> builder = new ImmutableList.Builder<Point>();
        for (long rowIndex = 0; rowIndex < rows; rowIndex++) {
            for (long colIndex = 0; colIndex < columns; colIndex++) {
                double x = left + colIndex * columnSize;
                double y = top + rowIndex * rowSize;
                builder.add(new Point(x, y));
            }
        }
        return builder.build();
    }


    /**
     * Combine multiple shapes together into one Geometry.
     *
     * @param shapes The list of shapes (Path or Geometry objects) to combine.
     * @return The combined Geometry.
     */
    public static Geometry group(List<IGeometry> shapes) {
        if (shapes == null) return null;
        Geometry geo = new Geometry();
        for (IGeometry shape : shapes) {
            if (shape instanceof Path) {
                geo.add((Path) shape);
            } else if (shape instanceof Geometry) {
                geo.extend((Geometry) shape);
            } else {
                throw new RuntimeException("Unable to group " + shape + ": I can only group paths or geometry objects.");
            }
        }
        return geo;
    }

    /**
     * Create a line from point 1 to point 2.
     *
     * @param p1     The first point.
     * @param p2     The second point.
     * @param points The amount of points to generate along the line.
     * @return A line between two points.
     */
    public static Path line(Point p1, Point p2, long points) {
        Path p = new Path();
        p.line(p1.x, p1.y, p2.x, p2.y);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(1);
        p = p.resampleByAmount((int) points, true);
        return p;
    }

    /**
     * Create a line at the given starting point with the end point calculated by the angle and distance.
     *
     * @param point    The starting point of the line.
     * @param angle    The angle of the line.
     * @param distance The distance of the line.
     * @param points   The amount of points to generate along the line.
     * @return A new line.
     */
    public static Path lineAngle(Point point, double angle, double distance, long points) {
        Point p2 = coordinates(point, angle, distance);
        Path p = new Path();
        p.line(point.x, point.y, p2.x, p2.y);
        p.setFill(null);
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(1);
        p = p.resampleByAmount((int) points, true);
        return p;
    }

    /**
     * Create a path that visually links the two shapes together.
     * The shapes are only used for their bounding rectangles.
     *
     * @param shape1      The first shape.
     * @param shape2      The second shape.
     * @param orientation The link orientation, either "horizontal" or "vertical".
     * @return A new path.
     */
    public static Path link(Grob shape1, Grob shape2, String orientation) {
        if (shape1 == null || shape2 == null) return null;
        Path p = new Path();
        Rect a = shape1.getBounds();
        Rect b = shape2.getBounds();
        if (orientation.equals("horizontal")) {
            double hw = (b.x - (a.x + a.width)) / 2;
            p.moveto(a.x + a.width, a.y);
            p.curveto(a.x + a.width + hw, a.y, b.x - hw, b.y, b.x, b.y);
            p.lineto(b.x, b.y + b.height);
            p.curveto(b.x - hw, b.y + b.height, a.x + a.width + hw, a.y + a.height, a.x + a.width, a.y + a.height);
        } else {
            double hh = (b.y - (a.y + a.height)) / 2;
            p.moveto(a.x, a.y + a.height);
            p.curveto(a.x, a.y + a.height + hh, b.x, b.y - hh, b.x, b.y);
            p.lineto(b.x + b.width, b.y);
            p.curveto(b.x + b.width, b.y - hh, a.x + a.width, a.y + a.height + hh, a.x + a.width, a.y + a.height);
        }
        return p;
    }

    /**
     * Calculate a point on the given shape.
     *
     * @param shape The shape.
     * @param t     The position of the point, going from 0.0-100.0
     * @return The point on the given location of the path.
     */
    public static Point pointOnPath(AbstractGeometry shape, double t) {
        if (shape == null) return null;
        t = Math.abs(t % 100);
        return shape.pointAt(t / 100);
    }

    @SuppressWarnings("unchecked")
    public static Object skew(Object shape, Point skew, Point origin) {
        if (shape == null) return null;
        Transform t = new Transform();
        t.translate(origin);
        t.skew(skew.x, skew.y);
        t.translate(-origin.x, -origin.y);

        if (shape instanceof IGeometry) {
            return t.map((IGeometry) shape);
        } else if (shape instanceof List) {
            return t.map((List<Point>) shape);
        } else {
            throw new UnsupportedOperationException("I cannot work with " + shape.getClass().getSimpleName() + " objects.");
        }
    }

    /**
     * Snap the shape to a grid.
     *
     * @param shape    The shape to snap.
     * @param distance The grid size, or distance between grid lines.
     * @param strength The snap strength, between 0.0-100.0. If 0.0, no snapping occurs. If 100.0, all points are on the grid.
     * @param position The grid position.
     * @return The snapped geometry.
     */
    public static AbstractGeometry snap(AbstractGeometry shape, final double distance, final double strength, final Point position) {
        if (shape == null) return null;
        final double dStrength = strength / 100.0;
        return shape.mapPoints(new Function<Point, Point>() {
            public Point apply(Point point) {
                if (point == null) return Point.ZERO;
                double x = MathUtils.snap(point.x + position.x, distance, dStrength) - position.x;
                double y = MathUtils.snap(point.y + position.y, distance, dStrength) - position.y;
                return new Point(x, y, point.type);
            }
        });
    }

    /**
     * Create a rectangle.
     *
     * @param position  The center position of the rectangle.
     * @param width     The width of the rectangle.
     * @param height    The height of the rectangle.
     * @param roundness The roundness of the rectangle, given as a x,y Point. If the roundness is (0,0), we draw a normal rectangle.
     * @return The new rectangle.
     */
    public static Path rect(Point position, double width, double height, Point roundness) {
        Path p = new Path();
        if (roundness.equals(Point.ZERO)) {
            p.rect(position.x, position.y, width, height);
        } else {
            p.roundedRect(position.x, position.y, width, height, roundness.x, roundness.y);
        }
        return p;
    }

    /**
     * Get the points of a given shape.
     *
     * @param shape The input shape.
     * @return A list of all points of the shape.
     */
    public static List<Point> toPoints(IGeometry shape) {
        if (shape == null) return null;
        return shape.getPoints();
    }

    /**
     * Decompose the given geometry into paths.
     *
     * @param shape The input geometry
     * @return The list of contained paths.
     */
    public static List<Path> ungroup(IGeometry shape) {
        if (shape == null) return null;
        if (shape instanceof Geometry) {
            return ((Geometry) shape).getPaths();
        } else if (shape instanceof Path) {
            return ImmutableList.of((Path) shape);
        } else {
            throw new RuntimeException("Don't know how to decompose " + shape + " into paths.");
        }
    }

    /**
     * Create a text path.
     *
     * @return A new Path.
     */
    public static Path textpath(String text, String fontName, double fontSize, String alignment, Point position, double width) {
        Text.Align align;
        try {
            align = Text.Align.valueOf(alignment);
        } catch (IllegalArgumentException ignore) {
            align = Text.Align.CENTER;
        }
        if (align == Text.Align.LEFT) {
            position = position.moved(0, 0);
        } else if (align == Text.Align.CENTER) {
            position = position.moved(-width / 2, 0);
        } else if (align == Text.Align.RIGHT) {
            position = position.moved(-width, 0);
        }

        Text t = new Text(text, position.x, position.y, width, 0);
        t.setFontName(fontName);
        t.setFontSize(fontSize);
        t.setAlign(align);

        return t.getPath();
    }

    /**
     * Create a new point with the given x,y coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return A new Point.
     */
    public static Point makePoint(double x, double y) {
        return new Point(x, y);
    }

    /**
     * Return the given Point as-is.
     */
    public static Point point(Point value) {
        return value;
    }

    //// Utility functions ////

    public static Path parsePath(String s) {
        checkNotNull(s);
        Path p = new Path();
        s = s.trim();
        for (String pointString : PATH_SPLITTER.split(s)) {
            pointString = pointString.trim();
            if (!pointString.isEmpty()) {
                p.add(parseContour(pointString));
            }
        }
        return p;
    }

    public static Contour parseContour(String s) {
        s = s.replace(",", " ");
        Contour contour = new Contour();
        boolean parseX = true;
        Double x = null;
        String lastString = null;
        for (String pointString : CONTOUR_SPLITTER.split(s)) {
            lastString = pointString;
            Double d = Double.parseDouble(pointString);
            if (parseX) {
                x = d;
                parseX = false;
            } else {
                contour.addPoint(new Point(x, d));
                parseX = true;
            }
        }
        if (! parseX)
            throw new IllegalArgumentException("Could not parse point " + lastString);
        return contour;
    }

    public static Point parsePoint(String s) {
        Double x = null, y = null;
        for (String numberString : POINT_SPLITTER.split(s)) {
            if (x == null) {
                x = Double.parseDouble(numberString);
            } else if (y == null) {
                y = Double.parseDouble(numberString);
            } else {
                throw new IllegalArgumentException("Too many coordinates in point " + s);
            }
        }
        if (x != null && y != null) {
            return new Point(x, y);
        } else {
            throw new IllegalArgumentException("Could not parse point " + s);
        }
    }

    public static List<Path> delaunay(List<Point> points) {
        if (points == null || points.isEmpty()) return ImmutableList.of();
        return DelaunayVoronoi.trianglesToPaths(DelaunayVoronoi.triangulate(points));
    }

    public static List<Path> voronoi(List<Point> points, double width, double height, double inset) {
        if (points == null || points.isEmpty()) return ImmutableList.of();
        return DelaunayVoronoi.voronoiCells(points, width, height, inset);
    }

    public static Path convexHull(List<Point> points) {
        if (points == null || points.isEmpty()) return new Path();
        return ConvexHull.computeHullPath(points);
    }

    public static Geometry smooth(Geometry shape, long iterations, double tension) {
        if (shape == null) return null;
        return ChaikinSmoothing.smoothGeometry(shape, (int) iterations, tension);
    }

    public static List<Path> flowField(List<Point> points, Point position, long steps, double stepLength, double scale, double noiseStrength, long seed) {
        List<Point> startPoints = points;
        if (startPoints == null || startPoints.isEmpty()) {
            startPoints = ImmutableList.of(position != null ? position : Point.ZERO);
        }
        ImmutableList.Builder<Path> builder = ImmutableList.builder();
        long maxSteps = Math.max(1, Math.min(steps, 2000));
        for (Point pt : startPoints) {
            if (pt == null) continue;
            Path path = new Path();
            double cx = pt.x;
            double cy = pt.y;
            path.moveto(cx, cy);
            for (int s = 0; s < maxSteps; s++) {
                double angleDeg = SimplexNoise.noise2D((cx + seed * 100) * scale, (cy + seed * 100) * scale) * noiseStrength;
                double rad = Math.toRadians(angleDeg);
                cx += Math.cos(rad) * stepLength;
                cy += Math.sin(rad) * stepLength;
                path.lineto(cx, cy);
            }
            path.setFill(null);
            path.setStroke(Color.BLACK);
            path.setStrokeWidth(1);
            builder.add(path);
        }
        return builder.build();
    }

    private static class TurtleState {
        final double x, y, angle, len;
        TurtleState(double x, double y, double angle, double len) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.len = len;
        }
    }

    public static Path lsystem(String axiom, String rules, long generations, double angle, double length, double lengthScale, Point position) {
        if (axiom == null || axiom.isEmpty()) return new Path();
        Map<Character, String> ruleMap = new HashMap<Character, String>();
        if (rules != null && !rules.trim().isEmpty()) {
            for (String rawLine : rules.split("[;\\r\\n]+")) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("[:=]|->");
                if (parts.length >= 2 && !parts[0].trim().isEmpty()) {
                    ruleMap.put(parts[0].trim().charAt(0), parts[1].trim());
                }
            }
        }

        int gens = (int) Math.max(0, Math.min(generations, 8));
        String current = axiom;
        for (int g = 0; g < gens; g++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < current.length(); i++) {
                char c = current.charAt(i);
                String repl = ruleMap.get(c);
                if (repl != null) {
                    sb.append(repl);
                } else {
                    sb.append(c);
                }
                if (sb.length() > 200000) break;
            }
            current = sb.toString();
        }

        Path path = new Path();
        double startX = position != null ? position.x : 0.0;
        double startY = position != null ? position.y : 0.0;
        double curAngle = -90.0;
        double curLen = length * Math.pow(lengthScale, gens);
        if (curLen <= 0.0) curLen = 1.0;

        double curX = startX;
        double curY = startY;
        path.moveto(curX, curY);

        Deque<TurtleState> stack = new ArrayDeque<TurtleState>();
        for (int i = 0; i < current.length(); i++) {
            char c = current.charAt(i);
            switch (c) {
                case 'F':
                case 'G':
                    double rad = Math.toRadians(curAngle);
                    curX += Math.cos(rad) * curLen;
                    curY += Math.sin(rad) * curLen;
                    path.lineto(curX, curY);
                    break;
                case 'f':
                    double radF = Math.toRadians(curAngle);
                    curX += Math.cos(radF) * curLen;
                    curY += Math.sin(radF) * curLen;
                    path.moveto(curX, curY);
                    break;
                case '+':
                    curAngle += angle;
                    break;
                case '-':
                    curAngle -= angle;
                    break;
                case '|':
                    curAngle += 180.0;
                    break;
                case '[':
                    stack.push(new TurtleState(curX, curY, curAngle, curLen));
                    break;
                case ']':
                    if (!stack.isEmpty()) {
                        TurtleState state = stack.pop();
                        curX = state.x;
                        curY = state.y;
                        curAngle = state.angle;
                        curLen = state.len;
                        path.moveto(curX, curY);
                    }
                    break;
                default:
                    break;
            }
        }
        path.setFill(null);
        path.setStroke(Color.BLACK);
        path.setStrokeWidth(1);
        return path;
    }

    private static Point displacePoint(Point pt, Point center, double force, double radius, String mode) {
        if (pt == null) return Point.ZERO;
        if (center == null) center = Point.ZERO;
        double dx = center.x - pt.x;
        double dy = center.y - pt.y;
        double dist = Math.hypot(dx, dy);
        if (dist > radius || dist < 1e-4) {
            return pt;
        }

        String m = mode != null ? mode.toLowerCase(Locale.US) : "linear";
        double factor;
        if ("inverse".equals(m)) {
            factor = force * (1.0 - dist / radius) / (dist * 0.05 + 1.0);
        } else if ("inverse_squared".equals(m)) {
            double norm = dist / radius;
            factor = force * (1.0 - norm) / (norm * norm * 4.0 + 1.0);
        } else {
            factor = force * (1.0 - dist / radius);
        }

        double ux = dx / dist;
        double uy = dy / dist;

        if ("vortex".equals(m)) {
            return new Point(pt.x - uy * factor, pt.y + ux * factor, pt.type);
        } else if ("spiral".equals(m)) {
            return new Point(pt.x + ux * factor * 0.5 - uy * factor * 0.8, pt.y + uy * factor * 0.5 + ux * factor * 0.8, pt.type);
        } else {
            return new Point(pt.x + ux * factor, pt.y + uy * factor, pt.type);
        }
    }

    public static Object attractor(Object shape, final Point position, final double force, final double radius, final String mode) {
        if (shape == null) return null;
        if (shape instanceof AbstractGeometry) {
            return ((AbstractGeometry) shape).mapPoints(new Function<Point, Point>() {
                public Point apply(Point point) {
                    return displacePoint(point, position, force, radius, mode);
                }
            });
        } else if (shape instanceof List) {
            ImmutableList.Builder<Object> builder = ImmutableList.builder();
            for (Object o : (List<?>) shape) {
                if (o instanceof Point) {
                    builder.add(displacePoint((Point) o, position, force, radius, mode));
                } else if (o instanceof AbstractGeometry) {
                    builder.add(((AbstractGeometry) o).mapPoints(new Function<Point, Point>() {
                        public Point apply(Point point) {
                            return displacePoint(point, position, force, radius, mode);
                        }
                    }));
                } else {
                    builder.add(o);
                }
            }
            return builder.build();
        } else if (shape instanceof Point) {
            return displacePoint((Point) shape, position, force, radius, mode);
        }
        return shape;
    }

    public static Path offsetPath(IGeometry shape, double distance, String join) {
        if (shape == null) return new Path();
        String j = join != null ? join.toLowerCase(Locale.US) : "round";
        int joinMode = "miter".equals(j) ? BasicStroke.JOIN_MITER : ("bevel".equals(j) ? BasicStroke.JOIN_BEVEL : BasicStroke.JOIN_ROUND);
        int capMode = "square".equals(j) ? BasicStroke.CAP_SQUARE : ("butt".equals(j) ? BasicStroke.CAP_BUTT : BasicStroke.CAP_ROUND);
        BasicStroke bs = new BasicStroke((float) Math.max(0.1, Math.abs(distance) * 2.0), capMode, joinMode);
        java.awt.geom.GeneralPath gp = new java.awt.geom.GeneralPath();
        if (shape instanceof Path) {
            gp.append(((Path) shape).getGeneralPath(), false);
        } else if (shape instanceof Geometry) {
            for (Path p : ((Geometry) shape).getPaths()) {
                gp.append(p.getGeneralPath(), false);
            }
        }
        Shape stroked = bs.createStrokedShape(gp);
        Path result = new Path(stroked);
        result.setFill(Color.BLACK);
        result.setStroke(null);
        return result;
    }

    public static List<Path> traceContours(String file, double threshold, long resolution, Point position, double width, double height) {
        if (file == null || file.trim().isEmpty()) return ImmutableList.of();
        File f = new File(file.trim());
        if (!f.exists() || !f.isFile()) return ImmutableList.of();
        java.awt.image.BufferedImage img;
        try {
            img = javax.imageio.ImageIO.read(f);
        } catch (Exception e) {
            return ImmutableList.of();
        }
        if (img == null) return ImmutableList.of();

        int res = (int) Math.max(10, Math.min(resolution, 200));
        double thresh = Math.max(0.0, Math.min(100.0, threshold));
        double[][] grid = new double[res + 1][res + 1];

        int imgW = img.getWidth();
        int imgH = img.getHeight();

        for (int gy = 0; gy <= res; gy++) {
            for (int gx = 0; gx <= res; gx++) {
                int px = (int) Math.round((double) gx / res * (imgW - 1));
                int py = (int) Math.round((double) gy / res * (imgH - 1));
                int rgb = img.getRGB(px, py);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                grid[gx][gy] = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0 * 100.0;
            }
        }

        double cellW = (width > 0 ? width : 500.0) / res;
        double cellH = (height > 0 ? height : 500.0) / res;
        double originX = (position != null ? position.x : 0) - (width > 0 ? width : 500.0) / 2.0;
        double originY = (position != null ? position.y : 0) - (height > 0 ? height : 500.0) / 2.0;

        ImmutableList.Builder<Path> paths = ImmutableList.builder();
        for (int y = 0; y < res; y++) {
            for (int x = 0; x < res; x++) {
                double v0 = grid[x][y];
                double v1 = grid[x + 1][y];
                double v2 = grid[x + 1][y + 1];
                double v3 = grid[x][y + 1];

                int state = 0;
                if (v0 >= thresh) state |= 1;
                if (v1 >= thresh) state |= 2;
                if (v2 >= thresh) state |= 4;
                if (v3 >= thresh) state |= 8;

                if (state == 0 || state == 15) continue;

                double x0 = originX + x * cellW;
                double y0 = originY + y * cellH;
                double x1 = x0 + cellW;
                double y1 = y0 + cellH;

                Point top = new Point((x0 + x1) / 2.0, y0);
                Point right = new Point(x1, (y0 + y1) / 2.0);
                Point bottom = new Point((x0 + x1) / 2.0, y1);
                Point left = new Point(x0, (y0 + y1) / 2.0);

                Path segment = new Path();
                segment.setFill(null);
                segment.setStroke(Color.BLACK);
                segment.setStrokeWidth(1);

                switch (state) {
                    case 1: case 14: segment.line(left.x, left.y, top.x, top.y); break;
                    case 2: case 13: segment.line(top.x, top.y, right.x, right.y); break;
                    case 3: case 12: segment.line(left.x, left.y, right.x, right.y); break;
                    case 4: case 11: segment.line(right.x, right.y, bottom.x, bottom.y); break;
                    case 5:
                        segment.line(left.x, left.y, top.x, top.y);
                        segment.line(right.x, right.y, bottom.x, bottom.y);
                        break;
                    case 6: case 9: segment.line(top.x, top.y, bottom.x, bottom.y); break;
                    case 7: case 8: segment.line(left.x, left.y, bottom.x, bottom.y); break;
                    case 10:
                        segment.line(top.x, top.y, right.x, right.y);
                        segment.line(left.x, left.y, bottom.x, bottom.y);
                        break;
                }
                paths.add(segment);
            }
        }
        return paths.build();
    }

    public static Geometry typeset(String text, String fontName, double fontSize, String alignment, Point position, double width, double height, double lineHeight, double tracking, long columns, double columnGutter) {
        Geometry geo = new Geometry();
        if (text == null || text.trim().isEmpty()) return geo;

        int cols = (int) Math.max(1, columns);
        double totalW = width > 0 ? width : 400.0;
        double gutter = Math.max(0.0, columnGutter);
        double colW = Math.max(20.0, (totalW - (cols - 1) * gutter) / cols);
        double fs = Math.max(1.0, fontSize);
        double lh = Math.max(0.5, lineHeight) * fs;
        double posX = position != null ? position.x : 0;
        double posY = position != null ? position.y : 0;
        double maxH = height > 0 ? height : Double.MAX_VALUE;

        Font font = new Font(fontName != null && !fontName.isEmpty() ? fontName : "Helvetica", Font.PLAIN, (int) Math.round(fs));

        String[] paragraphs = text.split("\r?\n");
        int currentCol = 0;
        double currentY = 0;

        for (String para : paragraphs) {
            if (para.isEmpty()) {
                currentY += lh;
                continue;
            }

            AttributedString as = new AttributedString(para);
            as.addAttribute(TextAttribute.FONT, font);
            if (tracking != 0.0) {
                as.addAttribute(TextAttribute.TRACKING, tracking * 0.01);
            }

            AttributedCharacterIterator aci = as.getIterator();
            FontRenderContext frc = new FontRenderContext(null, true, true);
            LineBreakMeasurer measurer = new LineBreakMeasurer(aci, frc);

            while (measurer.getPosition() < aci.getEndIndex()) {
                TextLayout layout = measurer.nextLayout((float) colW);
                if (currentY + layout.getAscent() + layout.getDescent() > maxH && currentCol + 1 < cols) {
                    currentCol++;
                    currentY = 0;
                }
                currentY += layout.getAscent();

                double colX = posX + currentCol * (colW + gutter);
                double lineX = colX;
                if ("CENTER".equalsIgnoreCase(alignment)) {
                    lineX += (colW - layout.getVisibleAdvance()) / 2.0;
                } else if ("RIGHT".equalsIgnoreCase(alignment)) {
                    lineX += (colW - layout.getVisibleAdvance());
                }

                AffineTransform at = AffineTransform.getTranslateInstance(lineX, posY + currentY);
                Shape outline = layout.getOutline(at);
                Path p = new Path(outline);
                p.setFillColor(Color.BLACK);
                geo.add(p);

                currentY += layout.getDescent() + layout.getLeading() + (lh - fs);
            }
        }
        return geo;
    }

    public static Path fitText(String text, String fontName, Point position, double width, double height, double minFontSize, double maxFontSize, String alignment) {
        if (text == null || text.isEmpty()) return new Path();
        double low = Math.max(1.0, minFontSize);
        double high = Math.max(low, maxFontSize > 0 ? maxFontSize : 300.0);
        double reqW = width > 0 ? width : 0;
        double reqH = height > 0 ? height : 0;

        String fn = fontName != null && !fontName.isEmpty() ? fontName : "Helvetica";

        if (reqW > 0 || reqH > 0) {
            for (int i = 0; i < 16; i++) {
                double mid = (low + high) / 2.0;
                Font f = new Font(fn, Font.PLAIN, (int) Math.round(mid));
                FontRenderContext frc = new FontRenderContext(null, true, true);
                TextLayout tl = new TextLayout(text, f, frc);
                Rectangle2D b = tl.getBounds();

                boolean fitsW = reqW <= 0 || b.getWidth() <= reqW;
                boolean fitsH = reqH <= 0 || b.getHeight() <= reqH;

                if (fitsW && fitsH) {
                    low = mid;
                } else {
                    high = mid;
                }
            }
        }

        return textpath(text, fn, low, alignment, position != null ? position : Point.ZERO, reqW);
    }

    public static Path textOnPath(String text, IGeometry shape, String fontName, double fontSize, String alignment, double margin, double baselineOffset, double tracking, boolean flip) {
        Path result = new Path();
        if (text == null || text.isEmpty() || shape == null) return result;

        Path path;
        if (shape instanceof Path) {
            path = (Path) shape;
        } else if (shape instanceof Geometry && !((Geometry) shape).getPaths().isEmpty()) {
            path = ((Geometry) shape).getPaths().get(0);
        } else {
            return result;
        }

        double totalLen = path.getLength();
        if (totalLen <= 0) return result;

        String fn = fontName != null && !fontName.isEmpty() ? fontName : "Helvetica";
        double fs = Math.max(1.0, fontSize);
        Font font = new Font(fn, Font.PLAIN, (int) Math.round(fs));
        FontRenderContext frc = new FontRenderContext(null, true, true);

        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        FontMetrics fm = g2d.getFontMetrics(font);

        double trackPx = tracking * 0.01 * fs;
        double totalTextW = 0;
        double[] charWidths = new double[text.length()];
        for (int i = 0; i < text.length(); i++) {
            charWidths[i] = fm.charWidth(text.charAt(i)) + trackPx;
            totalTextW += charWidths[i];
        }

        double startT = (margin / 100.0) % 1.0;
        if (startT < 0) startT += 1.0;

        if ("CENTER".equalsIgnoreCase(alignment)) {
            startT = (startT - (totalTextW / 2.0) / totalLen) % 1.0;
        } else if ("RIGHT".equalsIgnoreCase(alignment)) {
            startT = (startT - totalTextW / totalLen) % 1.0;
        }
        if (startT < 0) startT += 1.0;

        double currentT = startT;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            double cw = charWidths[i];
            if (Character.isWhitespace(ch)) {
                currentT = (currentT + cw / totalLen) % 1.0;
                continue;
            }

            double charMidT = (currentT + (cw / 2.0) / totalLen) % 1.0;
            Point pt = path.pointAt(charMidT);
            Point ptNext = path.pointAt((charMidT + 0.0005) % 1.0);
            double angle = Math.atan2(ptNext.y - pt.y, ptNext.x - pt.x);

            if (flip) {
                angle += Math.PI;
            }

            GlyphVector gv = font.createGlyphVector(frc, String.valueOf(ch));
            Shape glyphShape = gv.getOutline();

            AffineTransform at = new AffineTransform();
            at.translate(pt.x, pt.y);
            at.rotate(angle);
            double effBase = flip ? baselineOffset : -baselineOffset;
            at.translate(-cw / 2.0, effBase);

            result.extend(at.createTransformedShape(glyphShape));
            currentT = (currentT + cw / totalLen) % 1.0;
        }

        return result;
    }

    public static Geometry halftone(String imageFile, Point position, double width, double height, double dotSpacing, double angle, String shapeType, double scale, double threshold, boolean invert) {
        Geometry geo = new Geometry();
        if (imageFile == null || imageFile.trim().isEmpty()) return geo;
        BufferedImage img = null;
        try {
            img = javax.imageio.ImageIO.read(new File(imageFile.trim()));
        } catch (Exception e) { /* ignore */ }
        if (img == null) return geo;

        int imgW = img.getWidth();
        int imgH = img.getHeight();
        double w = width > 0 ? width : imgW;
        double h = height > 0 ? height : imgH;
        double sp = Math.max(3.0, dotSpacing);
        double sc = Math.max(0.1, scale);
        double posX = position != null ? position.x : 0;
        double posY = position != null ? position.y : 0;

        double rad = Math.toRadians(angle);
        double cosA = Math.cos(rad);
        double sinA = Math.sin(rad);

        double diag = Math.sqrt(w * w + h * h);
        int steps = (int) Math.ceil(diag / sp);

        String type = shapeType != null ? shapeType.toLowerCase().trim() : "dot";

        for (int iy = -steps; iy <= steps; iy++) {
            double gy = iy * sp;
            for (int ix = -steps; ix <= steps; ix++) {
                double gx = ix * sp;

                // Rotate grid point
                double rx = gx * cosA - gy * sinA;
                double ry = gx * sinA + gy * cosA;

                if (Math.abs(rx) > w / 2.0 || Math.abs(ry) > h / 2.0) continue;

                // Map to image coordinate
                int px = (int) Math.round((rx + w / 2.0) / w * (imgW - 1));
                int py = (int) Math.round((ry + h / 2.0) / h * (imgH - 1));
                px = Math.max(0, Math.min(imgW - 1, px));
                py = Math.max(0, Math.min(imgH - 1, py));

                int argb = img.getRGB(px, py);
                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;
                double lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
                if (invert) lum = 1.0 - lum;

                if (lum < threshold) continue;

                double dotSize = (1.0 - lum) * sp * sc;
                if (dotSize < 0.5) continue;

                double cx = posX + rx;
                double cy = posY + ry;

                Path p = new Path();
                p.setFillColor(Color.BLACK);
                if ("line".equals(type)) {
                    p.rect(cx, cy, dotSize, sp * 1.3);
                    p.rotate(angle);
                } else if ("cross".equals(type)) {
                    p.rect(cx, cy, dotSize, sp * 1.2);
                    p.rotate(angle);
                    Path p2 = new Path();
                    p2.setFillColor(Color.BLACK);
                    p2.rect(cx, cy, sp * 1.2, dotSize);
                    p2.rotate(angle);
                    geo.add(p2);
                } else {
                    // dot
                    p.ellipse(cx, cy, dotSize, dotSize);
                }
                geo.add(p);
            }
        }
        return geo;
    }

    public static Geometry truchetTiles(Point position, double width, double height, double tileSize, String tileType, double lineWidth, long seed) {
        Geometry geo = new Geometry();
        double w = width > 0 ? width : 400.0;
        double h = height > 0 ? height : 400.0;
        double ts = Math.max(8.0, tileSize);
        double lw = Math.max(0.5, lineWidth);
        double posX = position != null ? position.x : 0;
        double posY = position != null ? position.y : 0;

        int cols = (int) Math.ceil(w / ts);
        int rows = (int) Math.ceil(h / ts);
        double startX = posX - (cols * ts) / 2.0;
        double startY = posY - (rows * ts) / 2.0;

        Random rng = new Random(seed);
        String type = tileType != null ? tileType.toLowerCase().trim() : "arcs";

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double cx = startX + c * ts;
                double cy = startY + r * ts;
                int rot = rng.nextInt(4);

                if ("diagonals".equals(type)) {
                    Path p = new Path();
                    p.setStroke(Color.BLACK);
                    p.setStrokeWidth(lw);
                    p.setFill(null);
                    if (rot % 2 == 0) {
                        p.line(cx, cy, cx + ts, cy + ts);
                    } else {
                        p.line(cx + ts, cy, cx, cy + ts);
                    }
                    geo.add(p);
                } else if ("triangles".equals(type)) {
                    Path p = new Path();
                    p.setFillColor(Color.BLACK);
                    if (rot == 0) {
                        p.moveto(cx, cy); p.lineto(cx + ts, cy); p.lineto(cx, cy + ts); p.close();
                    } else if (rot == 1) {
                        p.moveto(cx + ts, cy); p.lineto(cx + ts, cy + ts); p.lineto(cx, cy); p.close();
                    } else if (rot == 2) {
                        p.moveto(cx + ts, cy + ts); p.lineto(cx, cy + ts); p.lineto(cx + ts, cy); p.close();
                    } else {
                        p.moveto(cx, cy + ts); p.lineto(cx, cy); p.lineto(cx + ts, cy + ts); p.close();
                    }
                    geo.add(p);
                } else {
                    // "arcs" - use Arc2D shapes
                    if (rot % 2 == 0) {
                        Path arc1 = new Path(new Arc2D.Double(cx - ts / 2.0, cy - ts / 2.0, ts, ts, 0, -90, Arc2D.OPEN));
                        arc1.setStroke(Color.BLACK);
                        arc1.setStrokeWidth(lw);
                        arc1.setFill(null);
                        Path arc2 = new Path(new Arc2D.Double(cx + ts / 2.0, cy + ts / 2.0, ts, ts, 180, -90, Arc2D.OPEN));
                        arc2.setStroke(Color.BLACK);
                        arc2.setStrokeWidth(lw);
                        arc2.setFill(null);
                        geo.add(arc1);
                        geo.add(arc2);
                    } else {
                        Path arc1 = new Path(new Arc2D.Double(cx + ts / 2.0, cy - ts / 2.0, ts, ts, 90, 90, Arc2D.OPEN));
                        arc1.setStroke(Color.BLACK);
                        arc1.setStrokeWidth(lw);
                        arc1.setFill(null);
                        Path arc2 = new Path(new Arc2D.Double(cx - ts / 2.0, cy + ts / 2.0, ts, ts, 270, 90, Arc2D.OPEN));
                        arc2.setStroke(Color.BLACK);
                        arc2.setStrokeWidth(lw);
                        arc2.setFill(null);
                        geo.add(arc1);
                        geo.add(arc2);
                    }
                }
            }
        }
        return geo;
    }

    public static Path guilloche(Point position, double majorRadius, double minorRadius, double offset, double steps, double revolutions, double modulationAmp, double modulationFreq) {
        Path p = new Path();
        p.setStroke(Color.BLACK);
        p.setStrokeWidth(1.0);
        p.setFill(null);

        double R = majorRadius != 0 ? majorRadius : 120.0;
        double r = Math.max(0.1, minorRadius != 0 ? minorRadius : 40.0);
        double d = offset != 0 ? offset : 50.0;
        int N = (int) Math.max(100, Math.min(50000, steps > 0 ? steps : 2000));
        double revs = revolutions > 0 ? revolutions : 10.0;
        double cx = position != null ? position.x : 0;
        double cy = position != null ? position.y : 0;

        double maxTheta = revs * 2.0 * Math.PI;

        for (int i = 0; i <= N; i++) {
            double theta = (double) i / N * maxTheta;
            double rm = r + modulationAmp * Math.sin(modulationFreq * theta);
            if (Math.abs(rm) < 0.001) rm = 0.001;

            double diff = R - rm;
            double ratio = diff * theta / rm;
            double x = cx + diff * Math.cos(theta) + d * Math.cos(ratio);
            double y = cy + diff * Math.sin(theta) - d * Math.sin(ratio);

            if (i == 0) {
                p.moveto(x, y);
            } else {
                p.lineto(x, y);
            }
        }
        return p;
    }

    public static IGeometry metaballs(List<?> circles, double threshold, double handleFactor, boolean unite) {
        List<nodebox.graphics.Metaballs.Circle> circleList = new ArrayList<nodebox.graphics.Metaballs.Circle>();
        if (circles != null) {
            for (Object obj : circles) {
                if (obj instanceof Path) {
                    Rect b = ((Path) obj).getBounds();
                    circleList.add(new nodebox.graphics.Metaballs.Circle(b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0, Math.max(b.getWidth(), b.getHeight()) / 2.0));
                } else if (obj instanceof Point) {
                    Point pt = (Point) obj;
                    circleList.add(new nodebox.graphics.Metaballs.Circle(pt.x, pt.y, 25.0));
                } else if (obj instanceof Rect) {
                    Rect r = (Rect) obj;
                    circleList.add(new nodebox.graphics.Metaballs.Circle(r.getX() + r.getWidth() / 2.0, r.getY() + r.getHeight() / 2.0, Math.max(r.getWidth(), r.getHeight()) / 2.0));
                }
            }
        }
        return nodebox.graphics.Metaballs.createMetaballs(circleList, threshold, handleFactor, unite);
    }

    public static Geometry modularGrid(Point position, double width, double height, long columns, long rows, double colGutter, double rowGutter, double topMargin, double bottomMargin, double leftMargin, double rightMargin, String outputMode) {
        Geometry geo = new Geometry();
        double w = width > 0 ? width : 800.0;
        double h = height > 0 ? height : 600.0;
        int cols = (int) Math.max(1, columns);
        int rws = (int) Math.max(1, rows);
        double cg = Math.max(0.0, colGutter);
        double rg = Math.max(0.0, rowGutter);
        double tm = Math.max(0.0, topMargin);
        double bm = Math.max(0.0, bottomMargin);
        double lm = Math.max(0.0, leftMargin);
        double rm = Math.max(0.0, rightMargin);
        double posX = position != null ? position.x : 0;
        double posY = position != null ? position.y : 0;

        double inW = Math.max(1.0, w - lm - rm);
        double inH = Math.max(1.0, h - tm - bm);
        double cellW = Math.max(1.0, (inW - (cols - 1) * cg) / cols);
        double cellH = Math.max(1.0, (inH - (rws - 1) * rg) / rws);

        double startX = posX - w / 2.0 + lm;
        double startY = posY - h / 2.0 + tm;

        String mode = outputMode != null ? outputMode.toLowerCase().trim() : "cells";

        if ("cells".equals(mode) || "all".equals(mode)) {
            for (int r = 0; r < rws; r++) {
                for (int c = 0; c < cols; c++) {
                    double cx = startX + c * (cellW + cg);
                    double cy = startY + r * (cellH + rg);
                    Path cell = new Path();
                    cell.rect(cx + cellW / 2.0, cy + cellH / 2.0, cellW, cellH);
                    cell.setFill(null);
                    cell.setStroke(Color.BLACK);
                    cell.setStrokeWidth(1.0);
                    geo.add(cell);
                }
            }
        }
        if ("lines".equals(mode) || "all".equals(mode)) {
            Path mBox = new Path();
            mBox.rect(posX - w / 2.0 + lm + inW / 2.0, posY - h / 2.0 + tm + inH / 2.0, inW, inH);
            mBox.setFill(null);
            mBox.setStroke(new Color(0.8, 0.2, 0.2));
            mBox.setStrokeWidth(1.0);
            geo.add(mBox);

            for (int c = 1; c < cols; c++) {
                double divX = startX + c * (cellW + cg) - cg / 2.0;
                Path vLine = new Path();
                vLine.line(divX, startY, divX, startY + inH);
                vLine.setStroke(new Color(0.5, 0.5, 0.5));
                vLine.setStrokeWidth(0.5);
                geo.add(vLine);
            }
            for (int r = 1; r < rws; r++) {
                double divY = startY + r * (cellH + rg) - rg / 2.0;
                Path hLine = new Path();
                hLine.line(startX, divY, startX + inW, divY);
                hLine.setStroke(new Color(0.5, 0.5, 0.5));
                hLine.setStrokeWidth(0.5);
                geo.add(hLine);
            }
        }
        return geo;
    }

    public static List<IGeometry> alignDistribute(List<?> shapes, String align, String distribute, double spacing) {
        List<IGeometry> result = new ArrayList<IGeometry>();
        if (shapes == null || shapes.isEmpty()) return result;

        List<IGeometry> valid = new ArrayList<IGeometry>();
        for (Object s : shapes) {
            if (s instanceof IGeometry) valid.add((IGeometry) s);
        }
        if (valid.isEmpty()) return result;

        String al = align != null ? align.toLowerCase().trim() : "none";
        String dist = distribute != null ? distribute.toLowerCase().trim() : "none";

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (IGeometry g : valid) {
            Rect b = g.getBounds();
            if (b.getX() < minX) minX = b.getX();
            double bMaxX = b.getX() + b.getWidth();
            if (bMaxX > maxX) maxX = bMaxX;
            if (b.getY() < minY) minY = b.getY();
            double bMaxY = b.getY() + b.getHeight();
            if (bMaxY > maxY) maxY = bMaxY;
        }
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;

        List<IGeometry> aligned = new ArrayList<IGeometry>();
        for (IGeometry g : valid) {
            Rect b = g.getBounds();
            double dx = 0, dy = 0;
            if ("left".equals(al)) dx = minX - b.getX();
            else if ("center".equals(al)) dx = midX - (b.getX() + b.getWidth() / 2.0);
            else if ("right".equals(al)) dx = maxX - (b.getX() + b.getWidth());

            if ("top".equals(al)) dy = minY - b.getY();
            else if ("middle".equals(al)) dy = midY - (b.getY() + b.getHeight() / 2.0);
            else if ("bottom".equals(al)) dy = maxY - (b.getY() + b.getHeight());

            if (dx != 0 || dy != 0) {
                Transform t = Transform.translated(dx, dy);
                aligned.add(t.map(g));
            } else {
                aligned.add(g);
            }
        }

        if ("horizontal".equals(dist) && aligned.size() > 1) {
            Collections.sort(aligned, new Comparator<IGeometry>() {
                @Override
                public int compare(IGeometry a, IGeometry b) {
                    return Double.compare(a.getBounds().getX(), b.getBounds().getX());
                }
            });
            double currentX = aligned.get(0).getBounds().getX();
            for (int i = 0; i < aligned.size(); i++) {
                IGeometry g = aligned.get(i);
                Rect b = g.getBounds();
                double dx = currentX - b.getX();
                Transform t = Transform.translated(dx, 0);
                IGeometry shifted = t.map(g);
                result.add(shifted);
                currentX += shifted.getBounds().getWidth() + spacing;
            }
        } else if ("vertical".equals(dist) && aligned.size() > 1) {
            Collections.sort(aligned, new Comparator<IGeometry>() {
                @Override
                public int compare(IGeometry a, IGeometry b) {
                    return Double.compare(a.getBounds().getY(), b.getBounds().getY());
                }
            });
            double currentY = aligned.get(0).getBounds().getY();
            for (int i = 0; i < aligned.size(); i++) {
                IGeometry g = aligned.get(i);
                Rect b = g.getBounds();
                double dy = currentY - b.getY();
                Transform t = Transform.translated(0, dy);
                IGeometry shifted = t.map(g);
                result.add(shifted);
                currentY += shifted.getBounds().getHeight() + spacing;
            }
        } else {
            result.addAll(aligned);
        }

        return result;
    }

    public static List<Path> packCircles(List<?> radii, Point center, double containerRadius, long iterations, long seed) {
        List<Double> radList = new ArrayList<Double>();
        if (radii != null) {
            for (Object obj : radii) {
                if (obj instanceof Number) {
                    radList.add(((Number) obj).doubleValue());
                }
            }
        }
        return nodebox.graphics.CirclePacker.pack(radList, center, containerRadius, (int) iterations, (int) seed);
    }

    public static IGeometry roughen(IGeometry shape, double size, double detail, String method, long seed) {
        if (shape == null) return null;
        double sz = Math.max(0.0, size);
        if (sz == 0.0) return shape;

        double dt = Math.max(0.1, detail);
        double maxSegLen = 20.0 / dt;
        boolean smooth = "smooth".equalsIgnoreCase(method);
        Random rng = new Random(seed);

        List<Path> paths = shape instanceof Path ? ImmutableList.of((Path) shape) : ((Geometry) shape).getPaths();
        Geometry result = new Geometry();

        for (Path p : paths) {
            Path newP = new Path();
            newP.setFillColor(p.getFillColor());
            newP.setStrokeColor(p.getStrokeColor());
            newP.setStrokeWidth(p.getStrokeWidth());

            for (Contour c : p.getContours()) {
                List<Point> pts = c.getPoints();
                if (pts.size() < 2) {
                    newP.add(c);
                    continue;
                }

                Contour newC = new Contour();
                newC.setClosed(c.isClosed());

                for (int i = 0; i < pts.size(); i++) {
                    Point p1 = pts.get(i);
                    Point p2 = pts.get((i + 1) % pts.size());
                    if (i == pts.size() - 1 && !c.isClosed()) {
                        double off = smooth ? SimplexNoise.simplex3D(p1.x * 0.05, p1.y * 0.05, (double) seed) * sz : (rng.nextDouble() * 2.0 - 1.0) * sz;
                        newC.addPoint(new Point(p1.x + off, p1.y + off, p1.type));
                        break;
                    }

                    double d = Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y));
                    int subdivs = (int) Math.max(1, Math.ceil(d / maxSegLen));

                    for (int s = 0; s < subdivs; s++) {
                        double t = (double) s / subdivs;
                        double x = p1.x + t * (p2.x - p1.x);
                        double y = p1.y + t * (p2.y - p1.y);

                        double nx = -(p2.y - p1.y) / (d > 0.0001 ? d : 1.0);
                        double ny = (p2.x - p1.x) / (d > 0.0001 ? d : 1.0);

                        double off = smooth ? SimplexNoise.simplex3D(x * 0.05, y * 0.05, (double) seed) * sz : (rng.nextDouble() * 2.0 - 1.0) * sz;
                        newC.addPoint(new Point(x + nx * off, y + ny * off, Point.LINE_TO));
                    }
                }
                newP.add(newC);
            }
            result.add(newP);
        }

        return shape instanceof Path ? result.getPaths().get(0) : result;
    }

    public static Geometry longShadow(IGeometry shape, double angle, double distance, double fade) {
        Geometry geo = new Geometry();
        if (shape == null) return geo;

        double rad = Math.toRadians(angle);
        double dist = Math.max(1.0, distance);
        double dx = Math.cos(rad) * dist;
        double dy = Math.sin(rad) * dist;

        int steps = (int) Math.max(5, Math.min(60, dist / 3.0));
        Area shadowArea = new Area();

        List<Path> paths = shape instanceof Path ? ImmutableList.of((Path) shape) : ((Geometry) shape).getPaths();
        for (Path p : paths) {
            Shape awt = p.getGeneralPath();
            for (int s = 0; s <= steps; s++) {
                double frac = (double) s / steps;
                AffineTransform at = AffineTransform.getTranslateInstance(dx * frac, dy * frac);
                shadowArea.add(new Area(at.createTransformedShape(awt)));
            }
        }

        Path shadowPath = new Path(shadowArea);
        double alpha = Math.max(0.05, Math.min(1.0, 0.45 * (1.0 - Math.min(1.0, fade) * 0.5)));
        shadowPath.setFillColor(new Color(0, 0, 0, alpha));
        geo.add(shadowPath);

        for (Path p : paths) {
            geo.add(p);
        }

        return geo;
    }

    public static Geometry extrude3d(IGeometry shape, double depth, double angle, Color frontColor, Color sideColor, double lightIntensity) {
        Geometry geo = new Geometry();
        if (shape == null) return geo;

        double rad = Math.toRadians(angle);
        double d = Math.max(0.0, depth);
        double dx = Math.cos(rad) * d;
        double dy = Math.sin(rad) * d;

        Color sideBase = sideColor != null ? sideColor : new Color(0.35, 0.35, 0.4);
        Color front = frontColor != null ? frontColor : new Color(0.85, 0.85, 0.9);
        double li = Math.max(0.0, Math.min(100.0, lightIntensity)) / 100.0;

        double lx = -0.7071;
        double ly = -0.7071;

        List<Path> paths = shape instanceof Path ? ImmutableList.of((Path) shape) : ((Geometry) shape).getPaths();

        for (Path p : paths) {
            for (Contour c : p.getContours()) {
                List<Point> pts = c.getPoints();
                if (pts.size() < 2) continue;

                int count = c.isClosed() ? pts.size() : pts.size() - 1;
                for (int i = 0; i < count; i++) {
                    Point p1 = pts.get(i);
                    Point p2 = pts.get((i + 1) % pts.size());

                    Point q1 = new Point(p1.x + dx, p1.y + dy);
                    Point q2 = new Point(p2.x + dx, p2.y + dy);

                    double edgeX = p2.x - p1.x;
                    double edgeY = p2.y - p1.y;
                    double edgeLen = Math.sqrt(edgeX * edgeX + edgeY * edgeY);
                    if (edgeLen < 0.0001) continue;

                    double nx = -edgeY / edgeLen;
                    double ny = edgeX / edgeLen;

                    double dot = nx * lx + ny * ly;
                    double factor = Math.max(0.2, Math.min(1.0, 0.5 + 0.5 * dot * li));

                    Color quadColor = new Color(
                            Math.max(0.0, Math.min(1.0, sideBase.getRed() * factor)),
                            Math.max(0.0, Math.min(1.0, sideBase.getGreen() * factor)),
                            Math.max(0.0, Math.min(1.0, sideBase.getBlue() * factor)),
                            sideBase.getAlpha()
                    );

                    Path quad = new Path();
                    quad.setFillColor(quadColor);
                    quad.moveto(p1.x, p1.y);
                    quad.lineto(p2.x, p2.y);
                    quad.lineto(q2.x, q2.y);
                    quad.lineto(q1.x, q1.y);
                    quad.close();
                    geo.add(quad);
                }
            }
        }

        for (Path p : paths) {
            Path frontPath = new Path(p);
            frontPath.setFillColor(front);
            frontPath.transform(Transform.translated(dx, dy));
            geo.add(frontPath);
        }

        return geo;
    }

    //// Handles ////

    public static Handle fourPointHandle() {
        return new FourPointHandle();
    }

    public static Handle freehandHandle() {
        return new FreehandHandle();
    }

    public static Handle lineAngleHandle() {
        CombinedHandle handle = new CombinedHandle();
        handle.addHandle(new PointHandle());
        handle.addHandle(new RotateHandle("angle", "position"));
        return handle;
    }

    public static Handle lineHandle() {
        return new LineHandle();
    }

    public static Handle pointHandle() {
        return new PointHandle();
    }

    public static Handle snapHandle() {
        return new SnapHandle();
    }

    public static Handle translateHandle() {
        return new TranslateHandle();
    }

    public static IGeometry booleanOperation(IGeometry shape1, IGeometry shape2, String operation) {
        return nodebox.graphics.VectorBooleans.combine(shape1, shape2, operation);
    }

    public static Geometry strokeStyle(IGeometry shape, double dashLength, double gapLength, double dashPhase, String cap, String join, boolean startArrow, boolean endArrow, double arrowSize) {
        return nodebox.graphics.StrokeStyler.apply(shape, dashLength, gapLength, dashPhase, cap, join, startArrow, endArrow, arrowSize);
    }

    public static Path morph(IGeometry shapeA, IGeometry shapeB, double progress, long samples) {
        return nodebox.graphics.ShapeMorpher.morph(shapeA, shapeB, progress, samples);
    }

    public static nodebox.graphics.Artboard artboard(IGeometry shape, String name, String preset, double width, double height, Point position, boolean clip, Color background, boolean showFrame) {
        return new nodebox.graphics.Artboard(name, preset, width, height, position, clip, background, showFrame, shape);
    }

    public static Geometry roundCorners(IGeometry shape, double radius, String type, double threshold, boolean clamp) {
        if (shape == null) return null;
        Geometry g = shape instanceof Geometry ? (Geometry) shape : ((Path) shape).asGeometry();
        return CornerRounding.roundCorners(g, radius, type, threshold, clamp);
    }

    public static List<Geometry> duplicator(IGeometry shape, String mode, long count, double spacing, double angle,
                                            long columns, long rows, double spacingX, double spacingY, double stagger,
                                            double radius, double startAngle, double endAngle, boolean orient,
                                            double growthRate, double angleStep,
                                            double stepRotation, double stepScale, double stepOpacity,
                                            double jitterPos, double jitterRot, double jitterScale, long seed) {
        if (shape == null) return ImmutableList.of();
        Geometry g = shape instanceof Geometry ? (Geometry) shape : ((Path) shape).asGeometry();
        return Duplicator.duplicate(g, mode, count, spacing, angle, columns, rows, spacingX, spacingY, stagger,
                radius, startAngle, endAngle, orient, growthRate, angleStep,
                stepRotation, stepScale, stepOpacity, jitterPos, jitterRot, jitterScale, seed);
    }

}
