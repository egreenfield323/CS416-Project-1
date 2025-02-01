import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Frame {
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

    // Intended for frames that read from a packet.
    public Frame() {
        this("", "", "");
    }

    public void readPacket(DatagramPacket packet) {
        // returns array of strings in this order:
        // source mac, destination mac, message, destination ip, destination port
        destIp = packet.getAddress();
        destPort = packet.getPort();

        data = packet.getData();
        ByteBuffer buffer = ByteBuffer.wrap(data);

        int sourceMacLength = buffer.getInt();
        byte[] sourceMacBytes = new byte[sourceMacLength];
        buffer.get(sourceMacBytes);
        sourceMac = new String(sourceMacBytes);

        int destMacLength = buffer.getInt();
        byte[] destMacBytes = new byte[destMacLength];
        buffer.get(destMacBytes);
        destMac = new String(sourceMacBytes);

        int messageLength = buffer.getInt();
        byte[] messageBytes = new byte[messageLength];
        buffer.get(messageBytes);
        message = new String(messageBytes);
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

    public static void main(String[] args) throws UnknownHostException {

        Frame test = new Frame("7c:7e:eb:c5:e1:57", "97:f9:01:c2:8e:e7", "hello");

        DatagramPacket packet = test.writePacket(InetAddress.getByName("172.0.0.1"), 3000);
        test.readPacket(packet);

        System.out.printf(
                "Source: %s, Destination: %s, Message:\n%s\nVirtual Port: %s:%d\n",
                test.sourceMac, test.destMac, test.message, test.destIp, test.destPort
        );
    }
}
