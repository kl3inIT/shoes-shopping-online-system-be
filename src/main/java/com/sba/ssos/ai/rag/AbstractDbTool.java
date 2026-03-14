package com.sba.ssos.ai.rag;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;
import org.springframework.util.ReflectionUtils;

public abstract class AbstractDbTool {

  protected final String toolName;
  protected final String description;
  protected final Consumer<String> logger;

  protected AbstractDbTool(String toolName, String description, Consumer<String> logger) {
    this.toolName = toolName;
    this.description = description;
    this.logger = logger;
  }

  public ToolCallback getToolCallback() {
    Method method =
        Objects.requireNonNull(ReflectionUtils.findMethod(getClass(), "execute", String.class));

    return MethodToolCallback.builder()
        .toolDefinition(
            ToolDefinition.builder()
                .name(toolName)
                .description(description)
                .inputSchema(JsonSchemaGenerator.generateForMethodInput(method))
                .build())
        .toolObject(this)
        .toolMethod(method)
        .build();
  }

  public abstract String execute(String queryText);
}
