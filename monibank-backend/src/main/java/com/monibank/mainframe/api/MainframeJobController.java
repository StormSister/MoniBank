package com.monibank.mainframe.api;

import com.monibank.mainframe.hercules.MainframeBusinessJclFactory;
import com.monibank.mainframe.hercules.*;
import com.monibank.mainframe.hercules.jcl.*;
import com.monibank.mainframe.model.JobStatus;
import com.monibank.mainframe.model.JobSubmission;
import com.monibank.mainframe.model.MainframeJob;
import com.monibank.mainframe.port.MainframeGateway;
import com.monibank.mainframe.port.MainframeResultStore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mainframe/jobs")
@RequiredArgsConstructor
public class MainframeJobController {

    private final MainframeGateway mainframeGateway;
    private final TestJclFactory testJclFactory;
    private final HerculesJobTracker jobTracker;
    private final JobNameGenerator jobNameGenerator;
    private final PutCobolJclFactory putCobolJclFactory;
    private final CompileCobolJclFactory compileCobolJclFactory;
    private final RunCobolJclFactory runCobolJclFactory;
    private final MainframeResultStore mainframeResultStore;

    @PostMapping("/test")
    public ResponseEntity<JobSubmission> submitTestJob() {

        String jobName = jobNameGenerator.next();

        String jcl = testJclFactory.create(jobName);

        mainframeGateway.submitJcl(jcl);

        return ResponseEntity.accepted()
                .body(new JobSubmission(
                        jobName,
                        JobStatus.SUBMITTED
                ));
    }

    @GetMapping("/{jobName}")
    public ResponseEntity<MainframeJob> getJob(
            @PathVariable String jobName
    ) {
        return jobTracker.findJob(jobName)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/put-cobol/{programName}")
    public ResponseEntity<String> putCobol(
            @PathVariable String programName
    ) {

        mainframeGateway.submitJcl(
                putCobolJclFactory.create(programName)
        );

        return ResponseEntity.accepted()
                .body("SUBMITTED");
    }

    @PostMapping("/compile-cobol/{programName}")
    public ResponseEntity<String> compileCobol(
            @PathVariable String programName
    ) {

        mainframeGateway.submitJcl(
                compileCobolJclFactory.create(programName)
        );

        return ResponseEntity.accepted()
                .body("SUBMITTED");
    }

    @PostMapping("/run-cobol/{programName}")
    public ResponseEntity<String> runCobol(
            @PathVariable String programName
    ) {

        String normalizedProgramName =
                programName.toUpperCase();

        mainframeGateway.submitJcl(
                runCobolJclFactory.create(normalizedProgramName)
        );

        return ResponseEntity.accepted()
                .body("SUBMITTED");
    }

    @GetMapping("/result/{requestId}")
    public ResponseEntity<List<String>> readResult(
            @PathVariable String requestId
    ) {

        String datasetName =
                "MBANK.RES." + requestId;

        return ResponseEntity.ok(
                mainframeResultStore.read(datasetName)
        );
    }

}