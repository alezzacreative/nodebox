package nodebox.client;

import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

/**
 * Better looking split divider that adapts to the active Theme.
 */
public class CustomSplitPane extends JSplitPane {

    public CustomSplitPane(int orientation, Component c1, Component c2) {
        super(orientation, c1, c2);
        setUI(createUI());
        setContinuousLayout(true);
        setBorder(null);
        setResizeWeight(0.5);
        setDividerLocation(0.5);
        setDividerSize(7);
        setBackground(Theme.SPLIT_PANE_BACKGROUND);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setBackground(Theme.SPLIT_PANE_BACKGROUND);
    }

    protected BasicSplitPaneUI createUI() {
        return new CustomSplitPaneUI();
    }

    private class CustomSplitPaneUI extends BasicSplitPaneUI {

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new BasicSplitPaneDivider(CustomSplitPaneUI.this) {
                @Override
                public void paint(Graphics g) {
                    int w = getWidth();
                    int h = getHeight();
                    g.setColor(Theme.SPLIT_PANE_BACKGROUND);
                    g.fillRect(0, 0, w, h);
                    g.setColor(Theme.SPLIT_PANE_BORDER);
                    if (getOrientation() == JSplitPane.VERTICAL_SPLIT) {
                        g.drawLine(0, 0, w, 0);
                        g.drawLine(0, h - 1, w, h - 1);
                    } else {
                        g.drawLine(0, 0, 0, h);
                        g.drawLine(w - 1, 0, w - 1, h);
                    }
                }
            };
        }
    }

}
