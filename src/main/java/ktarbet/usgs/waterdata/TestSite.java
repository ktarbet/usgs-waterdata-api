package ktarbet.usgs.waterdata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Fake USGS site 88888888 that generates deterministic data in memory.
 * No HTTP calls are made — the API methods detect the test site/series IDs
 * and delegate here.
 *
 * <p>The site provides four time series:</p>
 * <ul>
 *   <li>Daily discharge (param 00060, stat 00003) — value = day of month</li>
 *   <li>Continuous gage height (param 00065, Instantaneous) — value = hour of day</li>
 *   <li>Continuous water temperature, Left Bank (param 00010, Instantaneous) — value = hour of day</li>
 *   <li>Continuous water temperature, Right Bank (param 00010, Instantaneous) — value = hour of day</li>
 * </ul>
 */
public class TestSite
{
    public static final String SITE_NUMBER = "88888888";
    public static final String MONITORING_LOCATION_ID = "USGS-" + SITE_NUMBER;
    public static final String DAILY_DISCHARGE_TS_ID = "test-daily-discharge-" + SITE_NUMBER;
    public static final String CONTINUOUS_GAGE_TS_ID = "test-continuous-gage-" + SITE_NUMBER;
    public static final String CONTINUOUS_TEMP_LEFT_TS_ID = "test-continuous-temp-left-" + SITE_NUMBER;
    public static final String CONTINUOUS_TEMP_RIGHT_TS_ID = "test-continuous-temp-right-" + SITE_NUMBER;

    private TestSite() {}

    static boolean isTestSite(String monitoringLocationId)
    {
        return MONITORING_LOCATION_ID.equals(monitoringLocationId);
    }

    static boolean isTestSeriesId(String timeSeriesId)
    {
        return DAILY_DISCHARGE_TS_ID.equals(timeSeriesId)
            || CONTINUOUS_GAGE_TS_ID.equals(timeSeriesId)
            || CONTINUOUS_TEMP_LEFT_TS_ID.equals(timeSeriesId)
            || CONTINUOUS_TEMP_RIGHT_TS_ID.equals(timeSeriesId);
    }

    /**
     * Generate metadata for the test site.
     * Date range covers 2020-01-01 to 2030-01-01 so any reasonable query overlaps.
     */
    static List<TimeSeriesMetadata> generateMetadata()
    {
        List<TimeSeriesMetadata> list = new ArrayList<>();

        TimeSeriesMetadata daily = new TimeSeriesMetadata();
        daily.id = DAILY_DISCHARGE_TS_ID;
        daily.monitoringLocationId = MONITORING_LOCATION_ID;
        daily.parameterCode = Parameter.DISCHARGE;
        daily.parameterName = "Discharge";
        daily.statisticId = Statistic.MEAN;
        daily.unitOfMeasure = "ft^3/s";
        daily.computationPeriodIdentifier = "Daily";
        daily.computationIdentifier = "Mean";
        daily.begin = LocalDate.of(2020, 1, 1);
        daily.end = LocalDate.of(2030, 1, 1);
        daily.primary = "Primary";
        list.add(daily);

        TimeSeriesMetadata continuous = new TimeSeriesMetadata();
        continuous.id = CONTINUOUS_GAGE_TS_ID;
        continuous.monitoringLocationId = MONITORING_LOCATION_ID;
        continuous.parameterCode = Parameter.STAGE;
        continuous.parameterName = "Gage height";
        continuous.statisticId = null;
        continuous.unitOfMeasure = "ft";
        continuous.computationPeriodIdentifier = "Instantaneous";
        continuous.computationIdentifier = "Instantaneous";
        continuous.begin = LocalDate.of(2020, 1, 1);
        continuous.end = LocalDate.of(2030, 1, 1);
        continuous.primary = "Primary";
        list.add(continuous);

        TimeSeriesMetadata tempLeft = new TimeSeriesMetadata();
        tempLeft.id = CONTINUOUS_TEMP_LEFT_TS_ID;
        tempLeft.monitoringLocationId = MONITORING_LOCATION_ID;
        tempLeft.parameterCode = Parameter.WATER_TEMPERATURE;
        tempLeft.parameterName = "Temperature, water";
        tempLeft.statisticId = null;
        tempLeft.unitOfMeasure = "deg C";
        tempLeft.computationPeriodIdentifier = "Instantaneous";
        tempLeft.computationIdentifier = "Instantaneous";
        tempLeft.sublocationIdentifier = "Left Bank";
        tempLeft.begin = LocalDate.of(2020, 1, 1);
        tempLeft.end = LocalDate.of(2030, 1, 1);
        tempLeft.primary = "Primary";
        list.add(tempLeft);

        TimeSeriesMetadata tempRight = new TimeSeriesMetadata();
        tempRight.id = CONTINUOUS_TEMP_RIGHT_TS_ID;
        tempRight.monitoringLocationId = MONITORING_LOCATION_ID;
        tempRight.parameterCode = Parameter.WATER_TEMPERATURE;
        tempRight.parameterName = "Temperature, water";
        tempRight.statisticId = null;
        tempRight.unitOfMeasure = "deg C";
        tempRight.computationPeriodIdentifier = "Instantaneous";
        tempRight.computationIdentifier = "Instantaneous";
        tempRight.sublocationIdentifier = "Right Bank";
        tempRight.begin = LocalDate.of(2020, 1, 1);
        tempRight.end = LocalDate.of(2030, 1, 1);
        tempRight.primary = "Primary";
        list.add(tempRight);

        return list;
    }

    /** One value per day; value = day of month. */
    static List<DailyValue> generateDailyValues(String startDate, String endDate)
    {
        LocalDate start = LocalDate.parse(startDate.substring(0, 10));
        LocalDate end = LocalDate.parse(endDate.substring(0, 10));
        List<DailyValue> values = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1))
        {
            values.add(new DailyValue(d, d.getDayOfMonth()));
        }
        return values;
    }

    /** One value every 15 minutes; value = hour of day. */
    static List<InstantaneousValue> generateContinuousValues(String startInstant, String endInstant)
    {
        Instant start = Instant.parse(startInstant);
        Instant end = Instant.parse(endInstant);
        List<InstantaneousValue> values = new ArrayList<>();
        for (Instant t = start; t.isBefore(end); t = t.plus(15, ChronoUnit.MINUTES))
        {
            int hour = t.atZone(ZoneOffset.UTC).getHour();
            values.add(new InstantaneousValue(t, hour));
        }
        return values;
    }
}