package nodebox.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;

/**
 * Clean, modern combo box UI that adapts to the active Theme (Dark / Light).
 */
public class ThemeComboBoxUI extends BasicComboBoxUI {

    public static ComponentUI createUI(JComponent c) {
        return new ThemeComboBoxUI();
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        comboBox.setOpaque(false);
        comboBox.setFont(Theme.SMALL_BOLD_FONT);
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton() {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean dark = Theme.isDark();
                Color arrowColor;
                if (!comboBox.isEnabled()) {
                    arrowColor = dark ? new Color(100, 100, 105) : new Color(170, 170, 175);
                } else if (getModel().isPressed() || getModel().isArmed()) {
                    arrowColor = dark ? Color.WHITE : Color.BLACK;
                } else if (getModel().isRollover()) {
                    arrowColor = dark ? new Color(240, 240, 245) : new Color(40, 40, 45);
                } else {
                    arrowColor = dark ? new Color(161, 161, 170) : new Color(90, 90, 95);
                }

                int cx = getWidth() / 2 - 1;
                int cy = getHeight() / 2;

                g2.setColor(arrowColor);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 3, cy - 1, cx, cy + 2);
                g2.drawLine(cx, cy + 2, cx + 3, cy - 1);
                g2.dispose();
            }
        };
        button.setOpaque(false);
        button.setBorder(null);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        return button;
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = new BasicComboPopup(comboBox) {
            @Override
            protected void configurePopup() {
                super.configurePopup();
                boolean dark = Theme.isDark();
                setBackground(dark ? new Color(34, 34, 38) : Color.WHITE);
                setBorder(BorderFactory.createLineBorder(dark ? new Color(60, 60, 66) : new Color(190, 190, 196)));
            }

            @Override
            protected void configureList() {
                super.configureList();
                boolean dark = Theme.isDark();
                list.setBackground(dark ? new Color(34, 34, 38) : Color.WHITE);
                list.setForeground(dark ? new Color(228, 228, 231) : new Color(30, 30, 32));
                list.setSelectionBackground(dark ? new Color(63, 63, 70) : new Color(220, 220, 226));
                list.setSelectionForeground(dark ? Color.WHITE : Color.BLACK);
            }
        };
        return popup;
    }

    @Override
    protected ListCellRenderer<Object> createRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                boolean dark = Theme.isDark();
                if (isSelected) {
                    setBackground(dark ? new Color(63, 63, 70) : new Color(220, 220, 226));
                    setForeground(dark ? Color.WHITE : Color.BLACK);
                } else {
                    setBackground(dark ? new Color(34, 34, 38) : Color.WHITE);
                    setForeground(dark ? new Color(228, 228, 231) : new Color(30, 30, 32));
                }
                return this;
            }
        };
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean dark = Theme.isDark();
        int w = c.getWidth();
        int h = c.getHeight();

        Color bg;
        Color border;

        if (!c.isEnabled()) {
            bg = dark ? new Color(28, 28, 32) : new Color(240, 240, 242);
            border = dark ? new Color(48, 48, 54) : new Color(215, 215, 220);
        } else {
            bg = dark ? new Color(26, 26, 30) : Color.WHITE;
            border = dark ? new Color(60, 60, 66) : new Color(195, 195, 202);
        }

        int arc = 5;
        g2.setColor(bg);
        g2.fillRoundRect(1, 1, w - 2, h - 2, arc, arc);

        g2.setColor(border);
        g2.drawRoundRect(1, 1, w - 3, h - 3, arc, arc);

        if (c.hasFocus() && c.isEnabled()) {
            g2.setColor(dark ? new Color(96, 165, 250, 140) : new Color(59, 130, 246, 140));
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc + 1, arc + 1);
        }

        g2.dispose();

        super.paint(g, c);
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        // Handled in main paint method
    }
}
