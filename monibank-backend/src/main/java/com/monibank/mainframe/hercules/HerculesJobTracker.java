package com.monibank.mainframe.hercules;

import com.monibank.mainframe.model.JobStatus;
import com.monibank.mainframe.model.MainframeJob;
import com.monibank.mainframe.port.MainframeLogSource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class HerculesJobTracker {

    private static final Pattern START_JOB_PATTERN =
            Pattern.compile(
                    "START\\s+JOB\\s+(\\d+)\\s+(\\S+)"
            );

    private final MainframeLogSource logSource;

    public Optional<MainframeJob> findJob(String jobName) {

        List<String> lines =
                logSource.readRecentLines(3000);

        String jobId = null;
        JobStatus status = JobStatus.UNKNOWN;

        for (String line : lines) {

            Matcher startMatcher =
                    START_JOB_PATTERN.matcher(line);

            if (startMatcher.find()) {

                String foundJobName =
                        startMatcher.group(2);

                if (foundJobName.equals(jobName)) {

                    int jobNumber =
                            Integer.parseInt(
                                    startMatcher.group(1)
                            );

                    jobId =
                            "JOB%05d".formatted(jobNumber);

                    status = JobStatus.SUBMITTED;
                }

                continue;
            }

            if (jobId == null || !line.contains(jobName)) {
                continue;
            }

            if (line.contains("$HASP373")
                    && line.contains("STARTED")) {

                status = JobStatus.STARTED;
            }

            if (line.contains("$HASP395")
                    && line.contains("ENDED")) {

                status = JobStatus.COMPLETED;
            }

            if (line.contains("$HASP250")
                    && line.contains("PURGED")) {

                status = JobStatus.PURGED;
            }

            if (line.contains("ABEND")) {
                status = JobStatus.FAILED;
            }
        }

        if (jobId == null) {
            return Optional.empty();
        }

        return Optional.of(
                new MainframeJob(
                        jobName,
                        jobId,
                        status
                )
        );
    }
}