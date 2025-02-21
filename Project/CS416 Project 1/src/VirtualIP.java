public class VirtualIP {
    private String subnet, end;

    public VirtualIP(String subnet, String end) {
        this.subnet = subnet;
        this.end = end;
    }

    public VirtualIP(String virtualIP) {
        int index = virtualIP.indexOf('.');
        this.subnet = virtualIP.substring(0, index);
        this.end = virtualIP.substring(index + 1);
    }

    public String getSubnet() {
        return this.subnet;
    }

    public String getEnd() {
        return this.end;
    }

    public String toString() {
        return subnet + "." + end;
    }
}
