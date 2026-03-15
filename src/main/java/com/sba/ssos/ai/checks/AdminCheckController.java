package com.sba.ssos.ai.checks;

import com.sba.ssos.constant.ApiPaths;
import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.ADMIN_CHECKS)
@RequiredArgsConstructor
@Tag(name = "Admin Checks", description = "Administrative AI check management endpoints")
public class AdminCheckController {

    private final CheckAdminService checkAdminService;

    @GetMapping("/definitions")
    public ResponseGeneral<List<CheckDefResponse>> getCheckDefs() {
        return ResponseGeneral.ofSuccess("Check definitions retrieved", checkAdminService.getCheckDefs());
    }

    @PostMapping("/definitions")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseGeneral<CheckDefResponse> createCheckDef(
            @Valid @RequestBody CheckDefCreateRequest request) {
        return ResponseGeneral.ofCreated("Check definition created", checkAdminService.createCheckDef(request));
    }

    @PutMapping("/definitions/{id}")
    public ResponseGeneral<CheckDefResponse> updateCheckDef(
            @PathVariable UUID id,
            @Valid @RequestBody CheckDefUpdateRequest request) {
        return ResponseGeneral.ofSuccess("Check definition updated", checkAdminService.updateCheckDef(id, request));
    }

    @DeleteMapping("/definitions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCheckDef(@PathVariable UUID id) {
        checkAdminService.deleteCheckDef(id);
    }

    @PostMapping("/runs")
    public ResponseGeneral<CheckRunSummary> triggerRun() {
        CheckRunSummary result = checkAdminService.triggerRun();
        String msg = result != null ? "Check run completed" : "No active check definitions found";
        return ResponseGeneral.ofSuccess(msg, result);
    }

    @GetMapping("/runs")
    public ResponseGeneral<PageResponse<CheckRunSummaryResponse>> getCheckRuns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseGeneral.ofSuccess("Check runs retrieved", checkAdminService.getCheckRuns(page, size));
    }

    @DeleteMapping("/runs/{runId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCheckRun(@PathVariable UUID runId) {
        checkAdminService.deleteCheckRun(runId);
    }

    @GetMapping("/runs/{runId}/results")
    public ResponseGeneral<List<CheckResultDetailResponse>> getCheckResults(@PathVariable UUID runId) {
        return ResponseGeneral.ofSuccess("Check results retrieved", checkAdminService.getCheckResults(runId));
    }
}
