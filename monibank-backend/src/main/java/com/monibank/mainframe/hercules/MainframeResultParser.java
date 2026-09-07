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

    /*
     * MBR data records contain a fixed-width X(119) business payload.
     * A complete 160-byte MBR record may additionally carry X(17)
     * protocol padding after that payload.
     */
    private static final int DATA_PAYLOAD_LENGTH = 119;

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
             * Do not trim the end of a record. Trailing spaces may
             * belong to its fixed-width business payload.
             *
             * stripLeading() removes only transport/ASA padding
             * placed before the MBR prefix.
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
             * Only the first fields are needed to identify the
             * record type. Limiting the split protects semicolons
             * that may occur inside the fixed-width payload.
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
         * MBR;D;CUSTOMER;Rxxxxxxx;<X(119) payload><X(17) filler>
         *
         * The split is limited to five elements, so the complete
         * fixed-width tail remains untouched even if the payload
         * itself contains a semicolon.
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

        String payloadWithPadding =
                parts[4];

        if (payloadWithPadding.length()
                < DATA_PAYLOAD_LENGTH) {
            throw new IllegalArgumentException(
                    "Invalid MBR data payload length: "
                            + payloadWithPadding.length()
                            + "; expected at least "
                            + DATA_PAYLOAD_LENGTH
            );
        }

        /*
         * Expose only the business payload to entity parsers.
         * The remaining characters belong to the MBR envelope.
         */
        String payload =
                payloadWithPadding.substring(
                        0,
                        DATA_PAYLOAD_LENGTH
                );

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
