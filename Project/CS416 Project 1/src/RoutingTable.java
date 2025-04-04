import java.util.HashMap;

public class RoutingTable {
    public static class RouterTableMissException extends RuntimeException {
        public RouterTableMissException(String message) {
            super(message);
        }
    }

    public final HashMap<String, VirtualIP> nextHopTable;
    public final HashMap<String, String> portTable;

    public RoutingTable() {
        this.nextHopTable = new HashMap<>();
        this.portTable = new HashMap<>();
    }

    public void addNextHopEntry(String subnet, VirtualIP nextHopRouter) {
        this.nextHopTable.put(subnet, nextHopRouter);
    }

    public void addPortEntry(String subnet, String neighborMAC) {
        this.portTable.put(subnet, neighborMAC);
    }

    // If there is no next hop, will return null.
    public VirtualIP getNextHop(String subnet) {
        return nextHopTable.get(subnet);
    }

    public String resolveNeighboringSubnetMac(String subnet) throws RouterTableMissException {
        if (!portTable.containsKey(subnet)) {
            throw new RouterTableMissException(
                    "Subnet " + subnet + " is not in the routing table, and no default gateway has "
                            + "been configured");
        }

        return portTable.get(subnet);
    }

    public String[] getDirectlyConnectedSubnets() {
        return portTable.keySet().toArray(new String[0]);
    }

    public DistanceVector getInitialDistanceVector() {
        DistanceVector dv = new DistanceVector();
        for (String subnet : this.portTable.keySet()) {
            dv.addEntry(subnet, 0);
        }
        return dv;
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder("");

        if (!portTable.isEmpty()) {
            ret.append("\nPort table: \n");
            portTable.forEach(
                    (key, value) -> ret.append("subnet: ").append(key).append(", port: ").append(value).append("\n"));
        }
        if (!nextHopTable.isEmpty()) {
            ret.append("\nNext hop table: \n");
            nextHopTable.forEach((key, value) -> ret.append("subnet: ").append(key).append(", next hop ip: ")
                    .append(value).append("\n"));
        }

        if (ret.toString().equals("")) {
            return "Empty routing table.";
        }

        return ret.toString();
    }
}

// Learn self IP, self MAC

// R1: Recieve packet from HA
// From net1.A to net3.D
// From HA to R1
// R1: Lookup net3 in hop table, get net2.R2
// R1: Lookup net2 in port table, get R2's mac (net2 neighbor)
// R1: Convert R2's mac to R2's physical port
// R1 sends packet to R2's physical port:
// From net1.A to net3.D
// From R1 to R2
// R2: Recieve packet from R1
// R2: Lookup net3 in hop table, get nothing, keep note
// R2: Lookup net3 in port table, get S2's mac (net3 neighbor)
// R2: Convert S2's mac to S2's physical port
// R2: Since nothing was in hop table, resolve net3.D to D's MAC (D)
// R2 sends packet to S2's physical port
// From net1.A to net3.D
// From R2 to HD
// S2: Gets packet, delivers to HD.

//
