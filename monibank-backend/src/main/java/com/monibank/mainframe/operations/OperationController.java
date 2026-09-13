package com.monibank.mainframe.operations;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
@Validated
public class OperationController {

    private final OperationQueryService operationQueryService;

    public OperationController(
            OperationQueryService operationQueryService
    ) {
        this.operationQueryService = operationQueryService;
    }

    @GetMapping
    public ResponseEntity<List<CoreOperationEvent>> getOperations(
            @RequestParam(defaultValue = "24")
            @Min(1) @Max(720)
            int hours,

            @RequestParam(defaultValue = "100")
            @Min(1) @Max(500)
            int limit,

            @RequestParam(required = false)
            CoreOperationEvent.Core core,

            @RequestParam(required = false)
            CoreOperationEvent.Status status,

            @RequestParam(required = false)
            String operation
    ) {
        return ResponseEntity.ok(operationQueryService.find(
                hours,
                limit,
                core,
                status,
                operation
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<OperationSummaryResponse> getSummary(
            @RequestParam(defaultValue = "24")
            @Min(1) @Max(720)
            int hours,

            @RequestParam(required = false)
            CoreOperationEvent.Core core
    ) {
        return ResponseEntity.ok(
                operationQueryService.summarize(hours, core)
        );
    }
}
