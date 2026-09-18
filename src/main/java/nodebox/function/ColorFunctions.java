package nodebox.function;

import com.google.common.collect.ImmutableList;
import nodebox.graphics.Color;

import java.util.List;
import java.util.Locale;

public final class ColorFunctions {

    public static final FunctionLibrary LIBRARY;

    static {
        LIBRARY = JavaLibrary.ofClass("color", ColorFunctions.class, "color", "gray", "rgb", "hsb", "palette", "gradient", "extractPalette", "blend", "conicGradient");
    }

    public static Color color(Color color) {
        return color;
    }

    public static Color gray(double gray, double alpha, double range) {
        range = Math.max(range, 1);
        return new Color(gray / range, gray / range, gray / range, alpha / range);
    }

    public static Color rgb(double red, double green, double blue, double alpha, double range) {
        range = Math.max(range, 1);
        return new Color(red / range, green / range, blue / range, alpha / range);
    }

    public static Color hsb(double hue, double saturation, double brightness, double alpha, double range) {
        range = Math.max(range, 1);
        return new Color(hue / range, saturation / range, brightness / range, alpha / range, Color.Mode.HSB);
    }

    public static List<Color> palette(Color baseColor, String type, long count, double spread) {
        if (baseColor == null) baseColor = new Color(0.2, 0.6, 0.9, 1.0);
        int n = (int) Math.max(1, Math.min(count, 100));
        if (spread <= 0.0) spread = 0.08;
        if (spread > 1.0) spread = spread / 360.0;

        double h = baseColor.getHue();
        double s = baseColor.getSaturation();
        double b = baseColor.getBrightness();
        double a = baseColor.getAlpha();

        ImmutableList.Builder<Color> list = ImmutableList.builder();
        String t = type != null ? type.toLowerCase(Locale.US) : "analogous";

        if ("monochromatic".equals(t)) {
            for (int i = 0; i < n; i++) {
                double factor = n > 1 ? (double) i / (n - 1) : 0.5;
                double newS = Math.max(0.05, Math.min(1.0, s * (0.4 + 0.6 * factor)));
                double newB = Math.max(0.1, Math.min(1.0, 0.25 + 0.75 * factor));
                list.add(Color.fromHSB(h, newS, newB, a));
            }
        } else if ("complementary".equals(t)) {
            for (int i = 0; i < n; i++) {
                double anchorH = (i % 2 == 0) ? h : (h + 0.5);
                double offset = n > 2 ? ((i / 2) - ((n / 2) - 1) / 2.0) * spread * 0.5 : 0;
                double curH = wrap01(anchorH + offset);
                list.add(Color.fromHSB(curH, s, b, a));
            }
        } else if ("triadic".equals(t)) {
            for (int i = 0; i < n; i++) {
                double anchorH = h + (i % 3) * (1.0 / 3.0);
                double offset = n > 3 ? ((i / 3) - ((n / 3) - 1) / 2.0) * spread * 0.5 : 0;
                double curH = wrap01(anchorH + offset);
                list.add(Color.fromHSB(curH, s, b, a));
            }
        } else if ("tetradic".equals(t)) {
            for (int i = 0; i < n; i++) {
                double anchorH = h + (i % 4) * 0.25;
                double offset = n > 4 ? ((i / 4) - ((n / 4) - 1) / 2.0) * spread * 0.5 : 0;
                double curH = wrap01(anchorH + offset);
                list.add(Color.fromHSB(curH, s, b, a));
            }
        } else if ("split-complementary".equals(t)) {
            double[] anchors = new double[]{h, wrap01(h + 0.5 - spread), wrap01(h + 0.5 + spread)};
            for (int i = 0; i < n; i++) {
                double anchorH = anchors[i % 3];
                double offset = n > 3 ? ((i / 3) - ((n / 3) - 1) / 2.0) * (spread * 0.3) : 0;
                double curH = wrap01(anchorH + offset);
                list.add(Color.fromHSB(curH, s, b, a));
            }
        } else { // "analogous" default
            for (int i = 0; i < n; i++) {
                double offset = n > 1 ? (i - (n - 1) / 2.0) * spread : 0;
                double curH = wrap01(h + offset);
                list.add(Color.fromHSB(curH, s, b, a));
            }
        }
        return list.build();
    }

    private static double wrap01(double v) {
        v = v % 1.0;
        if (v < 0) v += 1.0;
        return v;
    }

    public static Color gradient(List<Color> colors, double position, String colorSpace) {
        if (colors == null || colors.isEmpty()) {
            return Color.BLACK;
        }
        if (colors.size() == 1) {
            return colors.get(0);
        }

        double pos = position;
        if (pos > 1.0 && pos <= 100.0) {
            pos = pos / 100.0;
        }
        pos = Math.max(0.0, Math.min(1.0, pos));

        int n = colors.size();
        double tGlobal = pos * (n - 1);
        int idx = (int) Math.floor(tGlobal);
        double t = tGlobal - idx;
        if (idx >= n - 1) {
            idx = n - 2;
            t = 1.0;
        }

        Color c1 = colors.get(idx);
        Color c2 = colors.get(idx + 1);
        if (c1 == null) c1 = Color.BLACK;
        if (c2 == null) c2 = Color.BLACK;

        String cs = colorSpace != null ? colorSpace.toLowerCase(Locale.US) : "oklab";
        if ("rgb".equals(cs)) {
            double r = c1.getRed() * (1 - t) + c2.getRed() * t;
            double g = c1.getGreen() * (1 - t) + c2.getGreen() * t;
            double b = c1.getBlue() * (1 - t) + c2.getBlue() * t;
            double a = c1.getAlpha() * (1 - t) + c2.getAlpha() * t;
            return new Color(r, g, b, a);
        } else if ("hsb".equals(cs)) {
            double h1 = c1.getHue();
            double h2 = c2.getHue();
            double diff = h2 - h1;
            if (diff > 0.5) diff -= 1.0;
            else if (diff < -0.5) diff += 1.0;
            double h = wrap01(h1 + diff * t);
            double s = c1.getSaturation() * (1 - t) + c2.getSaturation() * t;
            double b = c1.getBrightness() * (1 - t) + c2.getBrightness() * t;
            double a = c1.getAlpha() * (1 - t) + c2.getAlpha() * t;
            return Color.fromHSB(h, s, b, a);
        } else { // "oklab" default
            double[] lab1 = rgbToOklab(c1.getRed(), c1.getGreen(), c1.getBlue());
            double[] lab2 = rgbToOklab(c2.getRed(), c2.getGreen(), c2.getBlue());

            double L = lab1[0] * (1 - t) + lab2[0] * t;
            double a = lab1[1] * (1 - t) + lab2[1] * t;
            double b = lab1[2] * (1 - t) + lab2[2] * t;
            double alpha = c1.getAlpha() * (1 - t) + c2.getAlpha() * t;

            double[] rgb = oklabToRgb(L, a, b);
            return new Color(rgb[0], rgb[1], rgb[2], alpha);
        }
    }

    private static double srgbToLinear(double c) {
        return c >= 0.04045 ? Math.pow((c + 0.055) / 1.055, 2.4) : c / 12.92;
    }

    private static double linearToSrgb(double c) {
        c = Math.max(0.0, Math.min(1.0, c));
        return c >= 0.0031308 ? 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055 : 12.92 * c;
    }

    private static double[] rgbToOklab(double r, double g, double b) {
        double lr = srgbToLinear(r);
        double lg = srgbToLinear(g);
        double lb = srgbToLinear(b);

        double l = 0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb;
        double m = 0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb;
        double s = 0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb;

        double l_ = Math.cbrt(l);
        double m_ = Math.cbrt(m);
        double s_ = Math.cbrt(s);

        double L = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_;
        double a = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_;
        double oklabB = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_;
        return new double[]{L, a, oklabB};
    }

    private static double[] oklabToRgb(double L, double a, double b) {
        double l_ = L + 0.3963377774 * a + 0.2158037573 * b;
        double m_ = L - 0.1055613458 * a - 0.0638541728 * b;
        double s_ = L - 0.0894841775 * a - 1.2914855480 * b;

        double l = l_ * l_ * l_;
        double m = m_ * m_ * m_;
        double s = s_ * s_ * s_;

        double lr = +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s;
        double lg = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s;
        double lb = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s;

        return new double[]{linearToSrgb(lr), linearToSrgb(lg), linearToSrgb(lb)};
    }

    public static List<Color> extractPalette(String imageFile, long count, long sampleStep) {
        int k = (int) Math.max(1, Math.min(count, 50));
        int step = (int) Math.max(1, sampleStep);
        return nodebox.graphics.ColorExtractor.extractPalette(imageFile, k, step);
    }

    public static Color blend(Color baseColor, Color blendColor, String mode, double opacity) {
        return nodebox.graphics.ColorHarmony.blend(baseColor, blendColor, mode, opacity);
    }

    public static List<Color> conicGradient(List<?> colors, long steps, double startAngle, double endAngle, String space) {
        int n = (int) Math.max(2, Math.min(steps, 2000));
        java.util.ArrayList<Color> colorList = new java.util.ArrayList<Color>();
        if (colors != null) {
            for (Object o : colors) {
                if (o instanceof Color) colorList.add((Color) o);
            }
        }
        if (colorList.isEmpty()) {
            colorList.add(Color.BLACK);
            colorList.add(Color.WHITE);
        }
        java.util.ArrayList<Color> result = new java.util.ArrayList<Color>(n);
        for (int i = 0; i < n; i++) {
            double frac = (double) i / (n - 1);
            result.add(gradient(colorList, frac, space));
        }
        return result;
    }

}
