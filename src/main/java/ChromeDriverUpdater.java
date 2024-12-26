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

    private final String root;
    private final Platform platform;

    public ChromeDriverUpdater(String root, Platform platform) {
        this.root = root;
        this.platform = platform;
    }

    /**
     * @return the name of the manifest file which should correspond to the version number of both chrome and chromedriver
     */

    public String readManifest() {
        File directory = new File(
                root + "/chrome-" + platform + "/chrome-" + platform + "/");
        if (directory.exists() && directory.isDirectory()) {
            String[] files = directory.list();
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".manifest")) {
                        System.out.print("Found manifest. ");
                        String version = file.substring(0, file.lastIndexOf("."));
                        System.out.println("Current version: " + version);
                        return version;
                    }
                }
            }
        }
        return "failed to get version from manifest";
    }

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

    public void downloadCtF(String version, String type) throws IOException {
        String urlString = "https://storage.googleapis.com/chrome-for-testing-public/"
                + version + "/" + platform + "/" + type + ".zip";
        try {
            System.out.println("Downloading " + type + " from " + urlString);
            URL chromeDriver = new URI(urlString).toURL();

            File directory = new File(root + "/chrome");
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    System.out.println("Created directory " + directory);
                }
            }

            try (InputStream inputStream = chromeDriver.openStream();
            FileOutputStream outputStream = new FileOutputStream(directory + "/" + type + ".zip")) {
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

    public void extractCtF(String type) throws IOException {
        System.out.println("Extracting " + type);
        try(ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(root + "/chrome/" + type + ".zip"))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {

                File file = new File(root + "/chrome/" + type + "/" + zipEntry.getName());
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

    public void deleteZip(String type) {
        File toDelete = new File(root + "/chrome/" + type + ".zip");
        if (toDelete.exists() && toDelete.delete()) {
            System.out.println("Deleted " + toDelete.getName() + " successfully.");
        } else {
            System.out.println("Failed to delete: " + toDelete.getName());
            System.out.println("Delete manually at: " + toDelete.getAbsolutePath());
        }
    }

    private void install(String version, String type) throws IOException {
        downloadCtF(version, type);
        extractCtF(type);
        deleteZip(type);
    }

    public void install(String version) throws IOException {
        install(version, "chrome-" + platform);
        install(version, "chromedriver-" + platform);
    }
}
