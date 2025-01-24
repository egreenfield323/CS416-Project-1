import java.io.File;

public class ConfigParser {

    private File configFile;

    public ConfigParser(File configFile) {
        this.configFile = configFile;
    }

    public static VirtualPort[] getNeighbors() {
        VirtualPort[] result = new VirtualPort[]{};

        return result;
    }
}