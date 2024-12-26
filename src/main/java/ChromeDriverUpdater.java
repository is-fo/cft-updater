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

    public static void main(String[] args) {
        try {
            String latestVersion = getLatestVersion();
            String currentVersion = readManifest(new File("chrome/chrome-win64/chrome-win64/"));
            if (!currentVersion.equals(latestVersion)) {
                install(latestVersion);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param directory should be chrome/chrome-win64/chrome-win64/ (for now)
     * @return the name of the manifest file which should correspond to the version number of both chrome and chromedriver
     */

    private static String readManifest(File directory) {
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

    private static String getLatestVersion() throws IOException {
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

    private static void downloadCtF(String version, String type) throws IOException {
        String urlString = "https://storage.googleapis.com/chrome-for-testing-public/"
                + version + "/win64/" + type + ".zip";
        try {
            System.out.println("Downloading " + type + " from " + urlString);
            URL chromeDriver = new URI(urlString).toURL();

            try (InputStream inputStream = chromeDriver.openStream();
            FileOutputStream outputStream = new FileOutputStream("chrome/" + type + ".zip")) {
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

    private static void extractCtF(String type) throws IOException {
        System.out.println("Extracting " + type);
        try(ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream("chrome/" + type + ".zip"))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {

                File file = new File("chrome\\" + type + "\\" + zipEntry.getName());
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

    private static void install(String version, String type) throws IOException {
        downloadCtF(version, type);
        extractCtF(type);
    }

    private static void install(String latestVersion) throws IOException {
        install(latestVersion, "chrome-win64");
        install(latestVersion, "chromedriver-win64");
    }
}
