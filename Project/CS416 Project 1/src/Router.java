import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Router {
    private static final int PACKET_SIZE = 1024;

    private static ConfigParser config;
    private static String mac;
    private static int hostingPort;
    private static RoutingTable routingTable;

    // Args: [config file path] [virtual MAC address]
    // Example: ./config_files/local.conf S1
    public static void main(String[] args) {
        readArgs(args);
        waitForPackets();
    }

    private static void readArgs(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: [config file path] [virtual MAC address]");
            System.exit(1);
        }

        mac = args[1];

        System.out.println("Reading config: " + args[0] + ", with MAC " + mac);

        File configPath = new File(System.getProperty("user.dir"), args[0]);
        if (!configPath.isFile()) {
            System.err.println(configPath + " does not exist or is not a file.");
            System.exit(1);
        }

        config = new ConfigParser(configPath);

        try {
            hostingPort = config.getVirtualPort(mac).port;
        }
        catch (Exception e) { // Update later with custom exception
            System.err.println(mac + " is not a valid virtual MAC address.");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        try {
            routingTable = config.getTableFromMac(mac);
        }
        catch (Exception e) { // Update later
            System.err.println(mac + " does not have a routing table.");
        }

        System.out.println("Routing table is initialized to:\n" + routingTable);
    }

    private static void waitForPackets() {
        try(DatagramSocket socket = new DatagramSocket(hostingPort)) {
            while (true) {
                receivePacket(socket);
            }
        } catch (SocketException exception) {
            System.err.println("Unable to get socket for port " + hostingPort);
            System.err.println("Details: " + exception.getMessage());
            System.exit(1);
        } catch (IOException exception) {
            System.err.println("An IOException occurred while handling a packet: " + exception.getMessage());
            System.err.println("(Packet was dropped.)");
        }
    }

    private static void receivePacket(DatagramSocket socket) throws IOException {
        DatagramPacket receivedPacket = new DatagramPacket(
                new byte[PACKET_SIZE], PACKET_SIZE
        );

        socket.receive(receivedPacket);
        Frame frame = new Frame();
        frame.readPacket(receivedPacket);

        // This should only occur when a switch floods.
        if (frame.destIp.getSubnet().equals(frame.sourceIp.getSubnet())) {
            System.out.printf("Received frame intended to stay in LAN (%s), dropping.\n", frame.destIp.getSubnet());
            return;
        }

        String forwardToSubnet = frame.destIp.getSubnet();
        VirtualIP nextHopRouter = routingTable.getNextHop(frame.destIp.getSubnet());
        VirtualPort subnetPort;

        frame.sourceMac = mac;

        if (nextHopRouter != null) {
            System.out.printf("Next hop to subnet %s is through %s.\n", forwardToSubnet, nextHopRouter);
            forwardToSubnet = nextHopRouter.getSubnet();
            frame.destMac = config.ResolveAddress(nextHopRouter);
        }
        else {
            frame.destMac = config.ResolveAddress(frame.destIp);
            System.out.printf(
                    "Subnet %s is directly connected, addressing to MAC %s.\n",
                    forwardToSubnet, frame.destMac
            );
        }

        try {
            // Mac address is used because it makes the config file nicer, in reality, mac isn't needed, just the
            // port. Similarly, you would not need to resolve the mac address into a port, that is just for the
            // config to be nicer. (Just pretend we get the port with the subnet.)
            String subnetNeighborMac = routingTable.resolveNeighboringSubnetMac(forwardToSubnet);
            subnetPort = config.getVirtualPort(subnetNeighborMac);
            System.out.printf("Subnet %s is through port %s, forwarding packet.\n", forwardToSubnet, subnetPort);

            DatagramPacket forwardedPacket = frame.writePacket(subnetPort);
            socket.send(forwardedPacket);
        } catch (RoutingTable.RouterTableMissException exception) {
            System.out.printf(
                    "Error: Subnet %s did not resolve to a port, and no default gateway for this router is configured, "
                            + "dropping the packet.\n", forwardToSubnet
            );
            //return;
        }
    }
}
