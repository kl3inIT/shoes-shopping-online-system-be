package com.sba.ssos.ai.checks;

import com.sba.ssos.dto.response.PageResponse;
import com.sba.ssos.entity.User;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CheckAdminServiceImpl implements CheckAdminService {

    private final CheckDefRepository checkDefRepository;
    private final CheckRunRepository checkRunRepository;
    private final CheckResultRepository checkResultRepository;
    private final CheckRunner checkRunner;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CheckDefResponse> getCheckDefs() {
        return checkDefRepository.findAll().stream()
                .map(this::toDefResponse)
                .toList();
    }

    @Override
    public CheckDefResponse createCheckDef(CheckDefCreateRequest request) {
        CheckDef def = new CheckDef();
        def.setQuestion(request.question());
        def.setReferenceAnswer(request.referenceAnswer());
        def.setCategory(request.category());
        def.setActive(request.active());
        checkDefRepository.save(def);
        return toDefResponse(def);
    }

    @Override
    public CheckDefResponse updateCheckDef(UUID id, CheckDefUpdateRequest request) {
        CheckDef def = checkDefRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CheckDef", id));
        if (request.question() != null) def.setQuestion(request.question());
        if (request.referenceAnswer() != null) def.setReferenceAnswer(request.referenceAnswer());
        if (request.category() != null) def.setCategory(request.category());
        if (request.active() != null) def.setActive(request.active());
        checkDefRepository.save(def);
        return toDefResponse(def);
    }

    @Override
    public void deleteCheckDef(UUID id) {
        CheckDef def = checkDefRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CheckDef", id));
        checkResultRepository.deleteByCheckDefId(id);
        checkDefRepository.delete(def);
    }

    @Override
    public CheckRunSummary triggerRun() {
        return checkRunner.runChecksAndSummarise();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CheckRunSummaryResponse> getCheckRuns(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(checkRunRepository.findAll(pageable).map(this::toRunSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckResultDetailResponse> getCheckResults(UUID runId) {
        return checkResultRepository.findByCheckRunIdOrderByScoreAsc(runId).stream()
                .map(this::toResultDetail)
                .toList();
    }

    @Override
    public void deleteCheckRun(UUID id) {
        CheckRun run = checkRunRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("CheckRun", id));
        checkResultRepository.deleteByCheckRunId(id);
        checkRunRepository.delete(run);
    }

    // ── Private mappers ───────────────────────────────────────────────────────

    private CheckDefResponse toDefResponse(CheckDef def) {
        return new CheckDefResponse(
                def.getId(),
                def.getCategory(),
                def.getQuestion(),
                Boolean.TRUE.equals(def.getActive()),
                def.getCreatedAt(),
                def.getCreatedBy());
    }

    private CheckRunSummaryResponse toRunSummary(CheckRun run) {
        String username = run.getCreatedBy() == null ? null
                : userRepository.findByKeycloakId(run.getCreatedBy())
                        .map(User::getUsername)
                        .orElse(null);
        return new CheckRunSummaryResponse(
                run.getId(),
                run.getScore(),
                run.getCreatedAt(),
                run.getCreatedBy(),
                username);
    }

    private CheckResultDetailResponse toResultDetail(CheckResult result) {
        return new CheckResultDetailResponse(
                result.getQuestion(),
                result.getReferenceAnswer(),
                result.getActualAnswer(),
                result.getScore(),
                result.getLog());
    }
}
