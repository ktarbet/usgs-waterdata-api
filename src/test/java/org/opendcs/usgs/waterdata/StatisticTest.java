package org.opendcs.usgs.waterdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StatisticTest {

    @ParameterizedTest(name = "[{index}] {0} is an observation time of {1}")
    @CsvSource({
            "32400, 2400",
            "30800, 0800",
            "30500, 0500",
            "31630, 1630",
            "30830, 0830",
    })
    void observationTimeCodes(String statisticId, String expected) {
        assertTrue(Statistic.isObservationAtTime(statisticId));
        assertEquals(expected, Statistic.observationTime(statisticId));
    }

    @ParameterizedTest(name = "[{index}] {0} is not an observation time")
    @ValueSource(strings = {"00003", "00001", "00011", "3", "32400x", "3aaaa", "30000", "30860"})
    void nonObservationTimeCodes(String statisticId) {
        assertFalse(Statistic.isObservationAtTime(statisticId));
        assertNull(Statistic.observationTime(statisticId));
    }

    @Test
    void nullIsNotAnObservationTime() {
        assertFalse(Statistic.isObservationAtTime(null));
        assertNull(Statistic.observationTime(null));
    }
}
