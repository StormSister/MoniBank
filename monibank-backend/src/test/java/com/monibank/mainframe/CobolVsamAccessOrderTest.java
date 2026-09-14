package com.monibank.mainframe;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class CobolVsamAccessOrderTest {

    @Test
    void addCustomerUsesSequenceBeforeCustomerFile() throws IOException {
        assertOrder(
                "cobol/application/customers/ADDCUSG.cob",
                readForUpdate("SEQFILE"),
                rewrite("SEQFILE"),
                write("CUSTFILE")
        );
    }

    @Test
    void addAccountUsesSequenceBeforeCustomerAndAccountFiles()
            throws IOException {
        assertOrder(
                "cobol/application/account/ADDACCT.cob",
                readForUpdate("SEQFILE"),
                read("CUSTFILE"),
                rewrite("SEQFILE"),
                write("ACCTFILE")
        );
    }

    @Test
    void addCardUsesSequenceBeforeAccountAndCardFiles()
            throws IOException {
        assertOrder(
                "cobol/application/card/ADDCARD.cob",
                readForUpdate("SEQFILE"),
                read("ACCTFILE"),
                rewrite("SEQFILE"),
                write("CARDFILE")
        );
    }

    @Test
    void postTransactionLocksSequenceBeforeAccountAndTransactionFiles()
            throws IOException {
        assertOrder(
                "cobol/transactions/POSTTXN.cob",
                readForUpdate("SEQFILE"),
                readForUpdate("ACCTFILE"),
                rewrite("SEQFILE"),
                write("TXNFILE"),
                rewrite("ACCTFILE")
        );
    }

    private static void assertOrder(
            String resource,
            String... fragments
    ) throws IOException {
        String source = normalizedResource(resource);
        int previous = -1;

        for (String fragment : fragments) {
            int current = source.indexOf(fragment, previous + 1);
            assertThat(current)
                    .as("%s should contain %s after position %d",
                            resource, fragment, previous)
                    .isGreaterThan(previous);
            previous = current;
        }
    }

    private static String normalizedResource(String name) throws IOException {
        ClassLoader loader = CobolVsamAccessOrderTest.class.getClassLoader();

        try (InputStream input = loader.getResourceAsStream(name)) {
            assertThat(input)
                    .as("COBOL resource %s", name)
                    .isNotNull();

            return new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .toUpperCase(Locale.ROOT)
                    .replaceAll("\\s+", " ");
        }
    }

    private static String readForUpdate(String file) {
        return "EXEC CICS READ FILE('" + file + "')";
    }

    private static String read(String file) {
        return "EXEC CICS READ FILE('" + file + "')";
    }

    private static String rewrite(String file) {
        return "EXEC CICS REWRITE FILE('" + file + "')";
    }

    private static String write(String file) {
        return "EXEC CICS WRITE FILE('" + file + "')";
    }
}
