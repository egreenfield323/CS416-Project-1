import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

public class Switch {
    private static class TableMissException extends RuntimeException {
        public TableMissException(String message) {
            super(message);
        }
    }

    private static final int PACKET_SIZE = 1024;

    private static VirtualPort[] ports;
    private static final HashMap<String, VirtualPort> addressTable = new HashMap<>();
    private static int hostingPort;

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

        System.out.println("Reading config: " + args[0] + ", with MAC " + args[1]);

        File configPath = new File(System.getProperty("user.dir"), args[0]);
        if (!configPath.isFile()) {
            System.err.println(configPath + " does not exist or is not a file.");
            System.exit(1);
        }

        ConfigParser config = new ConfigParser(configPath);

        try {
            ports = config.getNeighbors(args[1]);
            hostingPort = config.getVirtualPort(args[1]).port;
        }
        catch (Exception e) { // Update later with custom exception
            System.err.println(args[1] + " is not a valid virtual MAC address.");
            System.exit(1);
        }
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
        VirtualPort sourcePort = frame.readPacket(receivedPacket);

        ensureInTable(frame.sourceMac, sourcePort);
        try {
            VirtualPort port = getTableEntry(frame.destMac);
            System.out.println("Table Hit: " + frame.destMac + " -> " + port);
            socket.send(frame.writePacket(port));
        }
        catch (TableMissException exception) {
            System.out.println("Table Miss: " + exception.getMessage());
            System.out.println("Flooding packet.");
            for (VirtualPort port: ports)
            {
                // Do not send back to the source.
                if (port.equals(sourcePort))
                {
                    continue;
                }
                socket.send(frame.writePacket(port));
            }
        }
    }

    private static VirtualPort getTableEntry(String destinationMac) throws TableMissException{
        if (!addressTable.containsKey(destinationMac))
        {
            throw new TableMissException(destinationMac + " not found in MAC table.");
        }

        return addressTable.get(destinationMac);
    }

    // This method will override previous table entries, leaving it vulnerable to MAC address
    // spoofing being used for communication interception.
    private static void ensureInTable(String sourceMac, VirtualPort port)
    {
        addressTable.put(sourceMac, port);
    }

}
