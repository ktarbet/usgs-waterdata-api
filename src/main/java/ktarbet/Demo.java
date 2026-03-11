package ktarbet;

import ktarbet.usgs.waterdata.*;

public class Demo {

    public static void main(String[] args) throws Exception {
        // read metadata for a location
        String location_id = "USGS-13213000";
        var metadata = UsgsWaterDataApi.getTimeSeriesMetadata(location_id);
        var PARI = TimeSeriesMetadata.filter(metadata, Parameter.DISCHARGE, Statistic.MEAN).get(0) ;

        System.out.println("Station: " + PARI.monitoringLocationId);
        System.out.println("Units: " + PARI.unitOfMeasure);

        System.out.println("Read Daily Mean Discharge, Boise River at Parma");
        
        var dailyTimeSeries = UsgsWaterDataApi.getDailyTimeSeries(location_id,
                Parameter.DISCHARGE, Statistic.MEAN,
                "2026-01-01T00:00:00Z", "2026-01-05T00:00:00Z");

        for (int i = 0; i < dailyTimeSeries.size() && i < 5; i++) {
            DailyValue dv = dailyTimeSeries.get(i);
            System.out.println("  " + dv.date + " = " + dv.value);
        }

        System.out.println("Read Continuous Stage Data, Cypress Creek at Grant Rd nr Cypress, TX");
        String location_id2 = "USGS-08068800";
        var continuousTimeSeries = UsgsWaterDataApi.getContinuousTimeSeries(
                location_id2, Parameter.STAGE, Statistic.INSTANTANEOUS,
                "2026-01-15T00:00:00Z", "2026-01-16T23:59:00Z");

        for (int i = 0; i < continuousTimeSeries.size() && i < 5; i++) {
            InstantaneousValue iv = continuousTimeSeries.get(i);
            System.out.println("  " + iv.time + " = " + iv.value);
        }

    }
}