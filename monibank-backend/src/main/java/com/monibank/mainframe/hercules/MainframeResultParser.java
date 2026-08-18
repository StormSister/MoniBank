package com.monibank.mainframe.hercules;

import com.monibank.mainframe.model.MainframeDataRecord;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.model.MainframeResultHeader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MainframeResultParser {

    public MainframeResult parse(List<String> records) {

        if (records == null || records.isEmpty()) {
            throw new IllegalArgumentException(
                    "Mainframe result cannot be empty"
            );
        }

        MainframeResultHeader header = null;
        List<MainframeDataRecord> data = new ArrayList<>();

        for (String rawRecord : records) {

            if (rawRecord == null || rawRecord.isBlank()) {
                continue;
            }

            String record = rawRecord.trim();

            if (!record.startsWith("MBR|")) {
                continue;
            }

            String[] parts =
                    record.split("\\|", -1);

            if (parts.length < 3) {
                throw new IllegalArgumentException(
                        "Invalid MBR record: " + record
                );
            }

            String type = parts[1].trim();

            switch (type) {

                case "S", "E" -> {

                    if (header != null) {
                        throw new IllegalStateException(
                                "Multiple result headers found"
                        );
                    }

                    header = parseHeader(parts);
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

        String type =
                value(parts, 1);

        String operation =
                value(parts, 2);

        String entityId =
                value(parts, 3);

        String status =
                value(parts, 4);

        String code =
                value(parts, 5);

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

        String entityType =
                value(parts, 2);

        StringBuilder payload =
                new StringBuilder();

        for (int i = 3; i < parts.length; i++) {

            if (i > 3) {
                payload.append("|");
            }

            payload.append(parts[i].trim());
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