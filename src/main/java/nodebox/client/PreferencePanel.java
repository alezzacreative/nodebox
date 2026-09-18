package nodebox.client;

import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferencePanel extends JDialog implements ActionListener {

    private final Application application;
    private final Preferences preferences;
    private JComboBox<String> themeComboBox;
    private JComboBox<String> cableStyleComboBox;
    private JCheckBox enableGpuAccelerationCheck;
    private JLabel gpuStatusLabel;
    private JCheckBox enableDeviceSupportCheck;

    public PreferencePanel(Application application, Window owner) {
        super(owner, "Preferences");
        this.application = application;
        preferences = Preferences.userNodeForPackage(Application.class);
        JPanel rootPanel = new JPanel(new BorderLayout(10, 10));
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel appearance = new JLabel("Appearance");
        appearance.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        appearance.setMinimumSize(new Dimension(300, 20));
        appearance.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        contentPanel.add(appearance);

        JPanel themePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        themePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel themeLabel = new JLabel("Theme: ");
        themeComboBox = new JComboBox<String>(new String[]{"Light", "Dark"});
        themePanel.add(themeLabel);
        themePanel.add(themeComboBox);
        contentPanel.add(themePanel);

        contentPanel.add(Box.createVerticalStrut(5));

        JPanel cablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cableLabel = new JLabel("Cable Style: ");
        cableStyleComboBox = new JComboBox<String>(new String[]{"Curved", "Straight", "Orthogonal"});
        cablePanel.add(cableLabel);
        cablePanel.add(cableStyleComboBox);
        contentPanel.add(cablePanel);

        contentPanel.add(Box.createVerticalStrut(15));

        JLabel rendering = new JLabel("Rendering & Performance");
        rendering.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        rendering.setMinimumSize(new Dimension(300, 20));
        rendering.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        contentPanel.add(rendering);

        JPanel gpuPanel = new JPanel();
        gpuPanel.setLayout(new BoxLayout(gpuPanel, BoxLayout.Y_AXIS));
        gpuPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableGpuAccelerationCheck = new JCheckBox("Enable GPU Hardware Acceleration");
        enableGpuAccelerationCheck.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        enableGpuAccelerationCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        enableGpuAccelerationCheck.setToolTipText("Accelerates viewport rendering and canvas panning/zooming using GPU VRAM.");

        gpuStatusLabel = new JLabel("Pipeline: " + nodebox.util.GPUUtils.getPipelineDescription());
        gpuStatusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        gpuStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        gpuStatusLabel.setBorder(BorderFactory.createEmptyBorder(2, 22, 0, 0));

        gpuPanel.add(enableGpuAccelerationCheck);
        gpuPanel.add(gpuStatusLabel);
        contentPanel.add(gpuPanel);

        contentPanel.add(Box.createVerticalStrut(15));

        JLabel experimental = new JLabel("Experimental Features");
        experimental.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        experimental.setMinimumSize(new Dimension(300, 20));
        experimental.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        contentPanel.add(experimental);

        enableDeviceSupportCheck = new JCheckBox("Device Support");
        enableDeviceSupportCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(enableDeviceSupportCheck);

        rootPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 10));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        buttonPanel.add(cancelButton);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(this);
        buttonPanel.add(saveButton);
        rootPanel.add(buttonPanel, BorderLayout.SOUTH);

        Color dialogBg = Theme.DIALOG_BACKGROUND;
        rootPanel.setBackground(dialogBg);
        contentPanel.setBackground(dialogBg);
        themePanel.setBackground(dialogBg);
        cablePanel.setBackground(dialogBg);
        gpuPanel.setBackground(dialogBg);
        buttonPanel.setBackground(dialogBg);
        appearance.setForeground(Theme.TEXT_NORMAL_COLOR);
        rendering.setForeground(Theme.TEXT_NORMAL_COLOR);
        experimental.setForeground(Theme.TEXT_NORMAL_COLOR);
        themeLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        cableLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        enableGpuAccelerationCheck.setBackground(dialogBg);
        enableGpuAccelerationCheck.setForeground(Theme.TEXT_NORMAL_COLOR);
        gpuStatusLabel.setForeground(Theme.TEXT_DISABLED_COLOR);
        enableDeviceSupportCheck.setBackground(dialogBg);
        enableDeviceSupportCheck.setForeground(Theme.TEXT_NORMAL_COLOR);
        themeComboBox.setUI(new nodebox.ui.ThemeComboBoxUI());
        cableStyleComboBox.setUI(new nodebox.ui.ThemeComboBoxUI());
        cancelButton.setUI(new nodebox.ui.ThemeButtonUI());
        saveButton.setUI(new nodebox.ui.ThemeButtonUI());

        getRootPane().setDefaultButton(saveButton);

        readPreferences();

        setContentPane(rootPanel);
        setMinimumSize(new Dimension(340, 220));
        setResizable(false);
        pack();
    }

    private boolean isDeviceSupportEnabled() {
        return Boolean.valueOf(preferences.get(Application.PREFERENCE_ENABLE_DEVICE_SUPPORT, "false"));
    }

    private void setEnableDeviceSupport(boolean enabled) {
        application.ENABLE_DEVICE_SUPPORT = enabled;
        preferences.put(Application.PREFERENCE_ENABLE_DEVICE_SUPPORT, Boolean.toString(enabled));
    }

    private void readPreferences() {
        enableDeviceSupportCheck.setSelected(isDeviceSupportEnabled());
        String currentTheme = preferences.get(Application.PREFERENCE_THEME, Application.THEME_LIGHT);
        if (Application.THEME_DARK.equalsIgnoreCase(currentTheme)) {
            themeComboBox.setSelectedItem("Dark");
        } else {
            themeComboBox.setSelectedItem("Light");
        }
        String currentCableStyle = application.getCableStyle();
        if (Application.CABLE_STYLE_STRAIGHT.equalsIgnoreCase(currentCableStyle)) {
            cableStyleComboBox.setSelectedItem("Straight");
        } else if (Application.CABLE_STYLE_ORTHOGONAL.equalsIgnoreCase(currentCableStyle)) {
            cableStyleComboBox.setSelectedItem("Orthogonal");
        } else {
            cableStyleComboBox.setSelectedItem("Curved");
        }
        boolean gpu = preferences.getBoolean(Application.PREFERENCE_GPU_ACCELERATION, Application.DEFAULT_GPU_ACCELERATION);
        enableGpuAccelerationCheck.setSelected(gpu);
    }

    public void actionPerformed(ActionEvent actionEvent) {
        boolean restartNeeded = false;

        if (isDeviceSupportEnabled() != enableDeviceSupportCheck.isSelected()) {
            setEnableDeviceSupport(enableDeviceSupportCheck.isSelected());
            restartNeeded = true;
        }

        String selectedTheme = "Dark".equals(themeComboBox.getSelectedItem()) ? Application.THEME_DARK : Application.THEME_LIGHT;
        String currentTheme = preferences.get(Application.PREFERENCE_THEME, Application.THEME_LIGHT);
        if (!selectedTheme.equalsIgnoreCase(currentTheme)) {
            preferences.put(Application.PREFERENCE_THEME, selectedTheme);
            application.applyTheme(selectedTheme);
        }

        String selectedCableStyle = Application.CABLE_STYLE_CURVED;
        if ("Straight".equals(cableStyleComboBox.getSelectedItem())) {
            selectedCableStyle = Application.CABLE_STYLE_STRAIGHT;
        } else if ("Orthogonal".equals(cableStyleComboBox.getSelectedItem())) {
            selectedCableStyle = Application.CABLE_STYLE_ORTHOGONAL;
        }
        application.setCableStyle(selectedCableStyle);

        boolean currentGpu = preferences.getBoolean(Application.PREFERENCE_GPU_ACCELERATION, Application.DEFAULT_GPU_ACCELERATION);
        boolean selectedGpu = enableGpuAccelerationCheck.isSelected();
        if (currentGpu != selectedGpu) {
            preferences.putBoolean(Application.PREFERENCE_GPU_ACCELERATION, selectedGpu);
            application.setGpuAccelerationEnabled(selectedGpu);
            restartNeeded = true;
        }

        if (restartNeeded) {
            JOptionPane.showMessageDialog(this, "Please restart NodeBox for the changes to take effect.");
        }
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
        dispose();
    }
}
