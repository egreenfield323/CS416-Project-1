import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class ConfigParser {

    private final File configFile;

    public ConfigParser(File configFile) {
        this.configFile = configFile;
    }

    public static void main(String[] args) {
        ConfigParser config_parser = new ConfigParser(new File("config_files/complete.conf"));
        System.out.println(config_parser.getTableFromMac("R1"));
    }

    public VirtualPort[] getNeighbors(String mac) {
        List<VirtualPort> result = new ArrayList<>();

        Properties prop = new Properties();


        try (FileInputStream fis = new FileInputStream(this.configFile)) {
            prop.load(fis);

            List<String> macs = new ArrayList<>();

            for (Enumeration<?> e = prop.propertyNames(); e.hasMoreElements(); ) {
                String current_mac = (String) e.nextElement();
                if (current_mac.equals(mac)) {
                    continue;
                }

                if (current_mac.startsWith(mac)) {
                    macs.add(current_mac.substring(current_mac.indexOf('-') + 1));
                } else if (current_mac.endsWith(mac)) {
                    macs.add(current_mac.substring(0, current_mac.indexOf('-')));
                }
            }

            for (String current_mac : macs) {
                String info = prop.getProperty(current_mac);
                String ip = info.substring(0, info.indexOf(':'));
                int port = Integer.parseInt(info.substring(info.indexOf(':') + 1));
                InetAddress inet_address = InetAddress.getByName(ip);
                result.add(new VirtualPort(inet_address, port));
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
            InetAddress inet_address = InetAddress.getByName(ip);
            result = (new VirtualPort(inet_address, port));

        } catch (IOException e) {
            System.err.println("Could not read config file " + e);
        }
        return result;
    }

    public String ResolveAddress(VirtualIP vIP) {
        String mac = "";
        Properties prop = new Properties();

        try (FileInputStream fis = new FileInputStream(this.configFile)) {

            prop.load(fis);
            mac = prop.getProperty(vIP.toString());

        } catch (IOException e) {
            System.err.println("Could not read config file " + e);
        }

        if (mac.equals("")) {
            System.err.println("Could not find Mac from " + vIP.toString());
        }

        return mac;
    }

    public VirtualPort GetVirtualPort(VirtualIP vIP) {
        return getVirtualPort(ResolveAddress(vIP));
    }

    public RoutingTable getTableFromMac(String mac) {
        RoutingTable table = new RoutingTable();

        try (BufferedReader br = new BufferedReader(new FileReader(this.configFile))) {
            String line;
            boolean isParsingTable = false;

            while ((line = br.readLine()) != null) {
                line = line.trim();

                // Detect when we enter R1 or R2's table
                if (line.equals("R1 TABLE") && mac.equals("R1")) {
                    isParsingTable = true;
                    continue;
                } else if (line.equals("R2 TABLE") && mac.equals("R2")) {
                    isParsingTable = true;
                    continue;
                } else if (line.isEmpty() || line.contains("ADDRESS RESOLUTION") || line.contains("LINKS")) {
                    isParsingTable = false;
                    continue;
                }

                if (isParsingTable) {
                    // Handles new table entries
                    if (line.contains("-") && !line.contains(".")) {
                        // Port entry (e.g., net1-S1)
                        String[] parts = line.split("-");
                        if (parts.length == 2) {
                            table.addPortEntry(parts[0], parts[1]);
                        }
                    } else if (line.contains("-") && line.contains(".")) {
                        // Next-hop entry (e.g., net3-net2.R1)
                        String[] parts = line.split("-");
                        if (parts.length == 2) {
                            table.addNextHopEntry(parts[0], new VirtualIP(parts[1]));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read config file: " + e);
        }
        return table;
    }
}