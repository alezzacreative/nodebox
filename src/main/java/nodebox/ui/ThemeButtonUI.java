package nodebox.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/**
 * Clean, modern button UI that adapts to the active Theme (Dark / Light)
 * and highlights default primary action buttons.
 */
public class ThemeButtonUI extends BasicButtonUI {

    private static final ThemeButtonUI INSTANCE = new ThemeButtonUI();

    public static ComponentUI createUI(JComponent c) {
        return INSTANCE;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (d != null) {
            d.height = Math.max(d.height, 24);
            d.width = Math.max(d.width, 50);
        }
        return d;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        ButtonModel model = b.getModel();
        int w = b.getWidth();
        int h = b.getHeight();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        boolean dark = Theme.isDark();
        JRootPane root = SwingUtilities.getRootPane(b);
        boolean isDefault = (root != null && root.getDefaultButton() == b);

        Color bg;
        Color border;
        Color fg;

        if (!b.isEnabled()) {
            if (dark) {
                bg = new Color(34, 34, 38);
                border = new Color(48, 48, 54);
                fg = new Color(113, 113, 122);
            } else {
                bg = new Color(240, 240, 242);
                border = new Color(215, 215, 220);
                fg = new Color(160, 160, 168);
            }
        } else if (model.isPressed() || model.isArmed()) {
            if (isDefault) {
                bg = new Color(29, 78, 216);
                border = new Color(37, 99, 235);
                fg = Color.WHITE;
            } else {
                if (dark) {
                    bg = new Color(30, 30, 34);
                    border = new Color(50, 50, 56);
                    fg = new Color(228, 228, 231);
                } else {
                    bg = new Color(210, 210, 216);
                    border = new Color(175, 175, 182);
                    fg = new Color(24, 24, 27);
                }
            }
        } else if (model.isRollover()) {
            if (isDefault) {
                bg = new Color(59, 130, 246);
                border = new Color(96, 165, 250);
                fg = Color.WHITE;
            } else {
                if (dark) {
                    bg = new Color(54, 54, 62);
                    border = new Color(75, 75, 86);
                    fg = new Color(250, 250, 250);
                } else {
                    bg = new Color(228, 228, 234);
                    border = new Color(185, 185, 195);
                    fg = new Color(18, 18, 20);
                }
            }
        } else {
            if (isDefault) {
                bg = new Color(37, 99, 235);
                border = new Color(59, 130, 246);
                fg = Color.WHITE;
            } else {
                if (dark) {
                    bg = new Color(45, 45, 52);
                    border = new Color(65, 65, 74);
                    fg = new Color(228, 228, 231);
                } else {
                    bg = new Color(245, 245, 248);
                    border = new Color(200, 200, 206);
                    fg = new Color(39, 39, 42);
                }
            }
        }

        int arc = 5;
        // Background
        g2.setColor(bg);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        // Border
        g2.setColor(border);
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        // Focus ring if focused
        if (b.hasFocus() && b.isEnabled()) {
            g2.setColor(dark ? new Color(96, 165, 250, 150) : new Color(59, 130, 246, 150));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc + 1, arc + 1);
        }

        // Layout Icon & Text
        Font font = b.getFont();
        if (font == null) {
            font = Theme.SMALL_BOLD_FONT;
        }
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();
        Insets insets = b.getInsets();
        Rectangle viewRect = new Rectangle(insets.left, insets.top, w - insets.left - insets.right, h - insets.top - insets.bottom);
        Rectangle iconRect = new Rectangle();
        Rectangle textRect = new Rectangle();

        String text = SwingUtilities.layoutCompoundLabel(
                b, fm, b.getText(), b.getIcon(),
                b.getVerticalAlignment(), b.getHorizontalAlignment(),
                b.getVerticalTextPosition(), b.getHorizontalTextPosition(),
                viewRect, iconRect, textRect,
                b.getText() == null ? 0 : b.getIconTextGap());

        // Paint icon
        if (b.getIcon() != null) {
            paintIcon(g2, c, iconRect);
        }

        // Paint text
        if (text != null && !text.isEmpty()) {
            g2.setColor(fg);
            int textY = textRect.y + fm.getAscent();
            g2.drawString(text, textRect.x, textY);
        }

        g2.dispose();
    }
}
