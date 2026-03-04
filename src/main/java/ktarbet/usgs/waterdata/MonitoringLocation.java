package ktarbet.usgs.waterdata;

import java.util.List;

public class MonitoringLocation {

    static MonitoringLocation findByNumber(List<MonitoringLocation> locations, String locationNumber) {
        return locations.stream()
                .filter(loc -> locationNumber.equals(loc.monitoringLocationNumber))
                .findFirst()
                .orElse(null);
    }

    public String x;
    public String y;
    public String id;
    public String agencyCode;
    public String agencyName;
    public String monitoringLocationNumber;
    public String monitoringLocationName;
    public String districtCode;
    public String countryCode;
    public String countryName;
    public String stateCode;
    public String stateName;
    public String countyCode;
    public String countyName;
    public String minorCivilDivisionCode;
    public String siteTypeCode;
    public String siteType;
    public String hydrologicUnitCode;
    public String basinCode;
    public String altitude;
    public String altitudeAccuracy;
    public String altitudeMethodCode;
    public String altitudeMethodName;
    public String verticalDatum;
    public String verticalDatumName;
    public String horizontalPositionalAccuracyCode;
    public String horizontalPositionalAccuracy;
    public String horizontalPositionMethodCode;
    public String horizontalPositionMethodName;
    public String originalHorizontalDatum;
    public String originalHorizontalDatumName;
    public String drainageArea;
    public String contributingDrainageArea;
    public String timeZoneAbbreviation;
    public String usesDaylightSavings;
    public String constructionDate;
    public String aquiferCode;
    public String nationalAquiferCode;
    public String aquiferTypeCode;
    public String wellConstructedDepth;
    public String holeConstructedDepth;
    public String depthSourceCode;

    static MonitoringLocation fromRow(DataTable table, int row) {
        MonitoringLocation loc = new MonitoringLocation();
        loc.x = table.get(row, "x");
        loc.y = table.get(row, "y");
        loc.id = table.get(row, "id");
        loc.agencyCode = table.get(row, "agency_code");
        loc.agencyName = table.get(row, "agency_name");
        loc.monitoringLocationNumber = table.get(row, "monitoring_location_number");
        loc.monitoringLocationName = table.get(row, "monitoring_location_name");
        loc.districtCode = table.get(row, "district_code");
        loc.countryCode = table.get(row, "country_code");
        loc.countryName = table.get(row, "country_name");
        loc.stateCode = table.get(row, "state_code");
        loc.stateName = table.get(row, "state_name");
        loc.countyCode = table.get(row, "county_code");
        loc.countyName = table.get(row, "county_name");
        loc.minorCivilDivisionCode = table.get(row, "minor_civil_division_code");
        loc.siteTypeCode = table.get(row, "site_type_code");
        loc.siteType = table.get(row, "site_type");
        loc.hydrologicUnitCode = table.get(row, "hydrologic_unit_code");
        loc.basinCode = table.get(row, "basin_code");
        loc.altitude = table.get(row, "altitude");
        loc.altitudeAccuracy = table.get(row, "altitude_accuracy");
        loc.altitudeMethodCode = table.get(row, "altitude_method_code");
        loc.altitudeMethodName = table.get(row, "altitude_method_name");
        loc.verticalDatum = table.get(row, "vertical_datum");
        loc.verticalDatumName = table.get(row, "vertical_datum_name");
        loc.horizontalPositionalAccuracyCode = table.get(row, "horizontal_positional_accuracy_code");
        loc.horizontalPositionalAccuracy = table.get(row, "horizontal_positional_accuracy");
        loc.horizontalPositionMethodCode = table.get(row, "horizontal_position_method_code");
        loc.horizontalPositionMethodName = table.get(row, "horizontal_position_method_name");
        loc.originalHorizontalDatum = table.get(row, "original_horizontal_datum");
        loc.originalHorizontalDatumName = table.get(row, "original_horizontal_datum_name");
        loc.drainageArea = table.get(row, "drainage_area");
        loc.contributingDrainageArea = table.get(row, "contributing_drainage_area");
        loc.timeZoneAbbreviation = table.get(row, "time_zone_abbreviation");
        loc.usesDaylightSavings = table.get(row, "uses_daylight_savings");
        loc.constructionDate = table.get(row, "construction_date");
        loc.aquiferCode = table.get(row, "aquifer_code");
        loc.nationalAquiferCode = table.get(row, "national_aquifer_code");
        loc.aquiferTypeCode = table.get(row, "aquifer_type_code");
        loc.wellConstructedDepth = table.get(row, "well_constructed_depth");
        loc.holeConstructedDepth = table.get(row, "hole_constructed_depth");
        loc.depthSourceCode = table.get(row, "depth_source_code");
        return loc;
    }
}
