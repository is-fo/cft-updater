import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.io.IOException;

public class UpdaterGUI {

    private JFrame frame;
    private JComboBox platformComboBox;

    private ChromeDriverUpdater chromeDriverUpdater;
    private String selectedDirectory;

    private void createWindow() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setPreferredSize(new Dimension(200, 100));

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

        panel.add(platformComboBox, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private JButton createBrowseButton() {
        JButton browseButton = new JButton("Browse");
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

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
        installButton.addActionListener(e -> {


            if (selectedDirectory == null) {
                selectedDirectory = System.getProperty("user.dir");
            }

            Platform selectedPlatform = (Platform) platformComboBox.getSelectedItem();
            chromeDriverUpdater = new ChromeDriverUpdater(selectedDirectory, selectedPlatform);

            int result = JOptionPane.showConfirmDialog(frame,
                    "Are you sure you want to install "
            + selectedPlatform + " at " + selectedDirectory + "?", "Confirm installation", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                try {
                    String latestVersion = chromeDriverUpdater.getLatestVersion();
                    String currentVersion = chromeDriverUpdater.readManifest();
                    if (!currentVersion.equals(latestVersion)) {
                        chromeDriverUpdater.install(latestVersion);
                        JOptionPane.showMessageDialog(frame,
                                "Installation successful: Version " + latestVersion);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                "Already up-to-date: Version " + currentVersion);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame,
                            "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
