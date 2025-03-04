public class VirtualIP {
    private static final String defaultGatewayIPSuffix = "DG";

    private final String subnet, end;

    public static VirtualIP getDefaultGatewayForSubnet(String subnet) {
        return new VirtualIP(subnet, defaultGatewayIPSuffix);
    }

    public VirtualIP(String subnet, String end) {
        this.subnet = subnet;
        this.end = end;
    }

    public VirtualIP(String virtualIP) {
        int index = virtualIP.indexOf('.');
        if (index == -1) {
            throw new RuntimeException("Could not format " + virtualIP + " into a virtual IP address.");
        }
        this.subnet = virtualIP.substring(0, index);
        this.end = virtualIP.substring(index + 1);
    }

    public String getSubnet() {
        return this.subnet;
    }

    public String getEnd() {
        return this.end;
    }

    public boolean isInSameSubnet(VirtualIP other) {
        return other.getSubnet().equals(subnet);
    }

    public String toString() {
        return subnet + "." + end;
    }
}
