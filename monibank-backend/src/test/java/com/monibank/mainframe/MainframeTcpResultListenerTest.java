package com.monibank.mainframe;

import com.monibank.mainframe.config.MainframeProperties;
import com.monibank.mainframe.hercules.MainframeTcpResultListener;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainframeTcpResultListenerTest {

    @Test
    void correlatesMbsRecordsWithRequestIdJobName()
            throws Exception {

        MainframeTcpResultListener listener =
                new MainframeTcpResultListener(
                        properties()
                );

        listener.registerDailyReport("R1234567");

        listener.handleLine(
                "****Z  START  JOB  276  R1234567  READ CLOSE"
        );
        listener.handleLine(
                "MBS;H;20260907;EUR;CLOSED"
        );
        listener.handleLine(
                "MBS;T;20260907;EUR;000000003;000000002;"
                        + "+0000000001100.00;000000001;"
                        + "+0000000000600.00;000000000;"
                        + "+0000000000000.00"
        );
        listener.handleLine(
                "MBS;C;20260907;ALL;000000006;000000005;"
                        + "000000001;000000000"
        );
        listener.handleLine(
                "MBS;E;20260907;EUR;OK"
        );

        List<String> result =
                listener.awaitDailyReport(
                        "R1234567",
                        Duration.ofMillis(10)
                );

        assertEquals(4, result.size());
        assertEquals(
                "MBS;E;20260907;EUR;OK",
                result.getLast()
        );

        listener.unregisterDailyReport("R1234567");
    }

    private MainframeProperties properties() {

        return new MainframeProperties(
                "HERCULES",
                "localhost",
                3505,
                3270,
                8038,
                5001,
                "localhost",
                "root",
                "/tmp/mainframe.log",
                "HERC01",
                "SECRET",
                "localhost",
                "/tmp/printer.txt",
                "monibank-mainframe",
                "/opt/mvs-tk5/prt/prt00e.txt"
        );
    }
}
