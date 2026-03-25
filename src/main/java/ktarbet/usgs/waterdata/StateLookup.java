package ktarbet.usgs.waterdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class StateLookup {

    private StateLookup() {
        // static utility class, prevent instantiation
    }

    private static final Map<String, StateInfo> statesByAbbreviation = new HashMap<>();

    public static class StateInfo {
        public String state;
        public String stateFp;
        public String stateNs;
        public String stateName;

        StateInfo(String state, String stateFp, String stateNs, String stateName) {
            this.state = state;
            this.stateFp = stateFp;
            this.stateNs = stateNs;
            this.stateName = stateName;
        }

        @Override
        public String toString() {
            return state + " (" + stateFp + ") " + stateName;
        }
    }

    private static void init() {
        try (InputStream is = StateLookup.class.getResourceAsStream("/national_state2020.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length < 4) continue;
                StateInfo info = new StateInfo(parts[0], parts[1], parts[2], parts[3]);
                statesByAbbreviation.put(parts[0], info);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load state data", e);
        }
    }

    public static String getStateCode(String stateAbbreviation) {
        if (statesByAbbreviation.isEmpty()) {
            init();
        }
        StateInfo info = statesByAbbreviation.get(stateAbbreviation);
        return info != null ? info.stateFp : null;
    }

    public static StateInfo getStateInfo(String stateAbbreviation) {
        if (statesByAbbreviation.isEmpty()) {
            init();
        }
        return statesByAbbreviation.get(stateAbbreviation);
    }

    /**
     * Returns all StateInfo entries sorted by state name.
     */
    private static List<StateInfo> getSortedStates() {
        if (statesByAbbreviation.isEmpty()) {
            init();
        }
        List<StateInfo> sorted = new ArrayList<>(statesByAbbreviation.values());
        sorted.sort(Comparator.comparing(info -> info.stateName));
        return sorted;
    }

    /**
     * Returns state names sorted alphabetically (e.g., "Alabama", "Alaska", ...).
     */
    public static String[] getStateNames() {
        return getSortedStates().stream().map(info -> info.stateName).toArray(String[]::new);
    }

    /**
     * Returns FIPS state codes in the same order as {@link #getStateNames()}
     * (e.g., "01", "02", ...).
     */
    public static String[] getStateCodes() {
        return getSortedStates().stream().map(info -> info.stateFp).toArray(String[]::new);
    }

    /**
     * Returns two-letter state abbreviations in the same order as {@link #getStateNames()}
     * (e.g., "AL", "AK", ...).
     */
    public static String[] getStateAbbreviations() {
        return getSortedStates().stream().map(info -> info.state).toArray(String[]::new);
    }

}
