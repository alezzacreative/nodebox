package nodebox.graphics;

import java.util.ArrayList;
import java.util.List;

public class ColorHarmony {

    public static List<Color> getHarmony(Color base, String type) {
        List<Color> result = new ArrayList<Color>();
        if (base == null) return result;

        double h = base.getHue();
        double s = base.getSaturation();
        double v = base.getBrightness();
        double a = base.getAlpha();

        String t = type != null ? type.toLowerCase().trim() : "complementary";

        if (t.equals("complementary")) {
            result.add(base);
            result.add(Color.fromHSB((h + 0.5) % 1.0, s, v, a));
            // Also provide light and dark variations for design utility
            result.add(Color.fromHSB(h, Math.max(0, s * 0.5), Math.min(1.0, v * 1.2), a));
            result.add(Color.fromHSB((h + 0.5) % 1.0, Math.max(0, s * 0.5), Math.min(1.0, v * 1.2), a));
            result.add(Color.fromHSB(h, s, Math.max(0, v * 0.6), a));
        } else if (t.equals("triadic")) {
            result.add(base);
            result.add(Color.fromHSB((h + 1.0 / 3.0) % 1.0, s, v, a));
            result.add(Color.fromHSB((h + 2.0 / 3.0) % 1.0, s, v, a));
        } else if (t.equals("analogous")) {
            result.add(Color.fromHSB((h - 30.0 / 360.0 + 1.0) % 1.0, s, v, a));
            result.add(Color.fromHSB((h - 15.0 / 360.0 + 1.0) % 1.0, s, v, a));
            result.add(base);
            result.add(Color.fromHSB((h + 15.0 / 360.0) % 1.0, s, v, a));
            result.add(Color.fromHSB((h + 30.0 / 360.0) % 1.0, s, v, a));
        } else if (t.equals("split_complementary") || t.equals("split-complementary")) {
            result.add(base);
            result.add(Color.fromHSB((h + 150.0 / 360.0) % 1.0, s, v, a));
            result.add(Color.fromHSB((h + 210.0 / 360.0) % 1.0, s, v, a));
        } else if (t.equals("tetradic") || t.equals("square")) {
            result.add(base);
            result.add(Color.fromHSB((h + 0.25) % 1.0, s, v, a));
            result.add(Color.fromHSB((h + 0.50) % 1.0, s, v, a));
            result.add(Color.fromHSB((h + 0.75) % 1.0, s, v, a));
        } else if (t.equals("monochromatic")) {
            result.add(Color.fromHSB(h, Math.max(0, s * 0.25), Math.min(1.0, v * 1.3), a));
            result.add(Color.fromHSB(h, Math.max(0, s * 0.5), Math.min(1.0, v * 1.15), a));
            result.add(base);
            result.add(Color.fromHSB(h, Math.min(1.0, s * 1.1), Math.max(0, v * 0.75), a));
            result.add(Color.fromHSB(h, Math.min(1.0, s * 1.2), Math.max(0, v * 0.5), a));
        } else {
            // Default fallback
            result.add(base);
            result.add(Color.fromHSB((h + 0.5) % 1.0, s, v, a));
        }

        return result;
    }

    public static Color blend(Color base, Color blend, String mode, double opacity) {
        if (base == null && blend == null) return new Color();
        if (base == null) return blend;
        if (blend == null) return base;

        double op = Math.max(0.0, Math.min(1.0, opacity));
        double r1 = base.getRed(), g1 = base.getGreen(), b1 = base.getBlue();
        double r2 = blend.getRed(), g2 = blend.getGreen(), b2 = blend.getBlue();

        String m = mode != null ? mode.toLowerCase().trim().replace("-", "_") : "multiply";

        double r = blendChannel(r1, r2, m);
        double g = blendChannel(g1, g2, m);
        double b = blendChannel(b1, b2, m);

        // Mix with opacity
        double rout = r1 + op * (r - r1);
        double gout = g1 + op * (g - g1);
        double bout = b1 + op * (b - b1);
        double aout = base.getAlpha() + op * (blend.getAlpha() - base.getAlpha());

        return new Color(clamp(rout), clamp(gout), clamp(bout), clamp(aout));
    }

    private static double blendChannel(double b, double s, String mode) {
        if (mode.equals("multiply")) {
            return b * s;
        } else if (mode.equals("screen")) {
            return 1.0 - (1.0 - b) * (1.0 - s);
        } else if (mode.equals("overlay")) {
            return b < 0.5 ? 2.0 * b * s : 1.0 - 2.0 * (1.0 - b) * (1.0 - s);
        } else if (mode.equals("darken")) {
            return Math.min(b, s);
        } else if (mode.equals("lighten")) {
            return Math.max(b, s);
        } else if (mode.equals("color_dodge") || mode.equals("dodge")) {
            return s >= 1.0 ? 1.0 : Math.min(1.0, b / (1.0 - s));
        } else if (mode.equals("color_burn") || mode.equals("burn")) {
            return s <= 0.0 ? 0.0 : 1.0 - Math.min(1.0, (1.0 - b) / s);
        } else if (mode.equals("hard_light")) {
            return s < 0.5 ? 2.0 * b * s : 1.0 - 2.0 * (1.0 - b) * (1.0 - s);
        } else if (mode.equals("soft_light")) {
            return s < 0.5 ? 2.0 * b * s + b * b * (1.0 - 2.0 * s)
                           : 2.0 * b * (1.0 - s) + Math.sqrt(b) * (2.0 * s - 1.0);
        } else if (mode.equals("difference")) {
            return Math.abs(b - s);
        } else if (mode.equals("exclusion")) {
            return b + s - 2.0 * b * s;
        } else {
            // Normal blend
            return s;
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
