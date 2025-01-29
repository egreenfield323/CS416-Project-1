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
    // Example: ./config_files/complete.conf S1
    public static void main(String[] args) {
        readArgs(args);
        waitForPackets();
    }

    private static void readArgs(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: [config file path] [virtual MAC address]");
            System.exit(1);
        }

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
        frame.readPacket(receivedPacket);

        // The destination IP and port are actually the sender's ip and port, since DatagramPacket
        // only stores either the source or destination (depending on whether this switch is sending or
        // receiving the packet.)
        ensureInTable(frame.sourceMac, new VirtualPort(frame.destIp, frame.destPort));
        try {
            VirtualPort port = getTableEntry(frame.destMac);
            System.out.println("Table Hit: " + frame.destMac + " -> " + port);
            socket.send(addressPacketTo(receivedPacket, port));
        }
        catch (TableMissException exception) {
            System.out.println("Table Miss: " + exception.getMessage());
            System.out.println("Flooding packet.");
            // Similarly to the previous case, dest ports are source since this frame was received.
            VirtualPort sourcePort = new VirtualPort(frame.destIp, frame.destPort);
            for (VirtualPort port: ports)
            {
                if (port == sourcePort)
                {
                    continue;
                }
                socket.send(addressPacketTo(receivedPacket, port));
            }
        }
    }

    private static DatagramPacket addressPacketTo(DatagramPacket packet, VirtualPort port)
    {
        // This is not passed through Frame because we want to preserve the original source and
        // destination MAC address, all we are doing is forwarding it.
        return new DatagramPacket(
                    packet.getData(),
                    packet.getLength(),
                    port.ip,
                    port.port
        );
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
