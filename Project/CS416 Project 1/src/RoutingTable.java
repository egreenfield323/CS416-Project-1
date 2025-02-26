import java.util.HashMap;

public class RoutingTable {
    public HashMap<String, VirtualIP> nextHopTable;
    public HashMap<String, String> portTable;

    public RoutingTable() {
        this.nextHopTable = new HashMap<>();
        this.portTable = new HashMap<>();
    }

    public String toString() {
        StringBuilder ret = new StringBuilder("");

        if (portTable != null) {
            ret.append("\nPort table: \n");
            portTable.forEach((key, value) -> ret.append("key: ").append(key).append(", value: ").append(value).append("\n"));
        }
        if (nextHopTable != null) {
            ret.append("\nNext hop table: \n");
            nextHopTable.forEach((key, value) -> ret.append("key: ").append(key).append(", value: ").append(value).append("\n"));
        }

        if (ret.toString().equals("")) {
            return "Given MAC has no table entries.";
        }

        return ret.toString();
    }
}

// Learn self IP, self MAC

// R1: Recieve packet from HA
//   From net1.A to net3.D
//   From HA to R1
// R1: Lookup net3 in hop table, get net2.R2
// R1: Lookup net2 in port table, get R2's mac (net2 neighbor)
// R1: Convert R2's mac to R2's physical port
// R1 sends packet to R2's physical port:
//   From net1.A to net3.D
//   From R1 to R2
// R2: Recieve packet from R1
// R2: Lookup net3 in hop table, get nothing, keep note
// R2: Lookup net3 in port table, get S2's mac (net3 neighbor)
// R2: Convert S2's mac to S2's physical port
// R2: Since nothing was in hop table, resolve net3.D to D's MAC (D)
// R2 sends packet to S2's physical port
//   From net1.A to net3.D
//   From R2 to HD
// S2: Gets packet, delivers to HD.

//

