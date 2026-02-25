package com.sba.ssos.ai.parameters;

import com.sba.ssos.dto.ResponseGeneral;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai-parameters")
@RequiredArgsConstructor
public class ParametersController {

  private final ParametersService parametersService;

  public record ParamSummary(
      UUID id,
      String description,
      AiParameters.TargetType targetType,
      boolean active) {}

  public record ParamDetail(
      UUID id,
      String description,
      AiParameters.TargetType targetType,
      boolean active,
      String content) {}

  public record CreateRequest(
      @NotNull AiParameters.TargetType targetType,
      String description,
      @NotBlank String content) {}

  public record UpdateContentRequest(@NotBlank String content, String description) {}

  @GetMapping
  public ResponseGeneral<List<ParamSummary>> list(
      @RequestParam(required = false) AiParameters.TargetType type) {

    List<ParamSummary> summaries = parametersService.listAll(type).stream()
        .map(p -> new ParamSummary(p.getId(), p.getDescription(), p.getTargetType(), p.isActive()))
        .toList();

    return ResponseGeneral.ofSuccess("Parameters listed", summaries);
  }

  @GetMapping("/{id}")
  public ResponseGeneral<ParamDetail> get(@PathVariable UUID id) {
    AiParameters p = parametersService.findById(id);
    return ResponseGeneral.ofSuccess("Parameter detail", toDetail(p));
  }

  @PostMapping
  public ResponseGeneral<ParamDetail> create(@Valid @RequestBody CreateRequest req) {
    AiParameters saved = parametersService.create(req.targetType(), req.description(), req.content());
    return ResponseGeneral.ofCreated("Parameters created", toDetail(saved));
  }

  @PostMapping("/from-default")
  public ResponseGeneral<ParamDetail> createFromDefault(
      @RequestParam(defaultValue = "CHAT") AiParameters.TargetType type) {

    AiParameters saved = parametersService.createFromDefault(type);
    return ResponseGeneral.ofCreated("Parameters created from default YAML", toDetail(saved));
  }

  @PutMapping("/{id}")
  public ResponseGeneral<ParamDetail> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateContentRequest req) {

    AiParameters saved = parametersService.updateContent(id, req.content(), req.description());
    return ResponseGeneral.ofSuccess("Parameters updated", toDetail(saved));
  }

  @PostMapping("/{id}/activate")
  public ResponseGeneral<ParamSummary> activate(@PathVariable UUID id) {
    AiParameters activated = parametersService.activate(id);
    return ResponseGeneral.ofSuccess("Parameters activated",
        new ParamSummary(activated.getId(), activated.getDescription(),
            activated.getTargetType(), activated.isActive()));
  }

  @PostMapping("/{id}/copy")
  public ResponseGeneral<ParamDetail> copy(@PathVariable UUID id) {
    AiParameters copied = parametersService.copy(id);
    return ResponseGeneral.ofCreated("Parameters copied", toDetail(copied));
  }

  @DeleteMapping("/{id}")
  public ResponseGeneral<Void> delete(@PathVariable UUID id) {
    parametersService.delete(id);
    return ResponseGeneral.ofSuccess("Parameters deleted");
  }

  private ParamDetail toDetail(AiParameters p) {
    return new ParamDetail(
        p.getId(), p.getDescription(), p.getTargetType(), p.isActive(), p.getContent());
  }
}
