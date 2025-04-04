import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class Router {
    private static final int PACKET_SIZE = 1024;

    private static ConfigParser config;
    private static String mac;
    private static int hostingPort;
    private static RoutingTable routingTable;
    private static DistanceVector currentDistanceVector;
    private static final ReentrantLock distanceVectorLock = new ReentrantLock();

    // Args: [config file path] [virtual MAC address]
    // Example: ./config_files/local.conf S1
    public static void main(String[] args) {
        readArgs(args);
        try (DatagramSocket socket = new DatagramSocket(hostingPort)) {
            ExecutorService service = Executors.newFixedThreadPool(1);

            try {
                service.submit(() -> sendDVPackets(socket));
                waitForPackets(socket);
            } finally {
                service.shutdown();
            }
        } catch (SocketException exception) {
            System.err.println("Unable to get socket for port " + hostingPort);
            System.err.println("Details: " + exception.getMessage());
            System.exit(1);
        }
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
            currentDistanceVector = routingTable.getInitialDistanceVector();
        }
        catch (Exception e) { // Update later
            System.err.println(mac + " does not have a routing table.");
        }

        System.out.println("Routing table is initialized to:\n" + routingTable);
    }

    private static void sendDVPackets(DatagramSocket socket) {
        System.out.println(
                "Press enter to force advertise the current distance vector. This should only be necessary for one "
                + "router in the network, as it will trigger the others to advertise."
        );
        try (Scanner scanner = new Scanner(System.in)) {
            scanner.nextLine();
        }

        advertiseDistanceVector(socket);
    }

    private static void waitForPackets(DatagramSocket socket) {
        while (true) {
            try {
                receivePacket(socket);
            }
            catch (IOException exception) {
                System.err.println("An IOException occurred while handling a packet: " + exception.getMessage());
                System.err.println("(Packet was dropped.)");
            }
        }
    }

    private static void receivePacket(DatagramSocket socket) throws IOException {
        DatagramPacket receivedPacket = new DatagramPacket(
                new byte[PACKET_SIZE], PACKET_SIZE
        );

        socket.receive(receivedPacket);
        Frame frame = new Frame();
        frame.readPacket(receivedPacket);

        switch (frame.identifyFrame()) {
            case DISTANCE_VECTOR -> handleDistanceVectorFrame(frame, socket);
            case MESSAGE -> handleMessageFrame(frame, socket);
        }

    }

    private static void handleDistanceVectorFrame(Frame frame, DatagramSocket socket) {
        DistanceVectorFrame vectorFrame = new DistanceVectorFrame(frame);
        DistanceVector neighborDistanceVector = vectorFrame.getDistanceVector();

        System.out.printf("Received distance vector frame from %s.\n", frame.sourceIp.toString());
        distanceVectorLock.lock();
        String[] entriesUpdated;
        try {
            entriesUpdated = currentDistanceVector.updateEntries(neighborDistanceVector);
            // Yes it is generally a bad idea performance wise to make this block wait on graphical output, but without
            // duplicating the distance vector I can't ensure it won't be changed asynchronously outside this block
            // before it is read.
            System.out.println("Distance vector was updated: \n");
            System.out.println(currentDistanceVector.toString());
        }
        finally {
            distanceVectorLock.unlock();
        }

        if (entriesUpdated.length > 0) {

            VirtualIP senderIP = frame.sourceIp;

            for (String subnet: entriesUpdated) {
                System.out.printf("Updated table to route %s through %s\n", subnet, senderIP.toString());
                routingTable.addNextHopEntry(subnet, senderIP);
            }

            advertiseDistanceVector(socket);
        }
    }

    private static void advertiseDistanceVector(DatagramSocket socket) {
        distanceVectorLock.lock();
        try {
            for (String subnet: routingTable.getDirectlyConnectedSubnets()) {
                VirtualPort subnetNeighborPort = config.getVirtualPort(
                        routingTable.resolveNeighboringSubnetMac(subnet)
                );
                Frame frame = new Frame(
                        mac, Frame.BROADCAST_MAC,
                        config.getRouterIPForSubnet(mac, subnet), VirtualIP.BROADCAST
                );

                DistanceVectorFrame vectorFrame = new DistanceVectorFrame(frame);
                vectorFrame.addDistanceVector(currentDistanceVector);

                DatagramPacket packet = frame.writePacket(subnetNeighborPort);
                socket.send(packet);
            }
        } catch (IOException e) {
            System.err.println("Failed to send DV packets: " + e.getMessage());
        } finally {
            distanceVectorLock.unlock();
        }
    }

    private static void handleMessageFrame(Frame frame, DatagramSocket socket) throws IOException{
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
