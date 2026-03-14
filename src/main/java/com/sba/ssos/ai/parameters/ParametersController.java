package com.sba.ssos.ai.parameters;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.ai.CreateAiParameterRequest;
import com.sba.ssos.dto.request.ai.UpdateAiParameterRequest;
import com.sba.ssos.utils.LocaleUtils;
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
@RequestMapping("/api/v1/ai-parameters")
@RequiredArgsConstructor
public class ParametersController {

  private final ParametersService parametersService;
  private final LocaleUtils localeUtils;

  @GetMapping({"", "/"})
  public ResponseGeneral<List<AiParameterSummaryResponse>> list(
      @RequestParam(required = false) AiParametersTargetType type) {

    List<AiParameterSummaryResponse> summaries = parametersService.listAll(type);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.ai_parameters.fetched"), summaries);
  }

  @GetMapping("/{id}")
  public ResponseGeneral<AiParameterDetailResponse> get(@PathVariable UUID id) {
    AiParameterDetailResponse dto = parametersService.findById(id);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.ai_parameters.retrieved"), dto);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseGeneral<AiParameterDetailResponse> create(
      @Valid @RequestBody CreateAiParameterRequest req) {

    AiParameterDetailResponse saved =
        parametersService.create(req.targetType(), req.description(), req.content());
    return ResponseGeneral.ofCreated(localeUtils.get("success.ai_parameters.created"), saved);
  }

  @PostMapping("/from-default")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseGeneral<AiParameterDetailResponse> createFromDefault(
      @RequestParam(defaultValue = "CHAT") AiParametersTargetType type) {

    AiParameterDetailResponse saved = parametersService.createFromDefault(type);
    return ResponseGeneral.ofCreated(
        localeUtils.get("success.ai_parameters.created_from_default"), saved);
  }

  @PutMapping("/{id}")
  public ResponseGeneral<AiParameterDetailResponse> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateAiParameterRequest req) {

    AiParameterDetailResponse saved =
        parametersService.updateContent(id, req.content(), req.description());
    return ResponseGeneral.ofSuccess(localeUtils.get("success.ai_parameters.updated"), saved);
  }

  @PostMapping("/{id}/activate")
  public ResponseGeneral<AiParameterSummaryResponse> activate(@PathVariable UUID id) {
    AiParameterSummaryResponse activated = parametersService.activate(id);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.ai_parameters.activated"), activated);
  }

  @PostMapping("/{id}/copy")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseGeneral<AiParameterDetailResponse> copy(@PathVariable UUID id) {
    AiParameterDetailResponse copied = parametersService.copy(id);
    return ResponseGeneral.ofCreated(localeUtils.get("success.ai_parameters.copied"), copied);
  }

  @DeleteMapping("/{id}")
  public ResponseGeneral<Void> delete(@PathVariable UUID id) {
    parametersService.delete(id);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.ai_parameters.deleted"));
  }
}
