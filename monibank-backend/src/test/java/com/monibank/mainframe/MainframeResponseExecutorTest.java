package com.monibank.mainframe;

import com.monibank.mainframe.hercules.MainframeResponseExecutor;
import com.monibank.mainframe.hercules.MainframeResultParser;
import com.monibank.mainframe.hercules.MainframeTcpResultListener;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.model.MainframeResultHeader;
import com.monibank.mainframe.port.MainframeResultStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MainframeResponseExecutorTest {

    private static final String REQUEST_ID = "R1234567";
    private static final String OPERATION = "ADDCUST";

    @Test
    void acceptsCorrelatedResultAfterTerminalFailure() throws Exception {
        MainframeResultStore resultStore = mock(MainframeResultStore.class);
        MainframeResultParser parser = mock(MainframeResultParser.class);
        MainframeTcpResultListener listener =
                mock(MainframeTcpResultListener.class);
        MainframeResponseExecutor executor = new MainframeResponseExecutor(
                resultStore,
                parser,
                listener
        );
        List<String> records = List.of(
                "MBR;S;ADDCUST;R1234567;C000000000015;A;OK"
        );
        MainframeResult expected = new MainframeResult(
                new MainframeResultHeader(
                        "S",
                        OPERATION,
                        REQUEST_ID,
                        "OK",
                        "C000000000015",
                        "A"
                ),
                List.of()
        );

        when(listener.await(REQUEST_ID, Duration.ofSeconds(5)))
                .thenReturn(records);
        when(parser.parse(records)).thenReturn(expected);

        MainframeResult actual = executor.execute(
                REQUEST_ID,
                OPERATION,
                null,
                () -> {
                    throw new IllegalStateException(
                            "Expected screen did not appear."
                    );
                }
        );

        assertSame(expected, actual);
        verify(listener).register(REQUEST_ID);
        verify(listener).unregister(REQUEST_ID);
    }

    @Test
    void preservesTerminalFailureWhenNoResultArrives() throws Exception {
        MainframeResultStore resultStore = mock(MainframeResultStore.class);
        MainframeResultParser parser = mock(MainframeResultParser.class);
        MainframeTcpResultListener listener =
                mock(MainframeTcpResultListener.class);
        MainframeResponseExecutor executor = new MainframeResponseExecutor(
                resultStore,
                parser,
                listener
        );
        IllegalStateException terminalFailure =
                new IllegalStateException(
                        "Expected screen did not appear."
                );

        when(listener.await(REQUEST_ID, Duration.ofSeconds(5)))
                .thenThrow(new TimeoutException("No MBR result."));

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> executor.execute(
                        REQUEST_ID,
                        OPERATION,
                        null,
                        () -> {
                            throw terminalFailure;
                        }
                )
        );

        assertSame(terminalFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        verify(listener).unregister(REQUEST_ID);
    }
}
