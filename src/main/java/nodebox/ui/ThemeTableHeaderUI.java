package nodebox.ui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class ThemeTableHeaderUI extends BasicTableHeaderUI {

    public static ComponentUI createUI(JComponent c) {
        return new ThemeTableHeaderUI();
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        if (header != null) {
            header.setDefaultRenderer(new ThemeTableHeaderRenderer());
        }
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g;
        boolean dark = Theme.isDark();
        Color bg = dark ? new Color(36, 36, 42) : new Color(236, 236, 240);
        Color border = dark ? new Color(48, 48, 56) : new Color(210, 210, 215);

        g2.setColor(bg);
        g2.fillRect(0, 0, c.getWidth(), c.getHeight());

        g2.setColor(border);
        g2.drawLine(0, c.getHeight() - 1, c.getWidth(), c.getHeight() - 1);

        super.paint(g, c);
    }

    public static class ThemeTableHeaderRenderer extends JLabel implements TableCellRenderer {

        public ThemeTableHeaderRenderer() {
            setOpaque(true);
            setFont(Theme.SMALL_BOLD_FONT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value == null ? "" : value.toString());
            boolean dark = Theme.isDark();
            if (dark) {
                setBackground(new Color(36, 36, 42));
                setForeground(new Color(228, 228, 231));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(48, 48, 56)),
                        BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            } else {
                setBackground(new Color(236, 236, 240));
                setForeground(new Color(30, 30, 32));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(210, 210, 215)),
                        BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            }
            return this;
        }
    }
}