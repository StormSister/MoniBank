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

            /*
             * NIE używamy trim().
             *
             * Rekord D może zawierać fixed-width payload.
             * Trailing spaces są częścią rekordu mainframe.
             */
            String record =
                    rawRecord
                            .replace("\f", "")
                            .replace("\r", "")
                            .stripLeading();

            if (!record.startsWith(PREFIX)) {
                continue;
            }

            /*
             * Potrzebujemy tylko pierwszych kilku pól,
             * żeby rozpoznać typ.
             */
            String[] prefixParts =
                    record.split(
                            SEPARATOR,
                            5
                    );

            if (prefixParts.length < 4) {
                throw new IllegalArgumentException(
                        "Invalid MBR record: "
                                + record
                );
            }

            String type =
                    prefixParts[1].trim();

            switch (type) {

                case "S", "E" -> {

                    if (header != null) {
                        throw new IllegalStateException(
                                "Multiple result headers found"
                        );
                    }

                    header =
                            parseHeader(record);
                }

                case "D" ->
                        data.add(
                                parseDataRecord(record)
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
            String record
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

        String[] parts =
                record.split(
                        SEPARATOR,
                        -1
                );

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
            String record
    ) {

        /*
         * MBR;D;CUSTOMER;Rxxxxxxx;<fixed-width payload>
         *
         * Split robimy maksymalnie na 5 części.
         * Dzięki temu cały payload zostaje nienaruszony.
         */
        String[] parts =
                record.split(
                        SEPARATOR,
                        5
                );

        if (parts.length < 5) {
            throw new IllegalArgumentException(
                    "Invalid MBR data record: "
                            + record
            );
        }

        String entityType =
                parts[2].trim();

        /*
         * CELOWO bez trim().
         *
         * Dla TCP może mieć dokładnie 119 znaków.
         * Dla fallbacku może mieć dodatkowy padding
         * wynikający z LRECL=160.
         *
         * Parser konkretnej encji bierze swoją
         * właściwą długość rekordu.
         */
        String payload =
                parts[4];

        return new MainframeDataRecord(
                entityType,
                payload
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