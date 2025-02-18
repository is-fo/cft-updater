import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <a href=https://googlechromelabs.github.io/chrome-for-testing/>Chrome for Testing</a>
 */
public class ChromeDriverUpdater {

    private final Platform platform;

    private String chromeDir;

    private final String fs = File.separator;

    public ChromeDriverUpdater(String root, Platform platform) {
        validateRoot(root);
        this.platform = platform;
    }

    private void validateRoot(String root) {
        File rootDir = new File(root);
        File potentialChromeDir = new File(rootDir, "chrome");
        if (potentialChromeDir.getName().equals(rootDir.getName())) { //if user selected chrome directory
            throw new IllegalArgumentException("Select the parent directory of 'chrome' or delete the 'chrome' directory");
        }

        if (!potentialChromeDir.exists()) {
            try {
                if (potentialChromeDir.mkdir()) {
                    System.out.println("Created directory at: " + potentialChromeDir.getAbsolutePath());
                } else {
                    System.out.println("Failed to create directory at: " + potentialChromeDir.getAbsolutePath());
                }
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        chromeDir = potentialChromeDir.getAbsolutePath();
    }

    /**
     * Attempts to find the ".version" file that this program writes
     * @return the version number if it exists
     */
    public String readVCfile() {
        String location = chromeDir + fs;
        File directory = new File(location);
        if (directory.exists() && directory.isDirectory()) {
            String[] files = directory.list();
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".version")) {
                        System.out.print("Found version. ");
                        String version = file.substring(0, file.lastIndexOf("."));
                        System.out.println("Current version: " + version);
                        return version;
                    }
                }
            }
        }
        return "failed to get version";
    }

    /**
     *
     * @param versionNumber Creates an empty file using this parameter as filename with ".version" appended
     * @return returns true if a file was successfully created (optional)
     */

    public boolean writeVCfile(String versionNumber) {
        try {
            File file = new File(chromeDir + fs + versionNumber + ".version");
            return file.createNewFile();
        } catch (IOException e) {
            System.err.println("Failed to write chrome version control file. " + e.getMessage());
        }
        return false;
    }

    /**
     * Sends an HTTP request to GET the latest version of Chrome for Testing.
     * @return The latest version of Chrome for Testing
     */
    public String getLatestVersion() throws IOException {
        String urlString = "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_STABLE";
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            InputStream inputStream = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String latestVersion = reader.readLine();
            System.out.println("Latest stable Chrome version: " + latestVersion);

            reader.close();
            inputStream.close();

            return latestVersion;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "failed to get latest version";
    }

    /**
     * @param version The version number retrieved from getLatestVersion() but any valid version number *should* work
     * @param type Valid types are chrome, chromedriver or chrome-headless-shell
     */
    public void downloadCtF(String version, String type) throws IOException {
        String urlString = "https://storage.googleapis.com/chrome-for-testing-public/"
                + version + "/" + platform + "/" + type + ".zip";
        try {
            System.out.println("Downloading " + type + " from " + urlString);
            URL chromeDownload = new URI(urlString).toURL();

            try (InputStream inputStream = chromeDownload.openStream();
                 FileOutputStream outputStream = new FileOutputStream(chromeDir + fs + type + ".zip")) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unzips
     */
    public void extractCtF(String type) throws IOException {
        System.out.println("Extracting " + type);
        try(ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(chromeDir + fs + type + ".zip"))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {

                File file = new File(chromeDir + fs + type + fs + zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (!file.isDirectory() && !file.mkdirs()) {
                        throw new IOException("Failed to create directory " + file);
                    }
                } else {
                    File parent = file.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                }

                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }
            zipInputStream.closeEntry();
        }
        System.out.println(type + " downloaded and extracted successfully.");
    }

    /**
     * Deletes the downloaded zip-files
     */
    public void deleteZip(String type) {
        File toDelete = new File(chromeDir + fs + type + ".zip");
        if (toDelete.exists() && toDelete.delete()) {
            System.out.println("Deleted " + toDelete.getName() + " successfully.");
        } else {
            System.out.println("Failed to delete: " + toDelete.getName());
            System.out.println("Delete manually at: " + toDelete.getAbsolutePath());
        }
    }

    /**
     * Downloads, extracts and deletes zip files automatically.
     */
    private void install(String version, String type) throws IOException {
        downloadCtF(version, type);
        extractCtF(type);
        deleteZip(type);
    }

    public void installHeadful(String version) throws IOException {
        install(version, "chromedriver-" + platform);
        install(version, "chrome-" + platform);
    }

    public void installHeadless(String version) throws IOException {
        install(version, "chromedriver-" + platform);
        install(version, "chrome-headless-shell-" + platform);
    }

    public void installAll(String version) throws IOException {
        install(version, "chrome-" + platform);
        install(version, "chromedriver-" + platform);
        install(version, "chrome-headless-shell-" + platform);
    }
}
