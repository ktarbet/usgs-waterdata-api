# usgs-water-api

A Java library for retrieving hydrologic data (daily values, peaks, continous (15-minute),  time-series metadata, monitoring locations) from the USGS Water Data API. 


```gradle
// gradle
implementation("io.github.ktarbet:usgs-waterdata-api:0.2.*")
```


```java
// import ktarbet.usgs.waterdata.*;
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
```


## API Key

Set the `USGS_WATER_API_KEY` environment variable to increase rate limits. Without a key, requests are subject to lower anonymous throttling.

```bash
export USGS_WATER_API_KEY=your-key-here
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

Files are named from the response `Content-Disposition` header. Duplicate filenames get an incrementing suffix (e.g. `daily.csv`, `daily1.csv`, `daily2.csv`).


# TODO

 - Allow user to specify what paramters they want such as 'Flow'
 - Use the /combined-metadata endpoint instead (For list of sites with daily data) and filter on data_type. You'll get one row per data collection rather than one row per site
 - keep querys below MAX size..
 - document -Djava.net.useSystemProxies=true
 - USGS to do: /ogcapi/v0/collections/peaks and /stac/v0/collections/ratings

Station Name=HYDER AK
Stream Name=SALMON R
Station ID=15008000
Version Name=USGS
Latitude=56.0259971857126
Longitude=-130.06687006967235
Elevation=286
Coord Datum=NAD27
PARAMETERS=ALL
PARAMETERS=Flow,Stage  (new feature)

 - put Site-types in config somewhere:
 - Internally use MonitoringLocation instead of legacy UsgsStation
 - get period of record from api (start with Daily)
 - get more site types.
 - when reading regular time series , check dates for ordering ()

gs-w_waterdata_support@usgs.gov


Reference and Examples



USGS 13037500 SNAKE RIVER NR HEISE ID

Parameter types
00060  Discharge cfs 1Day
00065  Gage Height ft

https://api.waterdata.usgs.gov/ogcapi/v0/collections/parameter-codes/items


monitoring locations

curl -X 'GET' \
  'https://api.waterdata.usgs.gov/ogcapi/v0/collections/monitoring-locations/items?f=csv&lang=en-US&limit=10000&skipGeometry=true&offset=0&agency_code=USGS&state_code=06&site_type_code=ST' \
  -H 'accept: application/geo+json'

https://api.waterdata.usgs.gov/ogcapi/v0/collections/site-types/items
Streams ST:
Canal: ST-Canal
Tidal Stream: ST-TS
Lake, Reservoir: LK
https://api.waterdata.usgs.gov/ogcapi/v0/collections/monitoring-locations/items?f=csv&lang=en-US&limit=10000&skipGeometry=false&offset=0&agency_code=USGS&state_code=06&site_type_code=ST



Time series metadata

https://api.waterdata.usgs.gov/ogcapi/v0/collections/time-series-metadata/items?limit=2000&state_name=Utah




daily data
old: https://waterservices.usgs.gov/nwis/dv?sites=10059500&startDT=2025-02-26&endDT=2026-02-28&format=rdb


https://api.waterdata.usgs.gov/ogcapi/v0/collections/daily/items?f=csv&lang=en-US&limit=1000&properties=time,value,unit_of_measure&skipGeometry=true&sortby=time&offset=0&monitoring_location_id=USGS-11463500&parameter_code=00060%2C00065&statistic_id=00003&time=2018-02-12T00%3A00%3A00Z%2F2018-03-18T12%3A31%3A12Z



 https://api.waterdata.usgs.gov/ogcapi/v0/collections/time-series-metadata/items?f=csv&lang=en-US&limit=10&skipGeometry=false&offset=0&monitoring_location_id=USGS-13037500



15 Minute data...

/collections/continuous/items



States

https://api.waterdata.usgs.gov/ogcapi/v0/collections/states/items?f=csv&lang=en-US&limit=10000&skipGeometry=false&offset=0



curl -X 'GET' \
  'https://api.waterdata.usgs.gov/ogcapi/v0/collections/time-series-metadata/items?f=csv&lang=en-US&limit=10&skipGeometry=true&offset=0&parameter_code=00060%2C00065&statistic_id=00030&state_name=Idaho' \
  -H 'accept: application/geo+json'
