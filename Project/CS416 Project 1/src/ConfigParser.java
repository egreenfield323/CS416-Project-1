import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class ConfigParser {

    private final File configFile;

    public ConfigParser(File configFile) {
        this.configFile = configFile;
    }

    public VirtualPort[] getNeighbors(String mac) {
        List<VirtualPort> result = new ArrayList<>();

        Properties prop = new Properties();


        try (FileInputStream fis = new FileInputStream(this.configFile)) {
            prop.load(fis);

            List<String> macs = new ArrayList<>();

            for (Enumeration<?> e = prop.propertyNames(); e.hasMoreElements(); ) {
                String current_mac = (String) e.nextElement();
                if (current_mac.startsWith(mac) && !current_mac.equals(mac)) {
                    macs.add(current_mac.substring(current_mac.indexOf('-') + 1));
                }
            }

            for (String current_mac : macs) {
                String info = prop.getProperty(current_mac);
                String ip = info.substring(0, info.indexOf(':'));
                int port = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                result.add(new VirtualPort(ip, port));
            }

        } catch (IOException e) {
            System.err.println("Could not read config file " + e);
        }

        if (result.isEmpty()) {
            System.err.println("Could not find any neighbors for MAC address " + mac);
        }

        return result.toArray(new VirtualPort[0]);
    }

    public VirtualPort getVirtualPort(String mac) {
        VirtualPort result = null;

        Properties prop = new Properties();

        try (FileInputStream fis = new FileInputStream(this.configFile)) {
            prop.load(fis);

            String info = prop.getProperty(mac);
            String ip = info.substring(0, info.indexOf(':'));
            int port = Integer.parseInt(info.substring(info.indexOf(':') + 1));
            result = (new VirtualPort(ip, port));

        } catch (IOException e) {
            System.err.println("Could not read config file " + e);
        }
        return result;
    }
}