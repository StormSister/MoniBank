package com.monibank.mainframe.operations;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public final class OperationQueryService {

    private final OperationJournalReader journalReader;

    public OperationQueryService(OperationJournalReader journalReader) {
        this.journalReader = journalReader;
    }

    public List<CoreOperationEvent> find(
            int hours,
            int limit,
            CoreOperationEvent.Core core,
            CoreOperationEvent.Status status,
            String operation
    ) {
        Instant from = Instant.now().minus(hours, ChronoUnit.HOURS);
        String operationFilter = operation == null
                ? null
                : operation.trim().toUpperCase(Locale.ROOT);

        return journalReader.findSince(from).stream()
                .filter(event -> core == null || event.core() == core)
                .filter(event -> status == null || event.status() == status)
                .filter(event -> operationFilter == null
                        || operationFilter.isBlank()
                        || event.operation().equals(operationFilter))
                .limit(limit)
                .toList();
    }

    public OperationSummaryResponse summarize(
            int hours,
            CoreOperationEvent.Core core
    ) {
        Instant to = Instant.now();
        Instant from = to.minus(hours, ChronoUnit.HOURS);
        List<CoreOperationEvent> events = journalReader.findSince(from)
                .stream()
                .filter(event -> core == null || event.core() == core)
                .toList();

        long total = events.size();
        long success = count(events, CoreOperationEvent.Status.SUCCESS);
        long businessErrors = count(
                events,
                CoreOperationEvent.Status.BUSINESS_ERROR
        );
        long technicalErrors = count(
                events,
                CoreOperationEvent.Status.TECHNICAL_ERROR
        );

        return new OperationSummaryResponse(
                from,
                to,
                total,
                success,
                businessErrors,
                technicalErrors,
                total == 0L ? 0.0 : roundOneDecimal(
                        success * 100.0 / total
                ),
                average(events, CoreOperationEvent::queueMs),
                average(events, CoreOperationEvent::durationMs),
                executorSummaries(events),
                operationBreakdowns(events)
        );
    }

    private static List<OperationSummaryResponse.ExecutorSummary>
    executorSummaries(List<CoreOperationEvent> events) {
        Map<String, List<CoreOperationEvent>> grouped = events.stream()
                .collect(Collectors.groupingBy(event ->
                        event.executorId() == null
                                ? "UNASSIGNED"
                                : event.executorId()
                ));

        return grouped.entrySet().stream()
                .map(entry -> new OperationSummaryResponse.ExecutorSummary(
                        entry.getKey(),
                        entry.getValue().size(),
                        count(
                                entry.getValue(),
                                CoreOperationEvent.Status.SUCCESS
                        ),
                        count(
                                entry.getValue(),
                                CoreOperationEvent.Status.BUSINESS_ERROR
                        ),
                        count(
                                entry.getValue(),
                                CoreOperationEvent.Status.TECHNICAL_ERROR
                        ),
                        average(
                                entry.getValue(),
                                CoreOperationEvent::durationMs
                        )
                ))
                .sorted(Comparator.comparing(
                        OperationSummaryResponse.ExecutorSummary::executorId
                ))
                .toList();
    }

    private static List<OperationSummaryResponse.OperationBreakdown>
    operationBreakdowns(List<CoreOperationEvent> events) {
        return events.stream()
                .collect(Collectors.groupingBy(
                        CoreOperationEvent::operation
                ))
                .entrySet().stream()
                .map(entry -> new OperationSummaryResponse.OperationBreakdown(
                        entry.getKey(),
                        entry.getValue().size(),
                        count(
                                entry.getValue(),
                                CoreOperationEvent.Status.SUCCESS
                        ),
                        count(
                                entry.getValue(),
                                CoreOperationEvent.Status.BUSINESS_ERROR
                        ),
                        count(
                                entry.getValue(),
                                CoreOperationEvent.Status.TECHNICAL_ERROR
                        )
                ))
                .sorted(Comparator.comparingLong(
                        OperationSummaryResponse.OperationBreakdown::totalCount
                ).reversed())
                .toList();
    }

    private static long count(
            List<CoreOperationEvent> events,
            CoreOperationEvent.Status status
    ) {
        return events.stream()
                .filter(event -> event.status() == status)
                .count();
    }

    private static long average(
            List<CoreOperationEvent> events,
            Function<CoreOperationEvent, Long> value
    ) {
        return Math.round(events.stream()
                .map(value)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0));
    }

    private static double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
