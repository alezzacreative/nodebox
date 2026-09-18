package nodebox.client;

import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencesDialog extends JDialog {

    private Preferences preferences;
    private JComboBox<String> themeComboBox;
    private JComboBox<String> cableStyleComboBox;
    private JCheckBox gpuAccelerationCheckBox;
    private JLabel gpuStatusLabel;

    public PreferencesDialog() {
        super((Frame) null, "Preferences");
        setLocationRelativeTo(null);
        JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new BoxLayout(rootPanel, BoxLayout.Y_AXIS));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel appearance = new JLabel("Appearance");
        appearance.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        appearance.setAlignmentX(Component.LEFT_ALIGNMENT);
        rootPanel.add(appearance);

        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        themePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel themeLabel = new JLabel("Theme: ");
        themeComboBox = new JComboBox<String>(new String[]{"Light", "Dark"});
        themePanel.add(themeLabel);
        themePanel.add(themeComboBox);
        rootPanel.add(themePanel);

        JPanel cablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        cablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cableLabel = new JLabel("Cable Style: ");
        cableStyleComboBox = new JComboBox<String>(new String[]{"Curved", "Straight", "Orthogonal"});
        cablePanel.add(cableLabel);
        cablePanel.add(cableStyleComboBox);
        rootPanel.add(cablePanel);

        rootPanel.add(Box.createVerticalStrut(10));

        JLabel rendering = new JLabel("Rendering & Performance");
        rendering.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        rendering.setAlignmentX(Component.LEFT_ALIGNMENT);
        rootPanel.add(rendering);

        JPanel gpuPanel = new JPanel();
        gpuPanel.setLayout(new BoxLayout(gpuPanel, BoxLayout.Y_AXIS));
        gpuPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gpuAccelerationCheckBox = new JCheckBox("Enable GPU Hardware Acceleration");
        gpuAccelerationCheckBox.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        gpuAccelerationCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        gpuAccelerationCheckBox.setToolTipText("Accelerates viewport rendering and canvas panning/zooming using GPU VRAM.");

        gpuStatusLabel = new JLabel("Pipeline: " + nodebox.util.GPUUtils.getPipelineDescription());
        gpuStatusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        gpuStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gpuStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 22, 0, 0));

        gpuPanel.add(gpuAccelerationCheckBox);
        gpuPanel.add(gpuStatusLabel);
        rootPanel.add(gpuPanel);

        rootPanel.add(Box.createVerticalStrut(10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doCancel();
            }
        });
        buttonPanel.add(cancelButton);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doSave();
            }
        });
        buttonPanel.add(saveButton);
        rootPanel.add(buttonPanel);
        getRootPane().setDefaultButton(saveButton);

        Color dialogBg = Theme.DIALOG_BACKGROUND;
        rootPanel.setBackground(dialogBg);
        themePanel.setBackground(dialogBg);
        cablePanel.setBackground(dialogBg);
        gpuPanel.setBackground(dialogBg);
        buttonPanel.setBackground(dialogBg);
        appearance.setForeground(Theme.TEXT_NORMAL_COLOR);
        rendering.setForeground(Theme.TEXT_NORMAL_COLOR);
        themeLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        cableLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        gpuAccelerationCheckBox.setBackground(dialogBg);
        gpuAccelerationCheckBox.setForeground(Theme.TEXT_NORMAL_COLOR);
        gpuStatusLabel.setForeground(Theme.TEXT_DISABLED_COLOR);
        themeComboBox.setUI(new nodebox.ui.ThemeComboBoxUI());
        cableStyleComboBox.setUI(new nodebox.ui.ThemeComboBoxUI());
        cancelButton.setUI(new nodebox.ui.ThemeButtonUI());
        saveButton.setUI(new nodebox.ui.ThemeButtonUI());

        readPreferences();

        setContentPane(rootPanel);
        setResizable(false);
        pack();
    }

    private void readPreferences() {
        this.preferences = Preferences.userNodeForPackage(Application.class);
        String currentTheme = preferences.get(Application.PREFERENCE_THEME, Application.THEME_LIGHT);
        if (Application.THEME_DARK.equalsIgnoreCase(currentTheme)) {
            themeComboBox.setSelectedItem("Dark");
        } else {
            themeComboBox.setSelectedItem("Light");
        }
        String currentCable = Application.getInstance() != null ? Application.getInstance().getCableStyle() : preferences.get(Application.PREFERENCE_CABLE_STYLE, Application.CABLE_STYLE_CURVED);
        if (Application.CABLE_STYLE_STRAIGHT.equalsIgnoreCase(currentCable)) {
            cableStyleComboBox.setSelectedItem("Straight");
        } else if (Application.CABLE_STYLE_ORTHOGONAL.equalsIgnoreCase(currentCable)) {
            cableStyleComboBox.setSelectedItem("Orthogonal");
        } else {
            cableStyleComboBox.setSelectedItem("Curved");
        }
        boolean gpu = preferences.getBoolean(Application.PREFERENCE_GPU_ACCELERATION, Application.DEFAULT_GPU_ACCELERATION);
        gpuAccelerationCheckBox.setSelected(gpu);
    }

    public void doCancel() {
        dispose();
    }

    public void doSave() {
        String selectedTheme = "Dark".equals(themeComboBox.getSelectedItem()) ? Application.THEME_DARK : Application.THEME_LIGHT;
        preferences.put(Application.PREFERENCE_THEME, selectedTheme);
        if (Application.getInstance() != null) {
            Application.getInstance().applyTheme(selectedTheme);
        } else {
            Theme.setTheme(selectedTheme);
        }

        String selectedCableStyle = Application.CABLE_STYLE_CURVED;
        if ("Straight".equals(cableStyleComboBox.getSelectedItem())) {
            selectedCableStyle = Application.CABLE_STYLE_STRAIGHT;
        } else if ("Orthogonal".equals(cableStyleComboBox.getSelectedItem())) {
            selectedCableStyle = Application.CABLE_STYLE_ORTHOGONAL;
        }
        if (Application.getInstance() != null) {
            Application.getInstance().setCableStyle(selectedCableStyle);
        } else {
            preferences.put(Application.PREFERENCE_CABLE_STYLE, selectedCableStyle);
        }

        boolean selectedGpu = gpuAccelerationCheckBox.isSelected();
        preferences.putBoolean(Application.PREFERENCE_GPU_ACCELERATION, selectedGpu);
        if (Application.getInstance() != null) {
            Application.getInstance().setGpuAccelerationEnabled(selectedGpu);
        }

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
        dispose();
    }

}
