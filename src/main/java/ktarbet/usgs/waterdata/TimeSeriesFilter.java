package ktarbet.usgs.waterdata;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fluent, chainable filter for {@link TimeSeriesMetadata} lists.
 *
 * <p>Usage:
 * <pre>
 * var results = TimeSeriesMetadata.filter(metadata)
 *     .parameterCode(Parameter.DISCHARGE)
 *     .statisticId(Statistic.MEAN)
 *     .toList();
 * </pre>
 */
public class TimeSeriesFilter {

    private final List<TimeSeriesMetadata> source;
    private Predicate<TimeSeriesMetadata> predicate = ts -> true;

    TimeSeriesFilter(List<TimeSeriesMetadata> source) {
        this.source = source;
    }

    private TimeSeriesFilter and(Predicate<TimeSeriesMetadata> p) {
        predicate = predicate.and(p);
        return this;
    }

    // --- Exact-match filters (varargs for single or multi-value) ---

    public TimeSeriesFilter parameterCode(String... codes) {
        return matchAny(codes, ts -> ts.parameterCode);
    }

    public TimeSeriesFilter statisticId(String... ids) {
        return matchAny(ids, ts -> ts.statisticId);
    }

    public TimeSeriesFilter computationPeriod(String... periods) {
        return matchAny(periods, ts -> ts.computationPeriodIdentifier);
    }

    public TimeSeriesFilter computation(String... identifiers) {
        return matchAny(identifiers, ts -> ts.computationIdentifier);
    }

    public TimeSeriesFilter sublocation(String... sublocations) {
        return matchAny(sublocations, ts -> ts.sublocationIdentifier);
    }

    public TimeSeriesFilter monitoringLocationId(String... ids) {
        return matchAny(ids, ts -> ts.monitoringLocationId);
    }

    public TimeSeriesFilter unitOfMeasure(String... units) {
        return matchAny(units, ts -> ts.unitOfMeasure);
    }

    public TimeSeriesFilter webDescriptionContains(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        String lower = text.toLowerCase();
        return and(ts -> ts.webDescription != null && ts.webDescription.toLowerCase().contains(lower));
    }

    public TimeSeriesFilter sublocationContains(String text) {
        if (text == null || text.isEmpty()) {
            return this;
        }
        String lower = text.toLowerCase();
        return and(ts -> ts.sublocationIdentifier != null && ts.sublocationIdentifier.toLowerCase().contains(lower));
    }

    /**
     * Keeps entries whose date range overlaps the given range.
     */
    public TimeSeriesFilter dateRange(LocalDate start, LocalDate end) {
        return and(ts -> ts.begin != null && ts.end != null
                && ts.begin.compareTo(end) <= 0 && ts.end.compareTo(start) >= 0);
    }

    /**
     * Keeps entries that have both begin and end dates set.
     */
    public TimeSeriesFilter hasDateRange() {
        return and(ts -> ts.begin != null && ts.end != null);
    }

    // --- Null/empty checks ---

    /**
     * Keeps entries where statisticId is not null or empty.
     */
    public TimeSeriesFilter hasStatistic() {
        return and(ts -> ts.statisticId != null && !ts.statisticId.isEmpty());
    }

    // --- Convenience composites ---

    /**
     * Filters to the most common Daily time series: computation period "Daily",
     * has a statistic, and computation is "Mean" or "Instantaneous".
     */
    public TimeSeriesFilter daily() {
        return computationPeriod("Daily")
                .hasStatistic()
                .computation("Mean", "Instantaneous");
    }

    // --- Escape hatch ---

    /**
     * Applies a custom predicate for filtering not covered by the built-in methods.
     */
    public TimeSeriesFilter where(Predicate<TimeSeriesMetadata> p) {
        return and(p);
    }

    // --- Terminal operations ---

    public List<TimeSeriesMetadata> toList() {
        return source.stream().filter(predicate).collect(Collectors.toList());
    }

    public Optional<TimeSeriesMetadata> findFirst() {
        return source.stream().filter(predicate).findFirst();
    }

    public Stream<TimeSeriesMetadata> stream() {
        return source.stream().filter(predicate);
    }

    public long count() {
        return source.stream().filter(predicate).count();
    }

    // --- Internal helpers ---

    @FunctionalInterface
    private interface FieldAccessor {
        String get(TimeSeriesMetadata ts);
    }

    private TimeSeriesFilter matchAny(String[] values, FieldAccessor accessor) {
        if (values == null || values.length == 0) {
            return this;
        }
        String[] cleaned = Arrays.stream(values)
                .filter(v -> v != null && !v.isEmpty())
                .toArray(String[]::new);
        if (cleaned.length == 0) {
            return this;
        }
        if (cleaned.length == 1) {
            String value = cleaned[0];
            return and(ts -> value.equals(accessor.get(ts)));
        }
        Set<String> set = new HashSet<>(Arrays.asList(cleaned));
        return and(ts -> set.contains(accessor.get(ts)));
    }
}
