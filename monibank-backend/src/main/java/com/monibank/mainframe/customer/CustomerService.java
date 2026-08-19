package com.monibank.mainframe.customer;

import com.monibank.mainframe.customer.api.CreateCustomerRequest;
import com.monibank.mainframe.customer.api.CustomerResponse;
import com.monibank.mainframe.customer.mainframe.CustomerMainframeOperations;
import com.monibank.mainframe.customer.mainframe.CustomerRecordMapper;
import com.monibank.mainframe.customer.mainframe.CustomerRecordParser;
import com.monibank.mainframe.hercules.*;
import com.monibank.mainframe.model.JobStatus;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.port.MainframeGateway;
import com.monibank.mainframe.port.MainframeLogSource;
import com.monibank.mainframe.port.MainframeResultStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRecordMapper customerRecordMapper;
    private final MainframeBusinessJclFactory businessJclFactory;
    private final MainframeGateway mainframeGateway;
    private final JobNameGenerator jobNameGenerator;
    private final MainframeLogSource mainframeLogSource;
    private final CustomerRecordParser customerRecordParser;
    private final HerculesJobTracker jobTracker;
    private final MainframeRequestIdGenerator requestIdGenerator;
    private final MainframeResultStore mainframeResultStore;
    private final MainframeResultParser mainframeResultParser;
    private final MainframeTcpResultListener tcpResultListener;

    public String createCustomer(CreateCustomerRequest request) {

        String record =
                customerRecordMapper.toRecord(request);

        String jobName =
                jobNameGenerator.next();

        String jcl =
                businessJclFactory.create(
                        jobName,
                        CustomerMainframeOperations.ADD_CUSTOMER,
                        record
                );

        mainframeGateway.submitJcl(jcl);

        return jobName;
    }

    public List<CustomerResponse> getCustomers() {

        String jobName = jobNameGenerator.next();

        String jcl =
                businessJclFactory.createReadAll(
                        jobName,
                        CustomerMainframeOperations.LIST_CUSTOMERS
                );

        mainframeGateway.submitJcl(jcl);

        waitForJob(jobName);

        List<String> lines =
                waitForPrintedJob(jobName);

        return lines.stream()
                .map(this::extractCustomerRecord)
                .filter(java.util.Objects::nonNull)
                .map(customerRecordParser::parse)
                .distinct()
                .toList();
    }

    private void waitForJob(String jobName) {

        for (int i = 0; i < 40; i++) {

            var job =
                    jobTracker.findJob(jobName);

            if (job.isPresent()) {

                JobStatus status =
                        job.get().status();

                if (status == JobStatus.COMPLETED
                        || status == JobStatus.PURGED) {
                    return;
                }

                if (status == JobStatus.FAILED) {
                    throw new IllegalStateException(
                            "Mainframe job failed: "
                                    + jobName
                    );
                }
            }

            sleep();
        }

        throw new IllegalStateException(
                "Timeout waiting for mainframe job: "
                        + jobName
        );
    }

    private List<String> waitForPrintedJob(
            String jobName
    ) {

        for (int i = 0; i < 40; i++) {

            List<String> lines =
                    mainframeLogSource
                            .readRecentLines(3000);

            boolean hasStart =
                    lines.stream()
                            .anyMatch(line ->
                                    line.contains("START  JOB")
                                            && line.contains(jobName)
                            );

            boolean hasEnd =
                    lines.stream()
                            .anyMatch(line ->
                                    line.contains("END   JOB")
                                            && line.contains(jobName)
                            );

            if (hasStart && hasEnd) {
                return lines;
            }

            sleep();
        }

        throw new IllegalStateException(
                "Timeout waiting for printed mainframe job: "
                        + jobName
        );
    }

    private List<CustomerResponse> extractCustomers(
            List<String> lines,
            String jobName
    ) {

        boolean insideJob = false;

        List<CustomerResponse> result =
                new ArrayList<>();

        for (String line : lines) {

            if (line.contains("START  JOB")
                    && line.contains(jobName)) {

                insideJob = true;
                continue;
            }

            if (insideJob
                    && line.contains("END   JOB")
                    && line.contains(jobName)) {

                break;
            }

            if (!insideJob) {
                continue;
            }

            String record =
                    extractCustomerRecord(line);

            if (record != null) {
                result.add(
                        customerRecordParser.parse(record)
                );
            }
        }

        return result;
    }

    private String extractCustomerRecord(String line) {

        if (line == null) {
            return null;
        }

        int activeIndex = line.indexOf("AC000");
        int inactiveIndex = line.indexOf("IC000");

        int index;

        if (activeIndex >= 0) {
            index = activeIndex;
        } else if (inactiveIndex >= 0) {
            index = inactiveIndex;
        } else {
            return null;
        }

        String candidate = line.substring(index);

        if (candidate.length() < 119) {
            return null;
        }

        return candidate.substring(0, 119);
    }

    private void sleep() {

        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for mainframe",
                    e
            );
        }
    }

    public MainframeResult changeStatus(
            String customerId,
            String status
    ) {

        String requestId =
                requestIdGenerator.next();

        String inputRecord =
                customerRecordMapper.toStatusUpdateRecord(
                        requestId,
                        customerId,
                        status
                );

        String jobName =
                jobNameGenerator.next();

        String resultDataset =
                "MBANK.RES." + requestId;

        /*
         * MUSI być przed submitJcl().
         * Odpowiedź może przyjść bardzo szybko.
         */
        tcpResultListener.register(requestId);

        try {

            String jcl =
                    businessJclFactory.createUpdate(
                            jobName,
                            resultDataset,
                            CustomerMainframeOperations.CHANGE_STATUS,
                            inputRecord
                    );

            mainframeGateway.submitJcl(jcl);

            List<String> rawRecords;

            try {

                rawRecords =
                        tcpResultListener.await(
                                requestId,
                                Duration.ofSeconds(5)
                        );

            } catch (TimeoutException e) {

                /*
                 * TCP nie odpowiedział w czasie.
                 * Wracamy do naszego starego mechanizmu
                 * odczytu datasetu.
                 */
                rawRecords =
                        mainframeResultStore.read(
                                resultDataset
                        );
            }

            MainframeResult result =
                    mainframeResultParser.parse(
                            rawRecords
                    );

            validateResult(result);

            return result;

        } finally {

            tcpResultListener.unregister(requestId);

            /*
             * CHGCUST nadal tworzy dataset jako fallback,
             * więc po wszystkim go sprzątamy.
             */
            try {
                mainframeResultStore.delete(
                        resultDataset
                );
            } catch (Exception ignored) {
                // cleanup nie może zepsuć odpowiedzi HTTP
            }
        }
    }

    private void validateResult(
            MainframeResult result
    ) {

        if ("E".equals(result.header().type())) {
            throw new IllegalStateException(
                    "Mainframe error: "
                            + result.header().code()
            );
        }

        if (!"S".equals(result.header().type())) {
            throw new IllegalStateException(
                    "Unexpected mainframe result type: "
                            + result.header().type()
            );
        }

        if (!"CHGCUST".equals(
                result.header().operation()
        )) {
            throw new IllegalStateException(
                    "Unexpected operation: "
                            + result.header().operation()
            );
        }
    }

}