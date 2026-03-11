package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UtilityTest {

    @Test
    void parameterCodeConstants_areJavaFriendly() {
        assertEquals("00065", Parameter.STAGE);
    }

    @Test
    void getStateCode_returnsCorrectFipsCode() {
        assertEquals("06", Utility.getStateCode("CA"));
        assertEquals("48", Utility.getStateCode("TX"));
        assertEquals("36", Utility.getStateCode("NY"));
    }

    @Test
    void getStateCode_returnsNullForUnknown() {
        assertNull(Utility.getStateCode("ZZ"));
    }

    @Test
    void getStateInfo_returnsFullInfo() {
        Utility.StateInfo info = Utility.getStateInfo("CA");
        assertNotNull(info);
        assertEquals("CA", info.state);
        assertEquals("06", info.stateFp);
        assertEquals("01779778", info.stateNs);
        assertEquals("California", info.stateName);
    }

}
