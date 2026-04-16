package ktarbet;

import ktarbet.usgs.waterdata.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Demo {

    public static void main(String[] args) throws Exception {
        // read metadata for a location
        String location_id = "USGS-13213000";
        var metadata = UsgsWaterDataApi.getTimeSeriesMetadata(location_id);
        var PARI = TimeSeriesMetadata.filter(metadata)
                .parameterCode(Parameter.DISCHARGE).statisticId(Statistic.MEAN)
                .findFirst().orElseThrow();

        // Use TimeSeries to get metadata + data together
        System.out.println("Read Daily Mean Discharge, Boise River at Parma");
        TimeSeries<DailyValue> dailyTS = UsgsWaterDataApi.getDailyTimeSeries(PARI,
                "2026-01-01T00:00:00Z", "2026-01-05T00:00:00Z");

        System.out.println("Station: " + dailyTS.getMonitoringLocationId());
        System.out.println("Units: " + dailyTS.getUnitOfMeasure());
        System.out.println("Parameter: " + dailyTS.getParameterName());
        dailyTS.printToConsole(5);

        System.out.println("Read Continuous Water Temperature Data, Sacramento River at Freeport, CA");
        String location_id2 = "USGS-11447650";
        var metadata2 = UsgsWaterDataApi.getTimeSeriesMetadata(location_id2);
        var tempMetas = TimeSeriesMetadata.filter(metadata2)
                .parameterCode(Parameter.WATER_TEMPERATURE).statisticId(Statistic.INSTANTANEOUS)
                .toList();

        for (var tempMeta : tempMetas) {
            TimeSeries<InstantaneousValue> continuousTS = UsgsWaterDataApi.getContinuousTimeSeries(tempMeta,
                    "2026-01-15T00:00:00Z", "2026-01-16T23:59:00Z");

            System.out.println("Station: " + continuousTS.getMonitoringLocationId());
            System.out.println("Sublocation: " + tempMeta.sublocationIdentifier);
            System.out.println("Description: " + tempMeta.webDescription);
            System.out.println("Units: " + continuousTS.getUnitOfMeasure());
            System.out.println("Parameter: " + continuousTS.getParameterName());
            continuousTS.printToConsole(5);
            System.out.println();
        }

        // read a very specific time-series  (There are two temperature time-series at this location, so we have to be specific about which one we want)
        var eastFender = TimeSeriesMetadata.filter(metadata2)
           .parameterCode(Parameter.WATER_TEMPERATURE)
           .statisticId(Statistic.INSTANTANEOUS)
           .sublocation("BGC PROJECT")
           .webDescriptionContains("East Fender").findFirst().orElseThrow();

        System.out.println("Reading East Fender time-series... (current data)");
        String end = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        String start = Instant.now().minus(7, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS).toString();
        TimeSeries<InstantaneousValue> eastfenderTS = UsgsWaterDataApi.getContinuousTimeSeries(eastFender,
                    start, end);
        
        eastfenderTS.printToConsole(5);
    }
}