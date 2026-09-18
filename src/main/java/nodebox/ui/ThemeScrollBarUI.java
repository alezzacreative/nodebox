package nodebox.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Clean, modern minimalist scrollbar UI that adapts to the active Theme (Dark / Light).
 */
public class ThemeScrollBarUI extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new ThemeScrollBarUI();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(0, 0));
        b.setMinimumSize(new Dimension(0, 0));
        b.setMaximumSize(new Dimension(0, 0));
        b.setOpaque(false);
        b.setFocusable(false);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        boolean dark = Theme.isDark();
        g.setColor(dark ? new Color(24, 24, 27) : new Color(240, 240, 242));
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean dark = Theme.isDark();
        Color thumbColor;
        if (isDragging) {
            thumbColor = dark ? new Color(113, 113, 122) : new Color(140, 140, 148);
        } else if (isThumbRollover()) {
            thumbColor = dark ? new Color(82, 82, 91) : new Color(165, 165, 175);
        } else {
            thumbColor = dark ? new Color(63, 63, 70) : new Color(195, 195, 202);
        }

        int x = thumbBounds.x + 2;
        int y = thumbBounds.y + 2;
        int w = Math.max(thumbBounds.width - 4, 4);
        int h = Math.max(thumbBounds.height - 4, 4);
        int arc = Math.min(w, h);

        g2.setColor(thumbColor);
        g2.fillRoundRect(x, y, w, h, arc, arc);
        g2.dispose();
    }
}
