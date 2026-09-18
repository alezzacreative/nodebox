package nodebox.util;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.Color;
import nodebox.graphics.Geometry;
import nodebox.graphics.Path;
import nodebox.graphics.Point;
import nodebox.graphics.Transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Duplicator {

    public enum Mode {
        LINEAR,
        GRID,
        RADIAL,
        SPIRAL,
        ALONG_PATH;

        public static Mode fromString(String name) {
            if (name == null) return LINEAR;
            String lower = name.trim().toLowerCase().replace("-", "_").replace(" ", "_");
            switch (lower) {
                case "grid":
                case "matrix":
                    return GRID;
                case "radial":
                case "circle":
                case "circular":
                    return RADIAL;
                case "spiral":
                case "helix":
                case "phyllotaxis":
                    return SPIRAL;
                case "along_path":
                case "path":
                case "curve":
                    return ALONG_PATH;
                case "linear":
                case "line":
                default:
                    return LINEAR;
            }
        }
    }

    public static class CloneInstance {
        public final double x, y;
        public final double rotation; // in degrees
        public final double scale;
        public final double alpha; // 0.0 to 1.0

        public CloneInstance(double x, double y, double rotation, double scale, double alpha) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.scale = scale;
            this.alpha = alpha;
        }
    }

    private Duplicator() {
    }

    /**
     * Duplicate and transform geometry across procedural clone patterns.
     */
    public static List<Geometry> duplicate(Geometry shape,
                                          String modeName,
                                          long count,
                                          double spacing,
                                          double angle,
                                          long columns,
                                          long rows,
                                          double spacingX,
                                          double spacingY,
                                          double stagger,
                                          double radius,
                                          double startAngle,
                                          double endAngle,
                                          boolean orient,
                                          double growthRate,
                                          double angleStep,
                                          Geometry targetPath,
                                          double stepRotation,
                                          double stepScale,
                                          double stepOpacity,
                                          double jitterPos,
                                          double jitterRot,
                                          double jitterScale,
                                          long seed) {
        if (shape == null) return ImmutableList.of();

        Mode mode = Mode.fromString(modeName);
        List<CloneInstance> instances = computeInstances(mode, count, spacing, angle,
                columns, rows, spacingX, spacingY, stagger,
                radius, startAngle, endAngle, orient,
                growthRate, angleStep, targetPath,
                stepRotation, stepScale, stepOpacity,
                jitterPos, jitterRot, jitterScale, seed);

        ImmutableList.Builder<Geometry> result = ImmutableList.builder();

        for (CloneInstance inst : instances) {
            Transform t = new Transform();
            t.translate(inst.x, inst.y);
            if (Math.abs(inst.rotation) > 1e-4) {
                t.rotate(inst.rotation);
            }
            if (Math.abs(inst.scale - 1.0) > 1e-4) {
                t.scale(Math.max(0.001, inst.scale));
            }

            Geometry cloned = t.map(shape);

            // Apply alpha / opacity fade if < 1.0
            if (inst.alpha < 0.999) {
                applyOpacity(cloned, inst.alpha);
            }

            result.add(cloned);
        }

        return result.build();
    }

    private static void applyOpacity(Geometry g, double alpha) {
        double a = Math.max(0.0, Math.min(1.0, alpha));
        for (Path p : g.getPaths()) {
            Color fill = p.getFillColor();
            if (fill != null) {
                p.setFillColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fill.getAlpha() * a));
            }
            Color stroke = p.getStrokeColor();
            if (stroke != null) {
                p.setStrokeColor(new Color(stroke.getRed(), stroke.getGreen(), stroke.getBlue(), stroke.getAlpha() * a));
            }
        }
    }

    public static List<CloneInstance> computeInstances(Mode mode,
                                                       long count,
                                                       double spacing,
                                                       double angle,
                                                       long columns,
                                                       long rows,
                                                       double spacingX,
                                                       double spacingY,
                                                       double stagger,
                                                       double radius,
                                                       double startAngle,
                                                       double endAngle,
                                                       boolean orient,
                                                       double growthRate,
                                                       double angleStep,
                                                       Geometry targetPath,
                                                       double stepRotation,
                                                       double stepScale,
                                                       double stepOpacity,
                                                       double jitterPos,
                                                       double jitterRot,
                                                       double jitterScale,
                                                       long seed) {
        List<CloneInstance> instances = new ArrayList<CloneInstance>();

        int totalCount = Math.max(1, (int) Math.min(count, 5000));

        switch (mode) {
            case GRID: {
                int cols = Math.max(1, (int) columns);
                int rws = Math.max(1, (int) rows);
                totalCount = cols * rws;
                int idx = 0;
                for (int r = 0; r < rws; r++) {
                    double rowY = (r - (rws - 1) / 2.0) * spacingY;
                    double rowOffset = (r % 2 != 0) ? stagger * spacingX : 0.0;
                    for (int c = 0; c < cols; c++) {
                        double colX = (c - (cols - 1) / 2.0) * spacingX + rowOffset;
                        instances.add(buildInstance(colX, rowY, 0.0, idx, totalCount,
                                stepRotation, stepScale, stepOpacity, jitterPos, jitterRot, jitterScale, seed));
                        idx++;
                    }
                }
                return instances;
            }

            case RADIAL: {
                double span = endAngle - startAngle;
                boolean fullCircle = Math.abs(Math.abs(span) - 360.0) < 1e-3;
                double step = totalCount > 1
                        ? (fullCircle ? span / totalCount : span / (totalCount - 1))
                        : 0.0;

                for (int i = 0; i < totalCount; i++) {
                    double a = startAngle + i * step;
                    double rad = Math.toRadians(a);
                    double x = radius * Math.cos(rad);
                    double y = radius * Math.sin(rad);
                    double rot = orient ? a : 0.0;
                    instances.add(buildInstance(x, y, rot, i, totalCount,
                            stepRotation, stepScale, stepOpacity, jitterPos, jitterRot, jitterScale, seed));
                }
                return instances;
            }

            case SPIRAL: {
                boolean isGolden = Math.abs(angleStep - 137.5) < 0.5;
                for (int i = 0; i < totalCount; i++) {
                    double a = i * angleStep;
                    double rad = Math.toRadians(a);
                    double r = isGolden
                            ? Math.sqrt(i) * growthRate * 12.0
                            : i * growthRate;
                    double x = r * Math.cos(rad);
                    double y = r * Math.sin(rad);
                    double rot = orient ? a : 0.0;
                    instances.add(buildInstance(x, y, rot, i, totalCount,
                            stepRotation, stepScale, stepOpacity, jitterPos, jitterRot, jitterScale, seed));
                }
                return instances;
            }

            case ALONG_PATH: {
                if (targetPath != null && targetPath.getLength() > 0) {
                    for (int i = 0; i < totalCount; i++) {
                        double t = totalCount > 1 ? (double) i / (totalCount - 1) : 0.0;
                        Point pt = targetPath.pointAt(t);
                        double rot = 0.0;
                        if (orient) {
                            double dt = 0.002;
                            double tPrev = Math.max(0.0, t - dt);
                            double tNext = Math.min(1.0, t + dt);
                            Point p1 = targetPath.pointAt(tPrev);
                            Point p2 = targetPath.pointAt(tNext);
                            rot = Math.toDegrees(Math.atan2(p2.y - p1.y, p2.x - p1.x));
                        }
                        instances.add(buildInstance(pt.x, pt.y, rot, i, totalCount,
                                stepRotation, stepScale, stepOpacity, jitterPos, jitterRot, jitterScale, seed));
                    }
                    return instances;
                }
                // Fallback to linear if no path provided
            }

            case LINEAR:
            default: {
                double rad = Math.toRadians(angle);
                double dirX = Math.cos(rad);
                double dirY = Math.sin(rad);

                for (int i = 0; i < totalCount; i++) {
                    double d = (i - (totalCount - 1) / 2.0) * spacing;
                    double x = d * dirX;
                    double y = d * dirY;
                    double rot = orient ? angle : 0.0;
                    instances.add(buildInstance(x, y, rot, i, totalCount,
                            stepRotation, stepScale, stepOpacity, jitterPos, jitterRot, jitterScale, seed));
                }
                return instances;
            }
        }
    }

    private static CloneInstance buildInstance(double baseX, double baseY, double baseRot,
                                               int index, int totalCount,
                                               double stepRotation, double stepScale, double stepOpacity,
                                               double jitterPos, double jitterRot, double jitterScale,
                                               long seed) {
        // Step transformations
        double rot = baseRot + (index * stepRotation);
        double sc = Math.max(0.001, 1.0 + (index * stepScale / 100.0));
        double alpha = Math.max(0.0, Math.min(1.0, 1.0 - (index * (1.0 - Math.max(0.0, Math.min(1.0, stepOpacity))) / Math.max(1, totalCount - 1))));

        // Deterministic pseudo-random jitter
        double jx = 0;
        double jy = 0;
        double jr = 0;
        double js = 0;

        if (jitterPos > 0 || jitterRot > 0 || jitterScale > 0) {
            Random rng = new Random(seed + (long) index * 104729L);
            if (jitterPos > 0) {
                jx = (rng.nextDouble() * 2.0 - 1.0) * jitterPos;
                jy = (rng.nextDouble() * 2.0 - 1.0) * jitterPos;
            }
            if (jitterRot > 0) {
                jr = (rng.nextDouble() * 2.0 - 1.0) * jitterRot;
            }
            if (jitterScale > 0) {
                js = (rng.nextDouble() * 2.0 - 1.0) * (jitterScale / 100.0);
            }
        }

        return new CloneInstance(baseX + jx, baseY + jy, rot + jr, Math.max(0.001, sc + js), alpha);
    }
}
