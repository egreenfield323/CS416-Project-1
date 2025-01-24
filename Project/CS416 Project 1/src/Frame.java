import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Frame {

    public DatagramPacket packet;

    public Frame(String sMAC, String dMAC, String message, InetAddress dIP, int dPort) {

//      payload order is destination MAC, source MAC, message
        String content = new String(dMAC.getBytes() + "|" + sMAC.getBytes() + "|" + message.getBytes());

        byte[] payload = content.getBytes();
        packet = new DatagramPacket(payload, payload.length, dIP, dPort);
    }
}