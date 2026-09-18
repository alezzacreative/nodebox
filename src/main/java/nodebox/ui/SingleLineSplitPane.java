package nodebox.ui;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

public class SingleLineSplitPane extends CustomSplitPane {

    public SingleLineSplitPane(int orientation, Component c1, Component c2) {
        super(orientation, c1, c2);
        setDividerSize(3);
        setBackground(Theme.SPLIT_PANE_BACKGROUND);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(Theme.SPLIT_PANE_BACKGROUND);
    }

    @Override
    protected BasicSplitPaneUI createUI() {
        return new SingleLineSplitPaneUI();
    }

    private class SingleLineSplitPaneUI extends BasicSplitPaneUI {
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(SingleLineSplitPaneUI.this) {
                @Override
                public void paint(Graphics g) {
                    int w = getWidth();
                    int h = getHeight();
                    g.setColor(Theme.SPLIT_PANE_BACKGROUND);
                    g.fillRect(0, 0, w, h);
                    g.setColor(Theme.DEFAULT_SPLIT_COLOR);
                    if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                        int cx = w / 2;
                        g.drawLine(cx, 0, cx, h);
                    } else {
                        int cy = h / 2;
                        g.drawLine(0, cy, w, cy);
                    }
                }
            };
        }
    }

}
