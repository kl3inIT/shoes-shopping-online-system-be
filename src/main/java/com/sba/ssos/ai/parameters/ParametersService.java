package com.sba.ssos.ai.parameters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sba.ssos.exception.base.BadRequestException;
import com.sba.ssos.exception.base.NotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ParametersService {

  private final AiParametersRepository repository;
  private final Resource defaultResource;
  private final ObjectMapper yamlMapper;

  public ParametersService(
      AiParametersRepository repository,
      @Value("${ai.parameters.path:classpath:/ai/default-chat-params.yml}")
          Resource defaultResource) {
    this.repository = repository;
    this.defaultResource = defaultResource;
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
  }

  public ParametersReader loadReader(AiParametersTargetType type) {
    Optional<AiParameters> active = repository.findFirstByActiveTrueAndTargetType(type);
    if (active.isPresent() && active.get().getContent() != null) {
      return parseYaml(active.get().getContent());
    }
    return loadFromClasspath();
  }

  public ParametersReader loadReader() {
    return loadReader(AiParametersTargetType.CHAT);
  }

  public String getSystemMessage() {
    return loadReader().getString("systemMessage");
  }

  public String getSystemMessage(AiParametersTargetType type) {
    return loadReader(type).getString("systemMessage");
  }

  private AiParameters getByIdEntity(UUID id) {
    return repository.findById(id).orElseThrow(() -> new NotFoundException("AiParameters", id));
  }

  public AiParameterDetailResponse findById(UUID id) {
    return AiParameterDetailResponse.from(getByIdEntity(id));
  }

  public List<AiParameterSummaryResponse> listAll(@Nullable AiParametersTargetType type) {
    List<AiParameters> entities =
        type != null
            ? repository.findByTargetTypeOrderByCreatedAtDesc(type)
            : repository.findAll();
    return entities.stream().map(AiParameterSummaryResponse::from).toList();
  }

  @Transactional
  public AiParameterDetailResponse create(
      AiParametersTargetType targetType, String description, String content) {
    AiParameters params = new AiParameters();
    params.setTargetType(targetType);
    params.setDescription(description);
    params.setContent(content);
    params.setActive(false);
    return AiParameterDetailResponse.from(repository.save(params));
  }

  @Transactional
  public AiParameterDetailResponse updateContent(
      UUID id, String content, @Nullable String description) {
    AiParameters params = getByIdEntity(id);
    params.setContent(content);
    if (description != null) {
      params.setDescription(description);
    }
    return AiParameterDetailResponse.from(repository.save(params));
  }

  @Transactional
  public AiParameterSummaryResponse activate(UUID id) {
    AiParameters params = getByIdEntity(id);
    repository.deactivateAllExcept(params.getTargetType(), params.getId());
    params.setActive(true);
    return AiParameterSummaryResponse.from(repository.save(params));
  }

  @Transactional
  public AiParameterDetailResponse copy(UUID sourceId) {
    AiParameters source = getByIdEntity(sourceId);
    AiParameters copy = new AiParameters();
    copy.setTargetType(source.getTargetType());
    copy.setContent(source.getContent());
    copy.setDescription(
        source.getDescription() != null ? source.getDescription() + " (copy)" : "copy");
    copy.setActive(false);
    return AiParameterDetailResponse.from(repository.save(copy));
  }

  @Transactional
  public void delete(UUID id) {
    AiParameters params = getByIdEntity(id);
    if (params.isActive()) {
      throw new BadRequestException("error.ai_parameters.delete_active");
    }
    repository.delete(params);
  }

  @Transactional
  public AiParameterDetailResponse createFromDefault(AiParametersTargetType type) {
    AiParameters params = new AiParameters();
    params.setTargetType(type);
    params.setContent(loadDefaultContent());
    params.setDescription("Loaded from default YAML");
    params.setActive(true);
    repository.deactivateAllExcept(type, UUID.randomUUID());
    return AiParameterDetailResponse.from(repository.save(params));
  }

  public String loadDefaultContent() {
    try (InputStream is = defaultResource.getInputStream()) {
      return new String(is.readAllBytes());
    } catch (Exception e) {
      log.error("Failed to read default parameters file", e);
      return "";
    }
  }

  @SuppressWarnings("unchecked")
  private ParametersReader parseYaml(String yaml) {
    try {
      Map<String, Object> map = yamlMapper.readValue(yaml, Map.class);
      return new ParametersReader(map);
    } catch (Exception e) {
      log.error("Failed to parse parameters YAML from DB, falling back to classpath", e);
      return loadFromClasspath();
    }
  }

  @SuppressWarnings("unchecked")
  private ParametersReader loadFromClasspath() {
    try (InputStream is = defaultResource.getInputStream()) {
      Map<String, Object> map = yamlMapper.readValue(is, Map.class);
      return new ParametersReader(map);
    } catch (Exception e) {
      log.error("Failed to load AI parameters from {}", defaultResource, e);
      return new ParametersReader(Map.of());
    }
  }
}
