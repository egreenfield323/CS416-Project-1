import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Host {
    private static final int PACKET_SIZE = 1024;

    private static VirtualPort switchPort;
    private static int port;
    private static String sourceMac;

    public static void main(String[] args) {
        readArgs(args);
        try (DatagramSocket socket = new DatagramSocket(port)) {
            ExecutorService service = Executors.newFixedThreadPool(1);

            try {
                service.submit(() -> receivePackets(socket));
                sendPacketsInteractively(socket);
            }
            finally {
                service.shutdown();
            }
        }
        catch (SocketException exception) {
            System.err.println("Could not get socket for port " + port + ": " + exception.getMessage());
        }
    }

    private static void readArgs(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: [config file path] [virtual MAC address]");
            System.exit(1);
        }

        sourceMac = args[1];

        System.out.println("Reading config: " + args[0] + ", with MAC " + sourceMac);

        File configPath = new File(System.getProperty("user.dir"), args[0]);
        if (!configPath.isFile()) {
            System.err.println(configPath + " does not exist or is not a file.");
            System.exit(1);
        }

        ConfigParser config = new ConfigParser(configPath);

        try {
            switchPort = config.getNeighbors(sourceMac)[0];
            port = config.getVirtualPort(sourceMac).port;
        }
        catch (Exception e) { // Update later with custom exception
            System.err.println(sourceMac + " is not a valid virtual MAC address.");
            System.exit(1);
        }
    }

    private static void sendPacketsInteractively(DatagramSocket socket) {
        try (Scanner in = new Scanner(System.in)) {
            while (true) {
                System.out.println("Enter a mac address to send this packet to:");
                String destMac = in.nextLine();

                System.out.printf("Enter a message to send to %s:\n", destMac);
                String message = in.nextLine();

                Frame frame = new Frame(sourceMac, destMac, message);
                DatagramPacket packet = frame.writePacket(switchPort.ip, switchPort.port);

                try {
                    socket.send(packet);
                }
                catch (IOException exception) {
                    System.err.println("Failed to send packet: " + exception.getMessage());
                }
            }
        }
    }

    private static void receivePackets(DatagramSocket socket) {
        while (true)
        {
            DatagramPacket receivedPacket = new DatagramPacket(
                    new byte[PACKET_SIZE], PACKET_SIZE
            );

            try {
                socket.receive(receivedPacket);
                Frame frame = new Frame();
                frame.readPacket(receivedPacket);

                if (!frame.destMac.equals(sourceMac)) {
                    System.out.printf(
                            "Received packet addressed to %s from %s, dropping.\n",
                            frame.destMac, frame.sourceMac
                    );
                }

                System.out.printf("Received from %s:\n%s\n", frame.sourceMac, frame.message);
            }
            catch (IOException exception) {
                System.err.println("Failure receiving packet: " + exception.getMessage());
                break;
            }
        }
    }


}
