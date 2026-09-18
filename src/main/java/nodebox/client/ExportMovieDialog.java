package nodebox.client;

import nodebox.movie.Movie;
import nodebox.movie.VideoFormat;
import nodebox.ui.Theme;
import nodebox.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * Dialog presented when exporting a movie.
 */
public class ExportMovieDialog extends JDialog implements ActionListener {

    private boolean dialogSuccessful = false;
    private JTextField fromField;
    private JTextField toField;
    private JTextField fileField;
    private JComboBox<VideoFormat> formatBox;

    private File exportPath;
    private int fromValue;
    private int toValue;
    private JButton exportButton;

    public ExportMovieDialog(Frame frame, File exportPath) {
        super(frame, "Export Movie");
        setModal(true);
        setResizable(false);

        this.exportPath = exportPath;

        // Main
        setLayout(new BorderLayout(5, 5));
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(new Theme.InsetsBorder(10, 10, 10, 10));
        add(mainPanel, BorderLayout.CENTER);

        // Directory
        JLabel fileLabel = new JLabel("File:  ");
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        filePanel.add(fileLabel);
        fileField = new JTextField(20);
        fileField.setEditable(false);
        filePanel.add(fileField);
        JButton chooseButton = new JButton("...");
        chooseButton.setPreferredSize(new Dimension(34, 25));
        chooseButton.addActionListener(this);
        filePanel.add(Box.createHorizontalStrut(5));
        filePanel.add(chooseButton);
        mainPanel.add(filePanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Format
        JLabel formatLabel = new JLabel("Format/Device: ");
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        formatPanel.add(formatLabel);
        mainPanel.add(formatPanel);
        formatBox = new JComboBox<>();
        for (VideoFormat format : Movie.VIDEO_FORMATS) {
            formatBox.addItem(format);
        }
        formatBox.setSelectedItem(Movie.DEFAULT_FORMAT);
        formatPanel.add(formatBox);

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
        if (exportPath == null)
            exportButton.setEnabled(false);
        buttonPanel.add(exportButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Apply theme styling
        Color dialogBg = Theme.DIALOG_BACKGROUND;
        getContentPane().setBackground(dialogBg);
        mainPanel.setBackground(dialogBg);
        filePanel.setBackground(dialogBg);
        formatPanel.setBackground(dialogBg);
        rangePanel.setBackground(dialogBg);
        buttonPanel.setBackground(dialogBg);

        fileLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        formatLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        fromLabel.setForeground(Theme.TEXT_NORMAL_COLOR);
        toLabel.setForeground(Theme.TEXT_NORMAL_COLOR);

        Color inputBg = Theme.isDark() ? new Color(26, 26, 30) : Color.WHITE;
        Color inputBorder = Theme.isDark() ? new Color(60, 60, 66) : new Color(195, 195, 202);
        fileField.setBackground(inputBg);
        fileField.setForeground(Theme.TEXT_NORMAL_COLOR);
        fileField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(inputBorder), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
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

        if (this.exportPath != null && this.exportPath.isFile())
            setExportPath(this.exportPath);
    }

    public static void main(String[] args) {
        ExportMovieDialog d = new ExportMovieDialog(null, null);
        d.setVisible(true);
    }

    private void doCancel() {
        setVisible(false);
    }

    private void doExport() {
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

    public File getExportPath() {
        return exportPath;
    }

    private void setExportPath(File d) {
        this.exportPath = d;
        if (this.exportPath == null)
            fileField.setText("");
        else
            fileField.setText(this.exportPath.getAbsolutePath());
        exportButton.setEnabled(this.exportPath != null);
    }

    public VideoFormat getVideoFormat() {
        return (VideoFormat) formatBox.getSelectedItem();
    }

    public int getFromValue() {
        return fromValue;
    }

    public int getToValue() {
        return toValue;
    }

    /**
     * Called when an output file needs to be chosen.
     *
     * @param e the action event
     */
    public void actionPerformed(ActionEvent e) {
        String path = exportPath == null ? null : exportPath.getAbsolutePath();
        File chosenFile = FileUtils.showSaveDialog(NodeBoxDocument.getCurrentDocument(), path, "mov,avi,mp4", "Movie files");
        setExportPath(chosenFile != null ? chosenFile : this.exportPath);
    }
}
