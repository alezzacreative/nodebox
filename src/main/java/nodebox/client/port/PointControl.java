package nodebox.client.port;

import nodebox.graphics.Point;
import nodebox.node.Port;
import nodebox.ui.DraggableNumber;
import nodebox.ui.Theme;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.google.common.base.Preconditions.checkArgument;

public class PointControl extends AbstractPortControl implements ChangeListener, ActionListener {

    private final DraggableNumber xNumber;
    private final DraggableNumber yNumber;
    private JCheckBox lockRatioCheck;
    private boolean isUpdating = false;
    private double currentRatio = 1.0;

    public PointControl(String nodePath, Port port) {
        super(nodePath, port);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));
        xNumber = new DraggableNumber();
        xNumber.addChangeListener(this);
        yNumber = new DraggableNumber();
        yNumber.addChangeListener(this);
        add(xNumber);
        add(Box.createHorizontalStrut(5));
        add(yNumber);

        String pName = port.getName().toLowerCase();
        if (pName.contains("roundness") || pName.contains("size") || pName.contains("scale") || pName.contains("radius")) {
            lockRatioCheck = new JCheckBox("Lock");
            lockRatioCheck.setFont(Theme.SMALL_FONT);
            lockRatioCheck.setForeground(Theme.TEXT_NORMAL_COLOR);
            lockRatioCheck.setToolTipText("Lock aspect ratio");
            lockRatioCheck.setFocusable(false);
            lockRatioCheck.setOpaque(false);
            lockRatioCheck.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (lockRatioCheck.isSelected()) {
                        double x = xNumber.getValue();
                        double y = yNumber.getValue();
                        currentRatio = (x != 0.0) ? (y / x) : 1.0;
                    }
                }
            });
            add(Box.createHorizontalStrut(6));
            add(lockRatioCheck);
        }

        setValueForControl(port.getValue());
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        xNumber.setEnabled(enabled);
        yNumber.setEnabled(enabled);
        if (lockRatioCheck != null) {
            lockRatioCheck.setEnabled(enabled);
        }
    }

    public void setValueForControl(Object v) {
        checkArgument(v instanceof Point);
        nodebox.graphics.Point pt = (Point) v;
        isUpdating = true;
        try {
            xNumber.setValue(pt.getX());
            yNumber.setValue(pt.getY());
            if (pt.getX() != 0.0) {
                currentRatio = pt.getY() / pt.getX();
            }
        } finally {
            isUpdating = false;
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (isUpdating) return;
        handleValueChange(e.getSource());
    }

    public void actionPerformed(ActionEvent e) {
        if (isUpdating) return;
        handleValueChange(e.getSource());
    }

    private void handleValueChange(Object source) {
        if (isUpdating) return;
        isUpdating = true;
        try {
            double x = xNumber.getValue();
            double y = yNumber.getValue();

            if (lockRatioCheck != null && lockRatioCheck.isSelected()) {
                if (source == xNumber) {
                    y = Math.round(x * currentRatio * 100.0) / 100.0;
                    yNumber.setValue(y);
                } else if (source == yNumber) {
                    x = (currentRatio != 0.0) ? (Math.round(y / currentRatio * 100.0) / 100.0) : y;
                    xNumber.setValue(x);
                }
            } else {
                if (x != 0.0) {
                    currentRatio = y / x;
                }
            }
            setPortValue(new Point(x, y));
        } finally {
            isUpdating = false;
        }
    }

}
