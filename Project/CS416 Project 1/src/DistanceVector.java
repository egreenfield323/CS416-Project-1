
import java.util.HashMap;

public class DistanceVector {
    private final HashMap<String, Integer> map;

    public DistanceVector() {
        this.map = new HashMap<>();
    }

    public void addEntry(String subnet, int distance) {
        map.put(subnet, distance);
    }

    public boolean updateEntries(DistanceVector neighbor) {
        boolean updatedFlag = false;

        for (String subnet : neighbor.map.keySet()) {
            int value = neighbor.map.get(subnet);
            if (!map.containsKey(subnet) || value + 1 < map.get(subnet)) {
                this.map.put(subnet, value + 1);
                updatedFlag = true;
            }

        }

        return updatedFlag;
    }
}