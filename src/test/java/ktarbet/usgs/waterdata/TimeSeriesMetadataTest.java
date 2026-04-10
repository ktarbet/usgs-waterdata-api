package ktarbet.usgs.waterdata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

public class TimeSeriesMetadataTest {
    
    @Test
    public void testFilter() throws Exception {

        CsvFile csv =new CsvFile("src/test/resources/time-series-metadata_USGS-13037500.csv");
        
        List<TimeSeriesMetadata> metadataList = csv.mapRows(TimeSeriesMetadata::fromRow);

        var filtered = TimeSeriesMetadata.filter(metadataList)
                .parameterCode(Parameter.DISCHARGE).statisticId(Statistic.MEAN).toList();
        assertEquals(1, filtered.size());
        assertEquals(Parameter.DISCHARGE, filtered.get(0).parameterCode);
        assertEquals(Statistic.MEAN, filtered.get(0).statisticId);


    }
}
