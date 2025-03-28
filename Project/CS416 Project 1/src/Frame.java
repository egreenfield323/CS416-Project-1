import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Frame {
    public byte[] data;
    public VirtualIP destIp;
    public VirtualIP sourceIp;
    public String sourceMac;
    public String destMac;

    public Frame(String sMAC, String dMAC, VirtualIP sIP, VirtualIP dIP, byte[] data) {
        sourceMac = sMAC;
        destMac = dMAC;
        sourceIp = sIP;
        destIp = dIP;
        this.data = data;
    }

    public Frame(String sMAC, String dMAC, VirtualIP sIP, VirtualIP dIP) {
        sourceMac = sMAC;
        destMac = dMAC;
        sourceIp = sIP;
        destIp = dIP;
        this.data = new byte[0];
    }

    // Intended for frames that read from a packet.
    public Frame() {
        this("", "", new VirtualIP("", ""), new VirtualIP("", ""), new byte[0]);
    }

    public VirtualPort readPacket(DatagramPacket packet) {
        byte[] packet_data = packet.getData();
        ByteBuffer buffer = ByteBuffer.wrap(packet_data);

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
        sourceIp = new VirtualIP(new String(sourceIpBytes));

        int destIpLength = buffer.getInt();
        byte[] destIpBytes = new byte[destIpLength];
        buffer.get(destIpBytes);
        destIp = new VirtualIP(new String(destIpBytes));

        int dataLength = buffer.getInt();
        this.data = new byte[dataLength];
        buffer.get(this.data);

        // Returning the "port" the packet was received from. (Basically layer 1.)
        return new VirtualPort(packet.getAddress(), packet.getPort());
    }

    public DatagramPacket writePacket(VirtualPort addressTo) {
        // payload order is source MAC, destination MAC, source IP, destination IP,
        // message
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.putInt(sourceMac.getBytes().length);
        buffer.put(sourceMac.getBytes());
        buffer.putInt(destMac.getBytes().length);
        buffer.put(destMac.getBytes());
        buffer.putInt(sourceIp.toString().getBytes().length);
        buffer.put(sourceIp.toString().getBytes());
        buffer.putInt(destIp.toString().getBytes().length);
        buffer.put(destIp.toString().getBytes());
        buffer.putInt(this.data.length);
        buffer.put(this.data);

        byte[] payload = Arrays.copyOf(buffer.array(), buffer.position());
        System.out.println("Sending packet to virtual port: " + addressTo);
        return new DatagramPacket(payload, payload.length, addressTo.ip, addressTo.port);

    }

    public static void main(String[] args) throws UnknownHostException {

        Frame test = new Frame("7c:7e:eb:c5:e1:57", "97:f9:01:c2:8e:e7", new VirtualIP("net1.A"),
                new VirtualIP("net3.D"), new byte[0]);
        VirtualPort sentTo = new VirtualPort(InetAddress.getByName("127.0.0.1"), 3000);

        DatagramPacket packet = test.writePacket(sentTo);
        test = new Frame(); // Clearing it just so we know it actually loads data from packet.
        VirtualPort receivedFrom = test.readPacket(packet);

        System.out.printf(
                "Source Mac: %s, Destination Mac: %s, Source IP: %s, Destination IP: %s, Message:\n%s\nVirtual Port: %s\n",
                test.sourceMac, test.destMac, test.sourceIp, test.destIp, test.data, receivedFrom);
    }
}
