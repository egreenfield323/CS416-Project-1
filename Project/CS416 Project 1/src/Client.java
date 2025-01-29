import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception{
        if(args.length != 5){
            System.out.println("Please specify server IP and port, and self port.");
            return;
        }

        InetAddress serverIP = InetAddress.getByName(args[0]);
        int serverPort = Integer.parseInt(args[1]);

        Scanner keyboard = new Scanner(System.in);
        String message = keyboard.nextLine();

        Frame frame = new Frame(args[3], args[4], message);

        int selfPort = Integer.parseInt(args[2]);
        DatagramSocket socket = new DatagramSocket(selfPort);

        DatagramPacket packet = frame.writePacket(serverIP, serverPort);

        socket.send(packet);

        DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
        socket.receive(reply);
        socket.close();

        byte[] serverMessage = Arrays.copyOf(
                reply.getData(),
                reply.getLength()
        );
        System.out.println(new String(serverMessage));
    }
}
