import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Frame {

    public DatagramPacket packet;

    public byte[] data;
    public InetAddress destIp;
    public int destPort;
    public String sourceMac;
    public String destMac;
    public String message;

    public Frame(String sMAC, String dMAC, String msg) {
        sourceMac = sMAC;
        destMac = dMAC;
        message = msg;
    }

    public String[] readPacket(DatagramPacket packet) {
        // returns array of strings in this order:
        // source mac, destination mac, message, destination ip, destination port
        destIp = packet.getAddress();
        destPort = packet.getPort();

        data = packet.getData();
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int sMacLength = buffer.getInt();
        byte[] sMacBytes = new byte[sMacLength];
        buffer.get(sMacBytes);
        String sMac = new String(sMacBytes);
        sourceMac = sMac;

        int dMacLength = buffer.getInt();
        byte[] dMacBytes = new byte[dMacLength];
        buffer.get(dMacBytes);
        String dMac = new String(dMacBytes);
        destMac = dMac;

        int msgLength = buffer.getInt();
        byte[] msgBytes = new byte[msgLength];
        buffer.get(msgBytes);
        String msg = new String(msgBytes);
        message = msg;

        return new String[] { sourceMac, destMac, message, destIp.toString(), Integer.toString(destPort) };
    }

    public DatagramPacket writePacket(InetAddress dIP, int dPort) {
        // payload order is destination MAC, source MAC, message
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(sourceMac.getBytes().length);
        buffer.put(sourceMac.getBytes());
        buffer.putInt(destMac.getBytes().length);
        buffer.put(destMac.getBytes());
        buffer.putInt(message.getBytes().length);
        buffer.put(message.getBytes());

        byte[] payload = Arrays.copyOf(buffer.array(), buffer.position());
        return new DatagramPacket(payload, payload.length, dIP, dPort);
    }

    public static void main(String[] args) {

        Frame test = new Frame("7c:7e:eb:c5:e1:57", "97:f9:01:c2:8e:e7", "hello");

        DatagramPacket packet = test.writePacket(new InetSocketAddress(3000).getAddress(), 3000);
        test.readPacket(packet);

        for (String i : test.readPacket(packet)) {
            System.out.println(i);
        }
    }
}
