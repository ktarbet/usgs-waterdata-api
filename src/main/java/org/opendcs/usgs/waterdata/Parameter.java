package org.opendcs.usgs.waterdata;

import java.util.List;

public final class Parameter {
    public static final String WATER_TEMPERATURE = "00010";
    public static final String AIR_TEMPERATURE = "00021";
    public static final String WIND_SPEED = "00035";
    public static final String WIND_DIRECTION = "00036";
    public static final String PRECIPITATION = "00045";
    public static final String RELATIVE_HUMIDITY = "00052";
    public static final String RESERVOIR_STORAGE_AC_FT = "00054";
    public static final String DISCHARGE = "00060";
    /** Reservoir water-surface elevation above the local gage datum, which MonitoringLocation.altitude cannot supply. */
    public static final String ELEVATION_RESERVOIR_ABOVE_DATUM = "00062";
    public static final String STAGE = "00065";
    public static final String SPECIFIC_CONDUCTANCE = "00095";
    public static final String SALINITY = "00096";
    public static final String RESERVOIR_STORAGE = "72036";
    public static final String SOLAR_RADIATION = "62608";
    /** Lake or reservoir water-surface elevation above NGVD 1929. */
    public static final String ELEVATION_LAKE_NGVD29 = "62614";
    /** Lake or reservoir water-surface elevation above NAVD 1988. */
    public static final String ELEVATION_LAKE_NAVD88 = "62615";
    /** Stream water-surface elevation above NAVD 1988. */
    public static final String ELEVATION_NAVD88 = "63160";

    public static List<String> all() {
        return List.of(
                WATER_TEMPERATURE, AIR_TEMPERATURE, WIND_SPEED, WIND_DIRECTION,
                PRECIPITATION, RELATIVE_HUMIDITY, RESERVOIR_STORAGE_AC_FT,
                DISCHARGE, ELEVATION_RESERVOIR_ABOVE_DATUM, STAGE,
                SPECIFIC_CONDUCTANCE, SALINITY, RESERVOIR_STORAGE, SOLAR_RADIATION,
                ELEVATION_LAKE_NGVD29, ELEVATION_LAKE_NAVD88, ELEVATION_NAVD88);
    }

    private Parameter() {
    }
}
