package com.monibank.mainframe;

import com.monibank.mainframe.hercules.MainframeResultParser;
import com.monibank.mainframe.model.MainframeResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainframeResultParserTest {

    private final MainframeResultParser parser =
            new MainframeResultParser();

    @Test
    void retainsRequestIdFromResultHeader() {
        MainframeResult result = parser.parse(
                List.of(
                        "MBR;S;ADDACCT ;R2099688;A000000000005;A;OK"
                )
        );

        assertEquals("R2099688", result.header().requestId());
        assertEquals("ADDACCT", result.header().operation());
        assertEquals("OK", result.header().code());
    }
}
