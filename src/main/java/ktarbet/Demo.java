package ktarbet;

import ktarbet.usgs.waterdata.*;

public class Demo {

    public static void main(String[] args) throws Exception {
        // read metadata for a location
        String location_id = "USGS-13213000";
        var metadata = UsgsWaterDataApi.getTimeSeriesMetadata(location_id);
        var PARI = TimeSeriesMetadata.filter(metadata, Parameter.DISCHARGE, Statistic.MEAN).get(0) ;

        // Use TimeSeries to get metadata + data together
        System.out.println("Read Daily Mean Discharge, Boise River at Parma");
        TimeSeries<DailyValue> dailyTS = UsgsWaterDataApi.getDailyTimeSeries(PARI,
                "2026-01-01T00:00:00Z", "2026-01-05T00:00:00Z");

        System.out.println("Station: " + dailyTS.getMonitoringLocationId());
        System.out.println("Units: " + dailyTS.getUnitOfMeasure());
        System.out.println("Parameter: " + dailyTS.getParameterName());
        for (int i = 0; i < dailyTS.size() && i < 5; i++) {
            DailyValue dv = dailyTS.values.get(i);
            System.out.println("  " + dv.date + " = " + dv.value);
        }

        System.out.println("Read Continuous Water Temperature Data, Sacramento River at Freeport, CA");
        String location_id2 = "USGS-11447650";
        var metadata2 = UsgsWaterDataApi.getTimeSeriesMetadata(location_id2);
        var tempMetas = TimeSeriesMetadata.filter(metadata2, Parameter.WATER_TEMPERATURE, Statistic.INSTANTANEOUS);

        for (var tempMeta : tempMetas) {
            TimeSeries<InstantaneousValue> continuousTS = UsgsWaterDataApi.getContinuousTimeSeries(tempMeta,
                    "2026-01-15T00:00:00Z", "2026-01-16T23:59:00Z");

            System.out.println("Station: " + continuousTS.getMonitoringLocationId());
            System.out.println("Sublocation: " + tempMeta.sublocationIdentifier);
            System.out.println("Description: " + tempMeta.webDescription);
            System.out.println("Units: " + continuousTS.getUnitOfMeasure());
            System.out.println("Parameter: " + continuousTS.getParameterName());
            for (int i = 0; i < continuousTS.size() && i < 5; i++) {
                InstantaneousValue iv = continuousTS.values.get(i);
                System.out.println("  " + iv.time + " = " + iv.value);
            }
            System.out.println();
        }

    }
}