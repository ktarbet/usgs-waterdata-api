package ktarbet.usgs.waterdata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Retrieves annual peak streamflow and gage height from the legacy NWIS
 * peak-flow service (tab-delimited RDB format).
 *
 * @see <a href="https://nwis.waterdata.usgs.gov/nwis/peak">NWIS Peak Streamflow</a>
 */
class PeakFlowService {

    static final String LEGACY_PEAK_URL =
            "https://nwis.waterdata.usgs.gov/nwis/peak?format=rdb&date_format=MM%%2FDD%%2FYYYY&site_no=%s";
    static final String LEGACY_PEAK_URL_RANGE = LEGACY_PEAK_URL + "&begin_date=%s&end_date=%s";

    private static final DateTimeFormatter PEAK_DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private PeakFlowService() {}

    /**
     * Retrieves annual peak streamflow and gage height.
     *
     * <p>Returns two {@link TimeSeries}: one for peak discharge ({@code peak_va}) and
     * one for gage height ({@code gage_ht}). Both share the same timestamps derived
     * from {@code peak_dt} (and {@code peak_tm} when available).
     *
     * @param siteMetadata the full list of {@link TimeSeriesMetadata} for the site,
     *                     used to look up parameter names and units
     * @return list of TimeSeries (discharge and stage), one entry per water year
     * @throws Exception on network or parsing errors
     */
    static List<TimeSeries<InstantaneousValue>> getAnnualPeaks(
            List<TimeSeriesMetadata> siteMetadata) throws Exception {
        return getAnnualPeaks(siteMetadata, null, null);
    }

    /**
     * Retrieves annual peak streamflow and gage height within a date range.
     *
     * @param siteMetadata the full list of {@link TimeSeriesMetadata} for the site
     * @param startDate start of date range (yyyy-MM-dd), or null for no lower bound
     * @param endDate   end of date range (yyyy-MM-dd), or null for no upper bound
     * @return list of TimeSeries (discharge and stage), one entry per water year
     * @throws Exception on network or parsing errors
     */
    static List<TimeSeries<InstantaneousValue>> getAnnualPeaks(
            List<TimeSeriesMetadata> siteMetadata, String startDate, String endDate) throws Exception {
        if (siteMetadata.isEmpty()) return Collections.emptyList();

        String monLocId = siteMetadata.get(0).monitoringLocationId;
        String siteNo = monLocId.startsWith("USGS-") ? monLocId.substring(5) : monLocId;

        String url;
        if (startDate != null && endDate != null) {
            url = String.format(LEGACY_PEAK_URL_RANGE, siteNo, startDate, endDate);
        } else {
            url = String.format(LEGACY_PEAK_URL, siteNo);
        }
        String rdb = WebUtility.getPage(url);
        if (rdb == null || rdb.isBlank()) return Collections.emptyList();

        DataTable table = RdbFile.fromString(rdb);
        List<InstantaneousValue> flows = new ArrayList<>();
        List<InstantaneousValue> stages = new ArrayList<>();

        for (int row = 0; row < table.getRowCount(); row++) {
            Instant time = parsePeakTime(table, row);
            flows.add(new InstantaneousValue(time, table.getDouble(row, "peak_va", UsgsWaterDataApi.UNDEFINED_DOUBLE)));
            stages.add(new InstantaneousValue(time, table.getDouble(row, "gage_ht", UsgsWaterDataApi.UNDEFINED_DOUBLE)));
        }

        List<TimeSeries<InstantaneousValue>> result = new ArrayList<>();
        result.add(new TimeSeries<>(peakMetadata(siteMetadata, Parameter.DISCHARGE), flows));
        result.add(new TimeSeries<>(peakMetadata(siteMetadata, Parameter.STAGE), stages));
        return result;
    }

    static Instant parsePeakTime(DataTable table, int row) {
        String dateStr = table.get(row, "peak_dt");
        LocalDate date = LocalDate.parse(dateStr, PEAK_DATE_FMT);
        String timeStr = table.get(row, "peak_tm");
        if (timeStr != null && !timeStr.isBlank()) {
            try {
                LocalTime lt = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
                return date.atTime(lt).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                // fall through to date-only
            }
        }
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static TimeSeriesMetadata peakMetadata(List<TimeSeriesMetadata> siteMetadata, String parameterCode) {
        TimeSeriesMetadata pm = siteMetadata.stream()
                .filter(ts -> parameterCode.equals(ts.parameterCode))
                .findFirst()
                .map(source -> {
                    TimeSeriesMetadata m = new TimeSeriesMetadata();
                    m.monitoringLocationId = source.monitoringLocationId;
                    m.parameterCode = source.parameterCode;
                    m.parameterName = source.parameterName;
                    m.unitOfMeasure = source.unitOfMeasure;
                    return m;
                })
                .orElseGet(() -> {
                    TimeSeriesMetadata m = new TimeSeriesMetadata();
                    m.monitoringLocationId = siteMetadata.get(0).monitoringLocationId;
                    m.parameterCode = parameterCode;
                    return m;
                });
        pm.computationPeriodIdentifier = "Water Year";
        pm.computationIdentifier = "Max At Event Time";
        return pm;
    }
}
