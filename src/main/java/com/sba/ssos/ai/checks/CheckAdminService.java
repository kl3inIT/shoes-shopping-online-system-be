package com.sba.ssos.ai.checks;

import com.sba.ssos.dto.response.PageResponse;
import java.util.List;
import java.util.UUID;

public interface CheckAdminService {

    List<CheckDefResponse> getCheckDefs();

    CheckDefResponse createCheckDef(CheckDefCreateRequest request);

    CheckDefResponse updateCheckDef(UUID id, CheckDefUpdateRequest request);

    void deleteCheckDef(UUID id);

    CheckRunSummary triggerRun();

    PageResponse<CheckRunSummaryResponse> getCheckRuns(int page, int size);

    List<CheckResultDetailResponse> getCheckResults(UUID runId);

    void deleteCheckRun(UUID id);
}
