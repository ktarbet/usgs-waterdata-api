package ktarbet.usgs.waterdata;

import java.util.List;

public final class Parameter {
    public static final String WATER_TEMPERATURE = "00010";
    public static final String AIR_TEMPERATURE = "00021";
    public static final String WIND_SPEED = "00035";
    public static final String WIND_DIRECTION = "00036";
    public static final String PRECIPITATION = "00045";
    public static final String RELATIVE_HUMIDITY = "00052";
    public static final String DISCHARGE = "00060";
    public static final String STAGE = "00065";
    public static final String SPECIFIC_CONDUCTANCE = "00095";
    public static final String SALINITY = "00096";
    public static final String RESERVOIR_STORAGE = "72036";
    public static final String SOLAR_RADIATION = "62608";
    public static final String ELEVATION_NAVD88 = "63160";

    public static List<String> all() {
        return List.of(
                WATER_TEMPERATURE, AIR_TEMPERATURE, WIND_SPEED, WIND_DIRECTION,
                PRECIPITATION, RELATIVE_HUMIDITY, DISCHARGE, STAGE,
                SPECIFIC_CONDUCTANCE, SALINITY, RESERVOIR_STORAGE,
                SOLAR_RADIATION, ELEVATION_NAVD88);
    }

    private Parameter() {
    }
}
