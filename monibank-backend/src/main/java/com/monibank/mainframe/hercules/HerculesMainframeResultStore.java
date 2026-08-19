package com.monibank.mainframe.hercules;

import com.monibank.mainframe.port.MainframeGateway;
import com.monibank.mainframe.port.MainframeLogSource;
import com.monibank.mainframe.port.MainframeResultStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class HerculesMainframeResultStore
        implements MainframeResultStore {

    private static final int RESULT_RECORD_LENGTH = 160;

    private final MainframeGateway mainframeGateway;
    private final MainframeLogSource mainframeLogSource;
    private final JobNameGenerator jobNameGenerator;
    private final MainframeResultJclFactory resultJclFactory;

    @Override
    public List<String> read(String datasetName) {

        String jobName =
                jobNameGenerator.next();

        String jcl =
                resultJclFactory.createRead(
                        jobName,
                        datasetName
                );

        mainframeGateway.submitJcl(jcl);

        List<String> jobLines =
                waitForResultJob(
                        jobName,
                        datasetName
                );

        return extractResultRecords(jobLines);
    }

    @Override
    public void delete(String datasetName) {

        String jobName =
                jobNameGenerator.next();

        String jcl =
                resultJclFactory.createDelete(
                        jobName,
                        datasetName
                );

        mainframeGateway.submitJcl(jcl);
    }

    private List<String> waitForResultJob(
            String jobName,
            String datasetName
    ) {

        for (int attempt = 0; attempt < 60; attempt++) {

            List<String> lines =
                    mainframeLogSource.readRecentLines(5000);

            List<String> block =
                    findResultJobBlock(
                            lines,
                            jobName,
                            datasetName
                    );

            if (block != null) {
                return block;
            }

            sleep();
        }

        throw new IllegalStateException(
                "Timeout waiting for result dataset "
                        + datasetName
        );
    }

    private List<String> findResultJobBlock(
            List<String> lines,
            String jobName,
            String datasetName
    ) {

        int datasetLine = -1;

        /*
         * Szukamy OD KOŃCA, ponieważ jobName może kiedyś
         * powtórzyć się po restarcie aplikacji.
         *
         * datasetName MBANK.RES.Rxxxxxxx jest unikalny
         * dla konkretnego requestu.
         */
        for (int i = lines.size() - 1; i >= 0; i--) {

            String line = lines.get(i);

            if (line.contains(datasetName)
                    && line.contains(jobName)) {

                datasetLine = i;
                break;
            }

            /*
             * W JCL listing może być datasetName,
             * ale jobName nie zawsze znajduje się
             * dokładnie w tej samej linii.
             */
            if (line.contains(datasetName)) {
                datasetLine = i;
                break;
            }
        }

        if (datasetLine < 0) {
            return null;
        }

        int start = -1;

        for (int i = datasetLine; i >= 0; i--) {

            String line = lines.get(i);

            if (line.contains("START  JOB")
                    && line.contains(jobName)) {

                start = i;
                break;
            }
        }

        if (start < 0) {
            return null;
        }

        int end = -1;

        for (int i = datasetLine; i < lines.size(); i++) {

            String line = lines.get(i);

            if (line.contains("END   JOB")
                    && line.contains(jobName)) {

                end = i;
                break;
            }
        }

        if (end < 0) {
            return null;
        }

        return new ArrayList<>(
                lines.subList(
                        start,
                        end + 1
                )
        );
    }

    private List<String> extractResultRecords(
            List<String> lines
    ) {

        List<String> result = new ArrayList<>();

        for (String line : lines) {

            if (line == null || line.isBlank()) {
                continue;
            }

            String normalized = line
                    .replace("\f", "")
                    .replace("\r", "")
                    .stripLeading();

            /*
             * Interesują nas WYŁĄCZNIE rekordy
             * protokołu Monibank.
             */
            if (!normalized.startsWith("MBR;")) {
                continue;
            }

            if (normalized.length() > RESULT_RECORD_LENGTH) {
                normalized = normalized.substring(
                        0,
                        RESULT_RECORD_LENGTH
                );
            }

            if (normalized.length() < RESULT_RECORD_LENGTH) {
                normalized =
                        normalized
                                + " ".repeat(
                                RESULT_RECORD_LENGTH
                                        - normalized.length()
                        );
            }

            result.add(normalized);
        }

        if (result.isEmpty()) {

            System.out.println(
                    "=== NO MONIBANK RESULT RECORDS FOUND ==="
            );

            for (String line : lines) {
                System.out.println(
                        "[" + line
                                .replace("\f", "<FF>")
                                .replace("\r", "<CR>")
                                .replace("\n", "<LF>")
                                + "]"
                );
            }

            throw new IllegalStateException(
                    "Mainframe result dataset contained "
                            + "no MBR result records"
            );
        }

        return result;
    }

    private void sleep() {

        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for mainframe result",
                    e
            );
        }
    }
}