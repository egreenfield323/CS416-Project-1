import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Frame {
    public byte[] data;
    public InetAddress destIp;
    public InetAddress sourceIp;
    public int destPort;
    public String sourceMac;
    public String destMac;
    public String message;

    public Frame(String sMAC, String dMAC, InetAddress sIP, InetAddress dIP, String msg) {
        sourceMac = sMAC;
        destMac = dMAC;
        sourceIp = sIP;
        destIp = dIP;
        message = msg;
    }

    // Intended for frames that read from a packet.
    public Frame() throws UnknownHostException {
        this("", "", InetAddress.getByName(""), InetAddress.getByName(""), "");
    }

    public void readPacket(DatagramPacket packet) throws UnknownHostException {
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
        destMac = new String(destMacBytes);

        int sourceIpLength = buffer.getInt();
        byte[] sourceIpBytes = new byte[sourceIpLength];
        buffer.get(sourceIpBytes);
        sourceIp = InetAddress.getByName(new String(sourceIpBytes));

        int destIpLength = buffer.getInt();
        byte[] destIpBytes = new byte[destIpLength];
        buffer.get(destIpBytes);
        destIp = InetAddress.getByName(new String(destIpBytes));

        int messageLength = buffer.getInt();
        byte[] messageBytes = new byte[messageLength];
        buffer.get(messageBytes);
        message = new String(messageBytes);
    }

    public DatagramPacket writePacket(InetAddress dIP, int dPort) {
        // payload order is source MAC, destination MAC, source IP, destination IP, message
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(sourceMac.getBytes().length);
        buffer.put(sourceMac.getBytes());
        buffer.putInt(destMac.getBytes().length);
        buffer.put(destMac.getBytes());
        buffer.putInt(sourceIp.toString().getBytes().length);
        buffer.put(sourceIp.toString().getBytes());
        buffer.putInt(destIp.toString().getBytes().length);
        buffer.put(destIp.toString().getBytes());
        buffer.putInt(message.getBytes().length);
        buffer.put(message.getBytes());

        byte[] payload = Arrays.copyOf(buffer.array(), buffer.position());
        return new DatagramPacket(payload, payload.length, dIP, dPort);
    }

    public static void main(String[] args) throws UnknownHostException {

        Frame test = new Frame("7c:7e:eb:c5:e1:57", "97:f9:01:c2:8e:e7", InetAddress.getByName("123.12.123.23"), InetAddress.getByName("192.168.0.225"), "hello");

        DatagramPacket packet = test.writePacket(test.destIp, 3000);
        test.readPacket(packet);

        System.out.printf(
                "Source Mac: %s, Destination Mac: %s, Source IP: %s, Destination IP: %s, Message:\n%s\nVirtual Port: %s:%d\n",
                test.sourceMac, test.destMac, test.sourceIp, test.destIp, test.message, test.destPort
        );
    }
}
