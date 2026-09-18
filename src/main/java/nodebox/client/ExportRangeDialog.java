package nodebox.client;

import nodebox.ui.ExportFormat;
import nodebox.ui.Platform;
import nodebox.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Dialog presented when exporting a range of frames.
 */
public class ExportRangeDialog extends JDialog implements ActionListener {

    private boolean dialogSuccessful = false;
    private JTextField fromField;
    private JTextField toField;
    private JTextField directoryField;
    private JComboBox<String> formatBox;

    private String exportPrefix;
    private File exportDirectory;
    private int fromValue;
    private int toValue;
    private ExportFormat format;
    private JTextField prefixField;
    private JButton exportButton;

    public ExportRangeDialog(Frame frame, File exportDirectory) {
        super(frame, "Export Range");
        setModal(true);
        setResizable(false);

        this.exportDirectory = exportDirectory;

        // Main
        setLayout(new BorderLayout(5, 5));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new Theme.InsetsBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);

        // Prefix
        JLabel prefixLabel = new JLabel("File Prefix:  ");
        JPanel prefixPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        prefixPanel.add(prefixLabel);
        prefixField = new JTextField("export", 20);
        prefixPanel.add(prefixField);
        mainPanel.add(prefixPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Directory
        JLabel directoryLabel = new JLabel("Directory:  ");
        JPanel directoryPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        directoryPanel.add(directoryLabel);
        directoryField = new JTextField(20);
        directoryField.setEditable(false);
        directoryPanel.add(directoryField);
        JButton chooseButton = new JButton("...");
        chooseButton.setPreferredSize(new Dimension(34, 25));
        chooseButton.addActionListener(this);
        directoryPanel.add(Box.createHorizontalStrut(5));
        directoryPanel.add(chooseButton);
        mainPanel.add(directoryPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Range
        JLabel fromLabel = new JLabel("From:");
        JLabel toLabel = new JLabel("To:");
        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 0));
        rangePanel.add(fromLabel);
        fromField = new JTextField("1", 5);
        rangePanel.add(fromField);
        rangePanel.add(toLabel);
        toField = new JTextField("100", 5);
        rangePanel.add(toField);
        mainPanel.add(rangePanel);

        // Format
        JLabel formatLabel = new JLabel("Format:");
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        formatPanel.add(formatLabel);
        formatBox = new JComboBox<>();
        formatBox.addItem("SVG");
        formatBox.addItem("PNG");
        formatBox.addItem("PDF");
        formatBox.setSelectedItem("SVG");
        formatPanel.add(formatBox);
        mainPanel.add(formatPanel);

        mainPanel.add(Box.createVerticalGlue());

        // Buttons
        mainPanel.add(Box.createVerticalStrut(10));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING, 10, 0));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doCancel();
            }
        });
        buttonPanel.add(cancelButton);
        exportButton = new JButton("Export");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                doExport();
            }
        });
        if (exportDirectory == null)
            exportButton.setEnabled(false);
        buttonPanel.add(exportButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Apply theme styling
        Color dialogBg = Theme.DIALOG_BACKGROUND;
        getContentPane().setBackground(dialogBg);
        mainPanel.setBackground(dialogBg);
        prefixPanel.setBackground(dialogBg);
        directoryPanel.setBackground(dialogBg);
        rangePanel.setBackground(dialogBg);
        formatPanel.setBackground(dialogBg);
        buttonPanel.setBackground(dialogBg);

        prefixLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        directoryLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        fromLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        toLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        formatLabel.setForeground(Theme.TEXT_NORMAL_COLOR);

        Color inputBg = Theme.isDark() ? new Color(26, 26, 30) : Color.WHITE;
        Color inputBorder = Theme.isDark() ? new Color(60, 60, 66) : new Color(195, 195, 202);
        prefixField.setBackground(inputBg);
        prefixField.setForeground(Theme.TEXT_NORMAL_COLOR);
        prefixField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(inputBorder), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        directoryField.setBackground(inputBg);
        directoryField.setForeground(Theme.TEXT_NORMAL_COLOR);
        directoryField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(inputBorder), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        fromField.setBackground(inputBg);
        fromField.setForeground(Theme.TEXT_NORMAL_COLOR);
        fromField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(inputBorder), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        toField.setBackground(inputBg);
        toField.setForeground(Theme.TEXT_NORMAL_COLOR);
        toField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(inputBorder), BorderFactory.createEmptyBorder(2, 4, 2, 4)));

        formatBox.setUI(new nodebox.ui.ThemeComboBoxUI());
        chooseButton.setUI(new nodebox.ui.ThemeButtonUI());
        cancelButton.setUI(new nodebox.ui.ThemeButtonUI());
        exportButton.setUI(new nodebox.ui.ThemeButtonUI());

        pack();
        getRootPane().setDefaultButton(exportButton);

        setExportDirectory(exportDirectory);
    }

    private void doCancel() {
        setVisible(false);
    }

    private void doExport() {
        exportPrefix = prefixField.getText();
        try {
            fromValue = Integer.valueOf(fromField.getText());
        } catch (NumberFormatException e) {
            fromValue = 1;
        }
        try {
            toValue = Integer.valueOf(toField.getText());
        } catch (NumberFormatException e) {
            toValue = 100;
        }
        dialogSuccessful = true;
        setVisible(false);
    }

    public boolean isDialogSuccessful() {
        return dialogSuccessful;
    }

    public String getExportPrefix() {
        return exportPrefix;
    }

    public File getExportDirectory() {
        return exportDirectory;
    }

    private void setExportDirectory(File d) {
        this.exportDirectory = d;
        if (this.exportDirectory == null) {
            directoryField.setText("");
        } else {
            directoryField.setText(this.exportDirectory.getAbsolutePath());
        }
        exportButton.setEnabled(this.exportDirectory != null);
    }

    public int getFromValue() {
        return fromValue;
    }

    public int getToValue() {
        return toValue;
    }

    public ExportFormat getFormat() {
        return ExportFormat.of(formatBox.getSelectedItem().toString());
    }

    /**
     * Called when a directory needs to be chosen.
     *
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e) {
        if (Platform.onMac()) {
            // On Mac, we can use the native FileDialog to choose a directory using a special property.
            FileDialog fileDialog = new FileDialog((Frame) null);
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fileDialog.setVisible(true);
            System.setProperty("apple.awt.fileDialogForDirectories", "false");
            String chosenFile = fileDialog.getFile();
            if (chosenFile == null) {
                setExportDirectory(null);
                return;
            }
            String dir = fileDialog.getDirectory();
            File f = new File(dir, chosenFile);
            if (!f.isDirectory()) {
                setExportDirectory(f.getParentFile());
            } else {
                setExportDirectory(f);
            }
        } else {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int retVal = chooser.showOpenDialog(null);
            if (retVal == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                assert file.isDirectory();
                setExportDirectory(file);
            } else {
                setExportDirectory(null);
            }
        }
    }

    public static void main(String[] args) {
        ExportRangeDialog d = new ExportRangeDialog(null, null);
        d.setVisible(true);
    }
}
