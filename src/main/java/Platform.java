public enum Platform {

    LINUX64("linux64"),
    MAC_ARM64("mac-arm64"),
    MAC_X64("mac-x64"),
    WIN32("win32"),
    WIN64("win64");

    private final String platform;

    Platform(String platform) {
        this.platform = platform;
    }

    @Override
    public String toString() {
        return platform;
    }
}
