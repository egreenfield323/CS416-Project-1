import java.net.InetAddress;

public class VirtualPort {
    public InetAddress ip;
    public int port;

    public VirtualPort(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String toString() {
        return ip.toString() + ":" + port;
    }

    public boolean equals(VirtualPort other)
    {
        return ip.equals(other.ip) && port == other.port;
    }
}
