package com.sba.ssos.ai.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sba.ssos.dto.response.PageResponse;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Test scaffold for CheckAdminService.
 * Covers: SC-4 (check def CRUD), SC-5a (trigger run), SC-5b (run history).
 */
@ExtendWith(MockitoExtension.class)
class AdminCheckServiceTest {

    @Mock
    private CheckDefRepository checkDefRepository;

    @Mock
    private CheckRunRepository checkRunRepository;

    @Mock
    private CheckResultRepository checkResultRepository;

    @Mock
    private CheckRunner checkRunner;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CheckAdminServiceImpl checkAdminService;

    // ── SC-4a: List definitions ───────────────────────────────────────────────

    @Test
    void getCheckDefs_returnsList() {
        CheckDef def = new CheckDef();
        def.setQuestion("Q1");
        def.setReferenceAnswer("A1");
        def.setActive(true);

        when(checkDefRepository.findAll()).thenReturn(List.of(def));

        List<CheckDefResponse> result = checkAdminService.getCheckDefs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).question()).isEqualTo("Q1");
    }

    // ── SC-4b: Create definition ──────────────────────────────────────────────

    @Test
    void createCheckDef_persistsAndReturns() {
        CheckDefCreateRequest request = new CheckDefCreateRequest("Q?", "A!", "cat", true);

        CheckDef saved = new CheckDef();
        saved.setQuestion("Q?");
        saved.setReferenceAnswer("A!");
        saved.setCategory("cat");
        saved.setActive(true);

        when(checkDefRepository.save(any(CheckDef.class))).thenReturn(saved);

        CheckDefResponse response = checkAdminService.createCheckDef(request);

        verify(checkDefRepository).save(any(CheckDef.class));
        assertThat(response.question()).isEqualTo("Q?");
        assertThat(response.category()).isEqualTo("cat");
    }

    // ── SC-4c: Update definition ──────────────────────────────────────────────

    @Test
    void updateCheckDef_returnsUpdated() {
        UUID id = UUID.randomUUID();
        CheckDef existing = new CheckDef();
        existing.setQuestion("old");
        existing.setReferenceAnswer("ref");
        existing.setActive(false);

        when(checkDefRepository.findById(id)).thenReturn(Optional.of(existing));
        when(checkDefRepository.save(any(CheckDef.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckDefUpdateRequest request = new CheckDefUpdateRequest("new", null, null, true);
        CheckDefResponse response = checkAdminService.updateCheckDef(id, request);

        assertThat(response.question()).isEqualTo("new");
        assertThat(response.active()).isTrue();
        verify(checkDefRepository).save(existing);
    }

    // ── SC-4d: Delete definition ──────────────────────────────────────────────

    @Test
    void deleteCheckDef_callsRepository() {
        UUID id = UUID.randomUUID();
        CheckDef def = new CheckDef();

        when(checkDefRepository.findById(id)).thenReturn(Optional.of(def));

        checkAdminService.deleteCheckDef(id);

        verify(checkDefRepository).delete(def);
    }

    @Test
    void deleteCheckDef_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(checkDefRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkAdminService.deleteCheckDef(id))
                .isInstanceOf(NotFoundException.class);
    }

    // ── SC-5a: Trigger run ────────────────────────────────────────────────────

    @Test
    void triggerRun_delegatesToCheckRunner() {
        CheckRunSummary summary = new CheckRunSummary(UUID.randomUUID(), 0.9, "ok");
        when(checkRunner.runChecksAndSummarise()).thenReturn(summary);

        CheckRunSummary result = checkAdminService.triggerRun();

        verify(checkRunner).runChecksAndSummarise();
        assertThat(result).isEqualTo(summary);
    }

    // ── SC-5b: Run history ────────────────────────────────────────────────────

    @Test
    void getCheckRuns_returnsPaginatedHistory() {
        CheckRun run = new CheckRun();
        run.setScore(0.8);
        run.setCreatedBy(null);  // null path → toRunSummary returns null username without userRepository call

        when(checkRunRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(run)));

        PageResponse<CheckRunSummaryResponse> result = checkAdminService.getCheckRuns(0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).score()).isEqualTo(0.8);
    }

    // ── SC-5c: Per-run results ────────────────────────────────────────────────

    @Test
    void getCheckResults_returnsPerRunDetail() {
        UUID runId = UUID.randomUUID();

        CheckResult res = new CheckResult();
        res.setQuestion("Q");
        res.setReferenceAnswer("ref");
        res.setActualAnswer("actual");
        res.setScore(0.75);
        res.setLog("log");

        when(checkResultRepository.findByCheckRunIdOrderByScoreAsc(runId)).thenReturn(List.of(res));

        List<CheckResultDetailResponse> result = checkAdminService.getCheckResults(runId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).actualAnswer()).isEqualTo("actual");
        assertThat(result.get(0).score()).isEqualTo(0.75);
    }
}
