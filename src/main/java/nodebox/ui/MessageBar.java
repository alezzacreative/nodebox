package nodebox.ui;

import javax.swing.*;
import java.awt.*;

public class MessageBar extends JLabel {

    public enum Type {INFO, WARNING}

    public MessageBar(String text) {
        this(text, Type.WARNING);
    }

    public MessageBar(String text, Type type) {
        super(text);
        setAlignmentX(JComponent.LEFT_ALIGNMENT);
        boolean dark = Theme.isDark();
        if (type == Type.WARNING) {
            if (dark) {
                setBorder(Borders.topBottom(1, new Color(180, 83, 9), 1, new Color(120, 53, 15)));
                setBackground(new Color(146, 64, 14));
                setForeground(new Color(254, 243, 199));
            } else {
                setBorder(Borders.topBottom(1, new Color(235, 164, 69), 1, new Color(187, 125, 37)));
                setBackground(new Color(226, 136, 10));
                setForeground(Color.WHITE);
            }
        } else {
            if (dark) {
                setBorder(Borders.topBottom(1, new Color(55, 55, 62), 1, new Color(32, 32, 36)));
                setBackground(new Color(40, 40, 46));
                setForeground(new Color(228, 228, 231));
            } else {
                setBorder(Borders.topBottom(1, new Color(194, 204, 193), 1, new Color(136, 136, 136)));
                setBackground(new Color(201, 201, 201));
                setForeground(new Color(52, 52, 52));
            }
        }
        setOpaque(true);
        setFont(Theme.SMALL_FONT);
        setPreferredSize(new Dimension(9999, 25));
        setMinimumSize(new Dimension(100, 25));
        setMaximumSize(new Dimension(9999, 25));
    }


}
