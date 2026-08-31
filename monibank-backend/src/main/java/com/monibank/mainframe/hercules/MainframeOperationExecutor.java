package com.monibank.mainframe.hercules;

import com.monibank.mainframe.model.MainframeOperationSpec;
import com.monibank.mainframe.model.MainframeResult;
import com.monibank.mainframe.port.MainframeGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MainframeOperationExecutor {

    private final MainframeJclFactory jclFactory;
    private final MainframeGateway mainframeGateway;
    private final JobNameGenerator jobNameGenerator;
    private final MainframeResponseExecutor responseExecutor;

    public MainframeResult execute(
            String requestId,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        String jobName =
                jobNameGenerator.next();

        String resultDataset =
                "MBANK.RES." + requestId;

        String expectedOperation =
                expectedResultOperation(spec);

        log.info(
                "MAINFRAME [{}] {} started - "
                        + "job={}, dataset={}",
                requestId,
                spec.operationName(),
                jobName,
                resultDataset
        );

        MainframeResult result =
                responseExecutor.execute(
                        requestId,
                        expectedOperation,
                        resultDataset,
                        () -> sendBatchRequest(
                                requestId,
                                jobName,
                                resultDataset,
                                spec,
                                inputRecord
                        )
                );

        log.info(
                "MAINFRAME [{}] {} COMPLETED SUCCESSFULLY",
                requestId,
                spec.operationName()
        );

        return result;
    }

    private void sendBatchRequest(
            String requestId,
            String jobName,
            String resultDataset,
            MainframeOperationSpec spec,
            String inputRecord
    ) {

        String jcl =
                jclFactory.create(
                        jobName,
                        resultDataset,
                        spec,
                        inputRecord
                );

        mainframeGateway.submitJcl(jcl);

        log.info(
                "MAINFRAME [{}] Job {} submitted",
                requestId,
                jobName
        );
    }

    private String expectedResultOperation(
            MainframeOperationSpec spec
    ) {

        if (spec.programName() == null
                || spec.programName().isBlank()) {

            throw new IllegalStateException(
                    "Operation has no result operation name: "
                            + spec.operationName()
            );
        }

        return spec.programName();
    }
}