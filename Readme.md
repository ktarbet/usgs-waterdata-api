# usgs-water-api

A Java library for retrieving hydrologic data (daily values, peaks, continous (~15-minute),  time-series metadata, monitoring locations) from the USGS Water Data API. 


```gradle
// gradle
implementation("org.opendcs:usgs-waterdata-api:0.3.*")
```


```java
// import org.opendcs.usgs.waterdata.*;
        // Optionally set your USGS API key (otherwise the USGS_WATER_API_KEY
        // environment variable is used). This takes priority over the env var.
        UsgsWaterDataApi.setApiKey("your-api-key-here");

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

        // Read annual peak flow and stage (one value per water year)
        System.out.println("\nRead Annual Peaks, Boise River near Featherville");
        var peakMetadata = UsgsWaterDataApi.getTimeSeriesMetadata("USGS-13186000");

        // Peaks metadata is marked with computationIdentifier "Max At Event Time"
        var flowMeta = TimeSeriesMetadata.filter(peakMetadata)
                .parameterCode(Parameter.DISCHARGE).computation("Max At Event Time")
                .findFirst().orElseThrow();
        var stageMeta = TimeSeriesMetadata.filter(peakMetadata)
                .parameterCode(Parameter.STAGE).computation("Max At Event Time")
                .findFirst().orElseThrow();

        TimeSeries<InstantaneousValue> peakFlow = UsgsWaterDataApi.getAnnualPeaks(flowMeta);
        TimeSeries<InstantaneousValue> peakStage = UsgsWaterDataApi.getAnnualPeaks(stageMeta);

        System.out.println("Peak " + peakFlow.getParameterName() + " (" + peakFlow.getUnitOfMeasure() + "):");
        peakFlow.printToConsole(5);
        System.out.println("Peak " + peakStage.getParameterName() + " (" + peakStage.getUnitOfMeasure() + "):");
        peakStage.printToConsole(5);

        // Some peaks carry qualifiers (e.g. BACKWATER, ESTIMATED, GHNOTASSCPKQ) needed to interpret the value
        System.out.println("Peak stage values that carry qualifiers:");
        peakStage.values.stream()
                .filter(v -> !v.qualifiers.isEmpty())
                .forEach(v -> System.out.println("  " + v));

        // Read the stage-discharge rating curve (raw RDB text)
        System.out.println("\nRead Rating Curve, Snake River near Moran, WY");
        String ratings = UsgsWaterDataApi.getRatings("USGS-13011000");
        ratings.lines().limit(20).forEach(System.out::println);

```output.txt
Read Daily Mean Discharge, Boise River at Parma
Station: USGS-13213000
Units: ft^3/s
Parameter: Discharge
  2026-01-01 = 884.0
  2026-01-02 = 960.0
  2026-01-03 = 945.0
  2026-01-04 = 959.0
  2026-01-05 = 942.0
Read Continuous Water Temperature Data, Sacramento River at Freeport, CA
Station: USGS-11447650
Sublocation: BGC PROJECT
Description: East Fender
Units: degC
Parameter: Temperature, water
  2026-01-15T00:00:00Z = 10.0
  2026-01-15T00:15:00Z = 10.0
  2026-01-15T00:30:00Z = 10.0
  2026-01-15T00:45:00Z = 10.0
  2026-01-15T01:00:00Z = 10.0

Station: USGS-11447650
Sublocation: West Fender
Description: West Fender
Units: degC
Parameter: Temperature, water

Station: USGS-11447650
Sublocation:
Description: Right Bank Pump Stand
Units: degC
Parameter: Temperature, water
  2026-01-15T00:00:00Z = 10.0
  2026-01-15T00:15:00Z = 10.0
  2026-01-15T00:30:00Z = 10.0
  2026-01-15T00:45:00Z = 10.0
  2026-01-15T01:00:00Z = 10.0

Reading East Fender time-series... (current data)
  2026-04-10T14:45:00Z = 19.0
  2026-04-10T15:00:00Z = 19.0
  2026-04-10T15:15:00Z = 19.0
  2026-04-10T15:30:00Z = 19.0
  2026-04-10T15:45:00Z = 19.0

Read Annual Peaks, Boise River near Featherville
Peak Discharge (ft^3/s):
  1945-05-05T00:00:00Z = 2930.0
  1946-04-27T00:00:00Z = 4210.0
  1947-05-09T00:00:00Z = 4300.0
  1948-05-29T00:00:00Z = 4750.0
  1949-05-17T00:00:00Z = 3880.0
Peak Gage height (ft):
  1945-05-05T00:00:00Z = 5.43
  1946-04-27T00:00:00Z = 6.66
  1947-05-09T00:00:00Z = 6.75
  1948-05-29T00:00:00Z = 6.87
  1949-05-17T00:00:00Z = 6.15
Peak stage values that carry qualifiers:
  1957-05-19T00:00:00Z = 6.93 [BACKWATER, GHNOTASSCPKQ]
  1957-06-06T00:00:00Z = 6.9 [NOTMAXGH]
  2008-05-20T00:00:00Z = 6.22 [NOTMAXGH]
  2008-05-21T00:00:00Z = 6.39 [GHNOTASSCPKQ]

Read Rating Curve, Snake River near Moran, WY
# //UNITED STATES GEOLOGICAL SURVEY       http://water.usgs.gov/
# //NATIONAL WATER INFORMATION SYSTEM     http://water.usgs.gov/data.html
# //DATA ARE PROVISIONAL AND SUBJECT TO CHANGE UNTIL PUBLISHED BY USGS
# //RETRIEVED: 2026-05-08 22:20:02
# //WARNING
# //WARNING The stage-discharge rating provided in this file should be
# //WARNING considered provisional and subject to change. Stage-discharge
# //WARNING ratings change over time as the channel features that control
# //WARNING the relation between stage and discharge vary. Users are
# //WARNING cautioned to consider carefully the applicability of this
# //WARNING rating before using it for decisions that concern personal or
# //WARNING public safety or operational consequences.
# //FILE TYPE="NWIS RATING" 
# //DATABASE NUMBER=01 DESCRIPTION=" Standard data base for this site."
# //STATION AGENCY="USGS " NUMBER="13011000       " TIME_ZONE="MST" DST_FLAG=Y
# //STATION NAME="SNAKE RIVER NEAR MORAN, WY"
# //LABEL="Discharge (ft^3/s)"
# //PARAMETER CODE="00060"
# //RATING SHIFTED="20260508222002 MST"
# //RATING ID="1.0" TYPE="STGQ" NAME="stage-discharge" AGING=????
```


## API Key

Set the `USGS_WATER_API_KEY` environment variable to increase rate limits. Without a key, requests are subject to lower anonymous throttling.

```bash
export USGS_WATER_API_KEY=your-key-here
```

Alternatively, set it from Java (this takes priority over the environment variable):

```java
UsgsWaterDataApi.setApiKey("your-api-key-here");
```

```bash
# Run unit tests
./gradlew :usgs-water-api:test
# Run integration tests (hits the live USGS API)
./gradlew :usgs-water-api:integrationTest
```

## Debugging

To save API responses to `~/usgs.waterdata/` for inspection, add the JVM flag:

```
-Dusgs.debug=true
```

Files are named `<collection>_<id>` after the request, e.g. `daily_4993ed5a....csv`, `time-series-metadata_USGS-13037500.csv`, or `ratings_13011000.rdb`. Duplicate filenames get an incrementing suffix (e.g. `daily_....csv`, `daily_...1.csv`).
