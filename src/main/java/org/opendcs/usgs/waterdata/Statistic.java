package org.opendcs.usgs.waterdata;

public final class Statistic {
    public static final String MAXIMUM = "00001";
    public static final String MINIMUM = "00002";
    public static final String MEAN = "00003";
    public static final String AM = "00004";
    public static final String PM = "00005";
    public static final String SUM = "00006";
    public static final String MODE = "00007";
    public static final String MEDIAN = "00008";
    public static final String STD = "00009";
    public static final String VARIANCE = "00010";
    public static final String INSTANTANEOUS = "00011";
    public static final String EQUIVALENT_MEAN = "00012";
    public static final String SKEWNESS = "00013";

    /** Observation at 24:00, the most common statistic for daily reservoir storage. */
    public static final String OBSERVATION_AT_2400 = "32400";

    private Statistic() {
    }

    /**
     * True for "observation at a fixed time of day" statistics, which take the form
     * 3HHMM (32400 = 24:00, 30800 = 08:00, 31630 = 16:30). USGS omits these from its
     * statistic-codes collection, but reservoir storage relies on them heavily.
     */
    public static boolean isObservationAtTime(String statisticId) {
        if (statisticId == null || statisticId.length() != 5 || statisticId.charAt(0) != '3') {
            return false;
        }
        for (int i = 1; i < 5; i++) {
            if (!Character.isDigit(statisticId.charAt(i))) {
                return false;
            }
        }
        int hour = Integer.parseInt(statisticId.substring(1, 3));
        int minute = Integer.parseInt(statisticId.substring(3, 5));
        return hour >= 1 && hour <= 24 && minute <= 59;
    }

    /**
     * The time of day an observation-at-time statistic refers to, as HHMM (32400 -&gt; "2400"),
     * or null when the code is not one. Useful for labelling series that would otherwise be
     * indistinguishable, since they all report a computation identifier of "SelectedValue".
     */
    public static String observationTime(String statisticId) {
        if (!isObservationAtTime(statisticId)) {
            return null;
        }
        return statisticId.substring(1);
    }
}
