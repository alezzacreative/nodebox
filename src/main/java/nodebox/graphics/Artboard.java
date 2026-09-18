package nodebox.graphics;

import java.awt.geom.Rectangle2D;

/**
 * An Artboard represents a designated design canvas / export frame
 * with specific pixel dimensions, position, background, clipping boundaries,
 * and export metadata (e.g. for Instagram Stories, Posts, Print layouts).
 */
public class Artboard extends Geometry {

    private String name;
    private String preset;
    private double width;
    private double height;
    private Point position;
    private boolean clip;
    private Color background;
    private boolean showFrame;
    private Geometry content;

    public Artboard(String name, String preset, double width, double height, Point position, boolean clip, Color background, boolean showFrame, IGeometry shape) {
        this.name = (name != null && !name.trim().isEmpty()) ? name.trim() : "Artboard";
        this.preset = preset != null ? preset.toLowerCase().trim() : "custom";
        this.position = position != null ? position : Point.ZERO;
        this.clip = clip;
        this.background = background;
        this.showFrame = showFrame;

        // Resolve dimensions according to standard presets
        double[] dims = resolvePresetDimensions(this.preset, width, height);
        this.width = dims[0];
        this.height = dims[1];

        this.content = new Geometry();
        buildArtboard(shape);
    }

    public static double[] resolvePresetDimensions(String preset, double customW, double customH) {
        if ("instagram_story".equals(preset)) {
            return new double[]{1080.0, 1920.0};
        } else if ("instagram_post".equals(preset)) {
            return new double[]{1080.0, 1080.0};
        } else if ("instagram_landscape".equals(preset)) {
            return new double[]{1080.0, 566.0};
        } else if ("twitter_header".equals(preset)) {
            return new double[]{1500.0, 500.0};
        } else if ("twitter_post".equals(preset)) {
            return new double[]{1200.0, 675.0};
        } else if ("youtube_thumbnail".equals(preset)) {
            return new double[]{1280.0, 720.0};
        } else if ("a4".equals(preset)) {
            return new double[]{2480.0, 3508.0};
        } else if ("us_letter".equals(preset)) {
            return new double[]{2550.0, 3300.0};
        } else if ("business_card".equals(preset)) {
            return new double[]{1050.0, 600.0};
        }
        return new double[]{Math.max(1.0, customW), Math.max(1.0, customH)};
    }

    private void buildArtboard(IGeometry shape) {
        double px = position.x;
        double py = position.y;
        double w = this.width;
        double h = this.height;

        // 1. Background fill
        if (background != null && background.getAlpha() > 0.0) {
            Path bg = new Path();
            bg.rect(px, py, w, h);
            bg.setFillColor(background);
            bg.setStrokeColor(null);
            content.add(bg);
            this.add(bg);
        }

        // 2. Shape content (with optional clipping)
        if (shape != null) {
            if (clip) {
                Path clipRect = new Path();
                clipRect.rect(px, py, w, h);
                IGeometry clipped = VectorBooleans.combine(shape, clipRect, "intersect");
                for (Path p : clipped.getPaths()) {
                    content.add(p);
                    this.add(p);
                }
            } else {
                for (Path p : shape.getPaths()) {
                    content.add(p);
                    this.add(p);
                }
            }
        }

        // 3. Optional on-screen artboard frame & label (only for canvas display, excluded from export)
        if (showFrame) {
            // Artboard boundary outline
            Path frame = new Path();
            frame.rect(px, py, w, h);
            frame.setFillColor(null);
            frame.setStrokeColor(new Color(0.2f, 0.6f, 1.0f, 0.75f));
            frame.setStrokeWidth(1.5f);
            this.add(frame);

            // Text label above top-left corner
            try {
                String labelText = name + "  [" + (int) w + " x " + (int) h + "]";
                Point labelPos = new Point(px - w / 2.0, py - h / 2.0 - 12.0);
                Path labelPath = nodebox.function.CoreVectorFunctions.textpath(labelText, "SansSerif", 13.0, "left", labelPos, 0.0);
                if (labelPath != null) {
                    labelPath.setFillColor(new Color(0.25f, 0.6f, 0.95f));
                    labelPath.setStrokeColor(null);
                    this.add(labelPath);
                }
            } catch (Exception e) {
                // Ignore font raster failure
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getPreset() {
        return preset;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Point getPosition() {
        return position;
    }

    public boolean isClipped() {
        return clip;
    }

    public Color getBackground() {
        return background;
    }

    public Rectangle2D getExportBounds() {
        return new Rectangle2D.Double(position.x - width / 2.0, position.y - height / 2.0, width, height);
    }

    public Geometry getContentForExport() {
        return content;
    }
}
