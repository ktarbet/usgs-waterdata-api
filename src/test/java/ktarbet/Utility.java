package ktarbet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public final class Utility {

    private Utility() {
        // static utility class, prevent instantiation
    }

    public static final class ParameterCode {
        public static final String WATER_TEMPERATURE = "00010";
        public static final String AIR_TEMPERATURE = "00021";
        public static final String WIND_SPEED = "00035";
        public static final String WIND_DIRECTION = "00036";
        public static final String PRECIPITATION = "00045";
        public static final String RELATIVE_HUMIDITY = "00052";
        public static final String FLOW = "00060";
        public static final String STAGE = "00065";
        public static final String SPECIFIC_CONDUCTANCE = "00095";
        public static final String SALINITY = "00096";
        public static final String RESERVOIR_STORAGE = "72036";
        public static final String SOLAR_RADIATION = "62608";
        public static final String ELEVATION_NAVD88 = "63160";

        private ParameterCode() {
        }
    }

    public static final class StatisticCode {
        public static final String MAXIMUM = "00001";
        public static final String MINIMUM = "00002";
        public static final String MEAN = "00003";
        public static final String AM = "00004";
        public static final String PM = "00005";
        public static final String SUM = "00006";
        public static final String MODE = "00007";
        public static final String MEDIAN = "00008";
        public static final String STD = "00009";
        public static final String VARIANCE = "00010";
        public static final String INSTANTANEOUS = "00011";
        public static final String EQUIVALENT_MEAN = "00012";
        public static final String SKEWNESS = "00013";

        private StatisticCode() {
        }
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
        try (InputStream is = Utility.class.getResourceAsStream("/national_state2020.txt");
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


}
