package nodebox.client;
import nodebox.node.Node;
import nodebox.node.Port;
import nodebox.ui.Theme;
import nodebox.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * NodeDocumentation provides offline, rich HTML tooltips for any Node.
 * It formats the title, category, description, inputs/ports with types and descriptions,
 * and output type matching the official NodeBox documentation structure.
 */
public class NodeDocumentation {

    private static final Map<String, String> NODE_DOCS = new HashMap<String, String>();
    private static final Map<String, String> PORT_DOCS = new HashMap<String, String>();

    static {
        // Built-in node documentation matching official NodeBox 3 reference (offline)
        register("colorize", "Change the color of a shape.", "geometry");
        registerPort("colorize", "fill", "The new fill color. Set alpha to 0 for no fill.");
        registerPort("colorize", "stroke", "The new stroke color.");
        registerPort("colorize", "strokeWidth", "The new stroke width. Set to 0 for no stroke.");

        register("rect", "Create a rectangle, square or rounded rectangle.", "geometry");
        registerPort("rect", "position", "The center point of the rectangle.");
        registerPort("rect", "width", "The width of the rectangle.");
        registerPort("rect", "height", "The height of the rectangle.");
        registerPort("rect", "roundness", "The corner roundness of the rectangle (X and Y).");

        register("ellipse", "Create an ellipse or circle.", "geometry");
        registerPort("ellipse", "position", "The center point of the ellipse.");
        registerPort("ellipse", "width", "The width of the ellipse.");
        registerPort("ellipse", "height", "The height of the ellipse.");

        register("line", "Create a line segment between two points.", "geometry");
        registerPort("line", "point1", "The start point of the line.");
        registerPort("line", "point2", "The end point of the line.");

        register("arc", "Create a circular arc, pie slice or chord.", "geometry");
        registerPort("arc", "position", "The center point of the arc.");
        registerPort("arc", "width", "The width of the arc.");
        registerPort("arc", "height", "The height of the arc.");
        registerPort("arc", "startAngle", "The start angle in degrees.");
        registerPort("arc", "degrees", "The arc angular span in degrees.");
        registerPort("arc", "type", "The type of arc: pie, chord or open.");

        register("star", "Create a regular star polygon with pointed tips.", "geometry");
        registerPort("star", "position", "The center point of the star.");
        registerPort("star", "points", "The number of star points.");
        registerPort("star", "outer", "The outer radius to the point tips.");
        registerPort("star", "inner", "The inner radius between the points.");

        register("polygon", "Create a regular polygon with equal sides.", "geometry");
        registerPort("polygon", "position", "The center point of the polygon.");
        registerPort("polygon", "sides", "The number of polygon sides.");
        registerPort("polygon", "radius", "The radius from the center to vertex points.");
        registerPort("polygon", "align", "Align the polygon flat on an edge.");

        register("textpath", "Create vector outline geometry from a text string.", "geometry");
        registerPort("textpath", "text", "The text string to render.");
        registerPort("textpath", "font", "The font family name.");
        registerPort("textpath", "size", "The font point size.");
        registerPort("textpath", "align", "Text alignment: left, center or right.");
        registerPort("textpath", "position", "The base position of the text.");

        register("grid", "Create a grid of points arranged in columns and rows.", "point");
        registerPort("grid", "columns", "The number of columns.");
        registerPort("grid", "rows", "The number of rows.");
        registerPort("grid", "width", "The total width of the grid.");
        registerPort("grid", "height", "The total height of the grid.");
        registerPort("grid", "position", "The center position of the grid.");

        register("copy", "Create multiple transformed copies of a shape.", "geometry");
        registerPort("copy", "copies", "The number of copies to create.");
        registerPort("copy", "order", "Transformation order (translate, rotate, scale).");
        registerPort("copy", "translate", "The offset delta to move each copy.");
        registerPort("copy", "rotate", "The incremental angle to rotate each copy.");
        registerPort("copy", "scale", "The incremental scale applied to each copy.");

        register("transform", "Transform geometry with translation, rotation and scale.", "geometry");
        registerPort("transform", "translate", "Translation offset point.");
        registerPort("transform", "rotate", "Rotation angle in degrees.");
        registerPort("transform", "scale", "Scaling factor point (100, 100 is 100%).");

        register("compound", "Combine vector shapes using boolean union, difference or intersection.", "geometry");
        registerPort("compound", "shape1", "The first input shape.");
        registerPort("compound", "shape2", "The second input shape.");
        registerPort("compound", "function", "Boolean operation: united, subtracted, or intersected.");

        register("connect", "Connect a list of points into a vector path contour.", "geometry");
        registerPort("connect", "points", "The list of points to connect.");
        registerPort("connect", "closed", "Whether to close the path into a loop.");

        register("fit", "Fit and scale a shape within rectangular bounding limits.", "geometry");
        registerPort("fit", "position", "Target center position.");
        registerPort("fit", "width", "Target maximum width.");
        registerPort("fit", "height", "Target maximum height.");
        registerPort("fit", "keep_proportions", "Preserve shape aspect ratio without stretching.");

        register("resample", "Distribute points evenly along path contours.", "point");
        registerPort("resample", "method", "Resampling method: by segment length or point amount.");
        registerPort("resample", "length", "Maximum segment length between points.");
        registerPort("resample", "points", "Total number of points.");

        register("scatter", "Scatter random points within the bounding area of a shape.", "point");
        registerPort("scatter", "amount", "Number of scattered points.");
        registerPort("scatter", "seed", "Random seed variation.");

        register("wiggle", "Add randomized jitter displacement to points or geometry.", "geometry");
        registerPort("wiggle", "offset", "Maximum displacement amount in X and Y.");
        registerPort("wiggle", "seed", "Random seed variation.");

        register("align", "Align a shape relative to horizontal and vertical reference anchors.", "geometry");
        registerPort("align", "halign", "Horizontal alignment: left, center, right, or none.");
        registerPort("align", "valign", "Vertical alignment: top, middle, bottom, or none.");

        register("sample", "Generate a list of evenly spaced floating-point numbers.", "float");
        registerPort("sample", "amount", "The number of samples to generate.");
        registerPort("sample", "start", "The start value of the sample sequence.");
        registerPort("sample", "end", "The end value of the sample sequence.");

        register("range", "Generate a sequence of numbers from start to end with a step.", "float");
        registerPort("range", "start", "Starting number.");
        registerPort("range", "end", "Ending limit.");
        registerPort("range", "step", "Increment step size.");

        // Newly developed nodes
        register("noise", "Sample continuous coherent Simplex/Perlin gradient noise in 1D, 2D, or 3D.", "float");
        registerPort("noise", "x", "X coordinate in noise space.");
        registerPort("noise", "y", "Y coordinate in noise space.");
        registerPort("noise", "z", "Z coordinate / time evolution in noise space.");
        registerPort("noise", "scale", "Spatial frequency scale multiplier.");
        registerPort("noise", "octaves", "Fractal Brownian motion (fBm) octaves.");

        register("flow_field", "Generate organic vector streamlines guided by a Simplex noise vector field.", "geometry");
        registerPort("flow_field", "seedPoints", "Seed points from which streamlines originate.");
        registerPort("flow_field", "steps", "Number of step segments per streamline.");
        registerPort("flow_field", "stepLength", "Length of each step segment.");
        registerPort("flow_field", "noiseScale", "Spatial frequency / scale of the noise field.");

        register("lsystem", "Generate fractal branching structures and space-filling curves using an L-System.", "geometry");
        registerPort("lsystem", "axiom", "The initial axiom string (e.g. F).");
        registerPort("lsystem", "rules", "Grammar rewrite rules (e.g. F=F+F-F-F+F).");
        registerPort("lsystem", "generations", "Number of recursive rewriting iterations.");
        registerPort("lsystem", "angle", "Turning angle in degrees for + and -.");

        register("voronoi", "Compute Voronoi cellular diagram polygon cells from input points.", "geometry");
        registerPort("voronoi", "points", "Input seed points.");
        registerPort("voronoi", "bounds", "Clipping boundary bounds rectangle.");

        register("delaunay", "Generate a Delaunay triangulation mesh from input points.", "geometry");
        registerPort("delaunay", "points", "Input vertex points to triangulate.");

        register("convex_hull", "Compute the tightest convex bounding polygon enclosing a set of points.", "geometry");
        registerPort("convex_hull", "points", "Input points.");

        register("attractor", "Attract, repel, or swirl points and vector geometry around a center point.", "geometry");
        registerPort("attractor", "position", "Center attractor position.");
        registerPort("attractor", "force", "Force magnitude (positive attracts, negative repels).");
        registerPort("attractor", "radius", "Maximum influence radius.");
        registerPort("attractor", "mode", "Attenuation mode: linear, inverse, vortex, or spiral.");

        register("offset_path", "Expand or shrink a vector path by an offset distance.", "geometry");
        registerPort("offset_path", "distance", "Offset distance (positive expands, negative contracts).");
        registerPort("offset_path", "join", "Corner join style: round, miter, or bevel.");

        register("trace_contours", "Extract vector isoline contours from an image at a brightness threshold.", "geometry");
        registerPort("trace_contours", "file", "Path to the input image file.");
        registerPort("trace_contours", "threshold", "Brightness threshold isoline level (0 - 100).");

        register("typeset", "Format and wrap text into rectangular editorial multi-column layouts.", "geometry");
        registerPort("typeset", "text", "The input text content to typeset.");
        registerPort("typeset", "columns", "Number of editorial text columns.");
        registerPort("typeset", "columnWidth", "Width of each column.");
        registerPort("typeset", "gutter", "Spacing between columns.");

        register("fit_text", "Auto-scale text size to precisely fill a target bounding box.", "geometry");
        registerPort("fit_text", "text", "The text string to fit.");
        registerPort("fit_text", "width", "Target box width.");
        registerPort("fit_text", "height", "Target box height.");

        register("text_on_path", "Flow and deform text characters smoothly along any vector curve.", "geometry");
        registerPort("text_on_path", "text", "Text characters to flow.");
        registerPort("text_on_path", "path", "Guide curve path.");

        register("halftone", "Generate printable dot-matrix screen halftones from images.", "geometry");
        registerPort("halftone", "imageFile", "Image to sample brightness from.");
        registerPort("halftone", "gridSize", "Halftone cell spacing.");
        registerPort("halftone", "maxDotSize", "Maximum halftone dot diameter.");

        register("truchet_tiles", "Generate decorative interlocking multi-scale Truchet tiling patterns.", "geometry");
        registerPort("truchet_tiles", "columns", "Number of tile columns.");
        registerPort("truchet_tiles", "rows", "Number of tile rows.");
        registerPort("truchet_tiles", "tileSize", "Dimension of each tile cell.");
        registerPort("truchet_tiles", "type", "Tile style: quarter_circles, diagonal_lines, or triangle.");

        register("guilloche", "Generate high-security intricate spirograph rosette curves.", "geometry");
        registerPort("guilloche", "R", "Radius of the fixed outer circle.");
        registerPort("guilloche", "r", "Radius of the rolling circle.");
        registerPort("guilloche", "d", "Pen distance from rolling circle center.");
        registerPort("guilloche", "steps", "Curve sample resolution.");

        register("metaballs", "Create organic fluid metaball blobs that merge smoothly when nearby.", "geometry");
        registerPort("metaballs", "points", "Center positions of metaball particles.");
        registerPort("metaballs", "radii", "Radii of particles.");
        registerPort("metaballs", "threshold", "Iso-surface blending threshold.");

        register("harmony", "Generate harmonic color schemes based on color wheel geometry.", "color");
        registerPort("harmony", "baseColor", "Root base color.");
        registerPort("harmony", "scheme", "Scheme: complementary, analogous, triadic, tetradic, or split.");

        register("extract_palette", "Extract dominant color palette swatches from an image via K-Means.", "color");
        registerPort("extract_palette", "imageFile", "Path to input image file.");
        registerPort("extract_palette", "count", "Number of dominant colors to extract.");

        register("blend_mode", "Blend two colors using standard graphic design blend modes.", "color");
        registerPort("blend_mode", "baseColor", "Base backdrop color.");
        registerPort("blend_mode", "blendColor", "Overlay blend color.");
        registerPort("blend_mode", "mode", "Blend mode: multiply, screen, overlay, darken, lighten, etc.");

        register("barchart", "Generate styled vector bar chart graphics from numerical data.", "geometry");
        registerPort("barchart", "values", "Numerical values to plot.");
        registerPort("barchart", "width", "Total chart width.");
        registerPort("barchart", "height", "Total chart height.");

        register("donut", "Generate a donut or ring pie chart sector from data values.", "geometry");
        registerPort("donut", "values", "Data slice proportions.");
        registerPort("donut", "innerRadius", "Inner hole radius.");
        registerPort("donut", "outerRadius", "Outer ring radius.");

        register("spring", "Simulate physics-based spring dampening motion.", "float");
        registerPort("spring", "stiffness", "Spring stiffness coefficient.");
        registerPort("spring", "damping", "Friction damping factor.");

        register("smooth", "Smooth jagged polygonal paths using Chaikin corner-cutting.", "geometry");
        registerPort("smooth", "iterations", "Number of smoothing passes.");

        register("artboard", "Define an export frame artboard for multi-canvas page layout and automated batch export.", "geometry");
        registerPort("artboard", "shape", "Vector shape content to place inside the artboard.");
        registerPort("artboard", "name", "Name of the artboard (used as export filename).");
        registerPort("artboard", "preset", "Predefined dimension standard: Instagram Story, Post, Twitter, A4, etc.");
        registerPort("artboard", "width", "Custom width in points/pixels.");
        registerPort("artboard", "height", "Custom height in points/pixels.");
        registerPort("artboard", "position", "Artboard center position coordinates.");
        registerPort("artboard", "clip", "Clip graphics extending beyond the artboard boundaries.");
        registerPort("artboard", "background", "Artboard background backdrop fill color.");
        registerPort("artboard", "showFrame", "Display artboard boundary outline and title label in editor.");
    }

    private static void register(String nodeName, String desc, String outType) {
        NODE_DOCS.put(nodeName.toLowerCase(), desc);
    }

    private static void registerPort(String nodeName, String portName, String desc) {
        PORT_DOCS.put(nodeName.toLowerCase() + "." + portName.toLowerCase(), desc);
    }

    /**
     * Generate a self-contained, offline HTML tooltip string for the given node.
     */
    public static String getHtmlTooltip(Node node) {
        if (node == null) return null;

        boolean dark = Theme.isDark();
        String title = StringUtils.humanizeName(node.getName());
        String rawName = node.getName();

        // Retrieve description from node or fallback dictionary
        String description = node.getDescription();
        if (description == null || description.trim().isEmpty() || description.equals("Base node to be extended for custom nodes.")) {
            description = NODE_DOCS.get(rawName.toLowerCase());
            if (description == null && node.getPrototype() != null) {
                description = NODE_DOCS.get(node.getPrototype().getName().toLowerCase());
            }
        }
        if (description == null || description.trim().isEmpty()) {
            description = "NodeBox component.";
        }

        // Colors
        String bgColor = dark ? "#18181b" : "#ffffff";
        String borderColor = dark ? "#3f3f46" : "#cbd5e1";
        String titleColor = dark ? "#f4f4f5" : "#0f172a";
        String subColor = dark ? "#38bdf8" : "#0284c7";
        String textColor = dark ? "#e4e4e7" : "#334155";
        String portNameColor = dark ? "#f4f4f5" : "#0f172a";
        String portTypeColor = dark ? "#94a3b8" : "#64748b";
        String descColor = dark ? "#cbd5e1" : "#475569";
        String dividerColor = dark ? "#27272a" : "#e2e8f0";

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family: sans-serif; font-size: 11px; margin: 4px; padding: 4px; width: 340px; background-color: ")
          .append(bgColor).append("; color: ").append(textColor).append(";'>");

        // Header
        sb.append("<div style='border-bottom: 1px solid ").append(borderColor).append("; padding-bottom: 4px; margin-bottom: 6px;'>");
        sb.append("<span style='font-size: 13px; font-weight: bold; color: ").append(titleColor).append(";'>").append(title).append("</span>");
        if (!title.equalsIgnoreCase(rawName)) {
            sb.append("<span style='font-size: 10px; color: ").append(portTypeColor).append("; margin-left: 5px;'>(").append(rawName).append(")</span>");
        }

        String cat = node.getCategory();
        if (cat == null || cat.isEmpty()) {
            if (node.getFunction() != null && node.getFunction().contains("/")) {
                cat = node.getFunction().split("/")[0];
            }
        }
        if (cat != null && !cat.isEmpty()) {
            sb.append("<span style='color: ").append(subColor).append("; font-size: 10px; font-weight: bold; margin-left: 8px;'>[")
              .append(cat.toUpperCase()).append("]</span>");
        }
        sb.append("</div>");

        // Description
        sb.append("<p style='margin: 0 0 8px 0; line-height: 1.3;'>").append(description).append("</p>");

        // Inputs
        sb.append("<div style='font-weight: bold; font-size: 10px; color: ").append(subColor).append("; margin-bottom: 4px; letter-spacing: 0.5px;'>INPUTS:</div>");
        sb.append("<table cellpadding='1' cellspacing='0' style='font-size: 11px; width: 100%;'>");

        int inputCount = 0;
        for (Port p : node.getInputs()) {
            String pName = p.getName();
            if (pName.startsWith("_") || p.getType().equals(Port.TYPE_STATE) || p.getType().equals(Port.TYPE_CONTEXT)) {
                continue;
            }
            inputCount++;
            String pLabel = p.getDisplayLabel();
            String pType = p.getType();
            if (p.hasListRange()) {
                pType = pType + " list";
            }
            String pDesc = p.getDescription();
            if (pDesc == null || pDesc.trim().isEmpty()) {
                pDesc = PORT_DOCS.get(rawName.toLowerCase() + "." + pName.toLowerCase());
            }
            if (pDesc == null || pDesc.trim().isEmpty()) {
                pDesc = "-";
            }

            sb.append("<tr>");
            sb.append("<td valign='top' style='font-weight: bold; color: ").append(portNameColor).append("; padding-right: 6px; white-space: nowrap;'>")
              .append(pLabel).append("</td>");
            sb.append("<td valign='top' style='color: ").append(portTypeColor).append("; padding-right: 8px; font-size: 10px; white-space: nowrap;'>(")
              .append(pType).append(")</td>");
            sb.append("<td valign='top' style='color: ").append(descColor).append(";'>").append(pDesc).append("</td>");
            sb.append("</tr>");
        }
        if (inputCount == 0) {
            sb.append("<tr><td colspan='3' style='color: ").append(portTypeColor).append("; font-style: italic;'>None</td></tr>");
        }
        sb.append("</table>");

        // Output
        String outType = node.getOutputType();
        if (outType != null && !outType.isEmpty()) {
            if (node.getOutputRange() == Port.Range.LIST) {
                outType = outType + " list";
            }
            sb.append("<div style='margin-top: 8px; border-top: 1px solid ").append(dividerColor)
              .append("; padding-top: 4px; font-size: 10px;'>");
            sb.append("<span style='font-weight: bold; color: ").append(subColor).append(";'>OUTPUT: </span>");
            sb.append("<span style='color: ").append(titleColor).append("; font-weight: bold;'>").append(outType).append("</span>");
            sb.append("</div>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }
}
