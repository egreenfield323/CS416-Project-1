import java.util.ArrayList;
import java.util.HashMap;

public class DistanceVector {
    private final HashMap<String, Integer> map;

    public DistanceVector() {
        this.map = new HashMap<>();
    }

    public void addEntry(String subnet, int distance) {
        map.put(subnet, distance);
    }

    public int getEntry(String subnet) {
        return this.map.get(subnet);
    }

    public String[] updateEntries(DistanceVector neighbor) {
        ArrayList<String> updatedEntries = new ArrayList<>();

        for (String subnet : neighbor.map.keySet()) {
            int value = neighbor.map.get(subnet);
            if (!map.containsKey(subnet) || value + 1 < map.get(subnet)) {
                this.map.put(subnet, value + 1);
                updatedEntries.add(subnet);
            }
        }
        return updatedEntries.toArray(String[]::new);
    }
}