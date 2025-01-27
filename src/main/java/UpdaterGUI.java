import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class UpdaterGUI {

    private JFrame frame;
    private JComboBox<Platform> platformComboBox;
    private ButtonGroup typeButtonGroup;
    private JRadioButton headlessMode;
    private JRadioButton headfulMode;

    private ChromeDriverUpdater chromeDriverUpdater;
    private String selectedDirectory;

    private void createWindow() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setPreferredSize(new Dimension(200, 180));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        frame.add(mainPanel);

        setupComponents(mainPanel);

        frame.pack();
        frame.setVisible(true);
    }
    private void setupComponents(JPanel panel) {
        platformComboBox = new JComboBox<>(Platform.values());
        platformComboBox.setUI(new BasicComboBoxUI());

        JButton browseButton = createBrowseButton();
        JButton installButton = createInstallButton();

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(browseButton);
        buttonPanel.add(installButton);

        panel.add(createTypeSelectionCheckBoxes(), BorderLayout.CENTER);
        panel.add(platformComboBox, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    }


    private JPanel createTypeSelectionCheckBoxes() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        headlessMode = new JRadioButton("headless");
        headlessMode.setSelected(true);
        headfulMode = new JRadioButton("head included");
        typeButtonGroup = new ButtonGroup();
        typeButtonGroup.add(headlessMode);
        typeButtonGroup.add(headfulMode);

        JCheckBox driver = new JCheckBox("chrome driver");
        driver.setSelected(true);
        driver.setEnabled(false);

        panel.add(headlessMode);
        panel.add(headfulMode);
        panel.add(driver);

        return panel;
    }

    private JButton createBrowseButton() {
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(_ -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

            int returnValue = fileChooser.showOpenDialog(frame);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedDirectory = fileChooser.getSelectedFile().toPath().toString();
                JOptionPane.showMessageDialog(frame,
                        "Selected directory: " + selectedDirectory);
            }
        });
        return browseButton;
    }

    private JButton createInstallButton() {
        JButton installButton = new JButton("Install");
        installButton.addActionListener(_ -> {
            if (selectedDirectory == null) {
                selectedDirectory = System.getProperty("user.dir");
            }
            Platform selectedPlatform = (Platform) platformComboBox.getSelectedItem();
            chromeDriverUpdater = new ChromeDriverUpdater(selectedDirectory, selectedPlatform);

            int result = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to install chrome for "
            + selectedPlatform + " at " + selectedDirectory + "?", "Confirm installation", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    String latestVersion = chromeDriverUpdater.getLatestVersion();
                    String currentVersion = chromeDriverUpdater.readVCfile();
                    if (!currentVersion.equals(latestVersion)) {
                        ButtonModel selectionModel = typeButtonGroup.getSelection();
                        if (selectionModel == headlessMode.getModel()) {
                            chromeDriverUpdater.installHeadless(latestVersion);
                        } else if (selectionModel == headfulMode.getModel()) {
                            chromeDriverUpdater.installHeadful(latestVersion);
                        } else {
                            throw new RuntimeException("ruh roh!");
                        }
                        chromeDriverUpdater.writeVCfile(latestVersion);
                        JOptionPane.showMessageDialog(frame,
                                "Installation successful: Version " + latestVersion);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                "Already up-to-date: Version " + currentVersion);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        });

        return installButton;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UpdaterGUI updaterGUI = new UpdaterGUI();
            updaterGUI.createWindow();
        });
    }
}
