package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StateLookupTest {

    @Test
    void parameterCodeConstants_areJavaFriendly() {
        assertEquals("00065", Parameter.STAGE);
    }

    @Test
    void getStateCode_returnsCorrectFipsCode() {
        assertEquals("06", StateLookup.getStateCode("CA"));
        assertEquals("48", StateLookup.getStateCode("TX"));
        assertEquals("36", StateLookup.getStateCode("NY"));
    }

    @Test
    void getStateCode_returnsNullForUnknown() {
        assertNull(StateLookup.getStateCode("ZZ"));
    }

    @Test
    void getStateInfo_returnsFullInfo() {
        StateLookup.StateInfo info = StateLookup.getStateInfo("CA");
        assertNotNull(info);
        assertEquals("CA", info.state);
        assertEquals("06", info.stateFp);
        assertEquals("01779778", info.stateNs);
        assertEquals("California", info.stateName);
    }

}
