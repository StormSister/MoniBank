package com.monibank.mainframe.hercules;

import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.model.MainframeResultHeader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MainframeResultParser {

    private static final String PREFIX = "MBR;";
    private static final String SEPARATOR = ";";

    public MainframeResult parse(List<String> records) {

        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException(
                    "Mainframe result cannot be empty"
            );
        }

        MainframeResultHeader header = null;
        List<MainframeDataRecord> data =
                new ArrayList<>();

        for (String rawRecord : records) {

            if (rawRecord == null
                    || rawRecord.isBlank()) {
                continue;
            }

            String record =
                    rawRecord.trim();

            if (!record.startsWith(PREFIX)) {
                continue;
            }

            String[] parts =
                    record.split(SEPARATOR, -1);

            if (parts.length < 4) {
                throw new IllegalArgumentException(
                        "Invalid MBR record: "
                                + record
                );
            }

            String type =
                    value(parts, 1);

            switch (type) {

                case "S", "E" -> {

                    if (header != null) {
                        throw new IllegalStateException(
                                "Multiple result headers found"
                        );
                    }

                    header =
                            parseHeader(parts);
                }

                case "D" ->
                        data.add(
                                parseDataRecord(parts)
                        );

                default ->
                        throw new IllegalArgumentException(
                                "Unknown MBR record type: "
                                        + type
                        );
            }
        }

        if (header == null) {
            throw new IllegalStateException(
                    "Mainframe result header not found"
            );
        }

        return new MainframeResult(
                header,
                List.copyOf(data)
        );
    }

    private MainframeResultHeader parseHeader(
            String[] parts
    ) {

        /*
         * MBR;S;CHGCUST;Rxxxxxxx;CUSTOMER;STATUS;CODE
         *
         * 0 MBR
         * 1 type
         * 2 operation
         * 3 requestId
         * 4 entityId
         * 5 status
         * 6 code
         */

        String type =
                value(parts, 1);

        String operation =
                value(parts, 2);

        String entityId =
                value(parts, 4);

        String status =
                value(parts, 5);

        String code =
                value(parts, 6);

        return new MainframeResultHeader(
                type,
                operation,
                code,
                entityId,
                status
        );
    }

    private MainframeDataRecord parseDataRecord(
            String[] parts
    ) {

        /*
         * MBR;D;CUSTOMER;Rxxxxxxx;<payload...>
         *
         * requestId z indeksu 3 pomijamy,
         * ponieważ służy listenerowi do routingu.
         */

        String entityType =
                value(parts, 2);

        StringBuilder payload =
                new StringBuilder();

        for (int i = 4;
             i < parts.length;
             i++) {

            if (i > 4) {
                payload.append(SEPARATOR);
            }

            payload.append(
                    parts[i].trim()
            );
        }

        return new MainframeDataRecord(
                entityType,
                payload.toString()
        );
    }

    private String value(
            String[] parts,
            int index
    ) {

        if (index >= parts.length) {
            return "";
        }

        return parts[index].trim();
    }
}