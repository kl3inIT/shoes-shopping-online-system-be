package com.sba.ssos.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ApplicationProfileConfigFilesTest {

  private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

  @Test
  void applicationConfigFilesLoadWithExpectedProfiles() throws IOException {
    var baseSources = load("application.yml");
    var devSources = load("application-dev.yml");
    var prodSources = load("application-prod.yml");

    assertThat(propertyValue(baseSources, "spring.profiles.default")).isEqualTo("dev");
    assertThat(propertyValue(devSources, "spring.config.activate.on-profile")).isEqualTo("dev");
    assertThat(propertyValue(prodSources, "spring.config.activate.on-profile")).isEqualTo("prod");

    assertThat(propertyValue(devSources, "springdoc.swagger-ui.enabled")).isEqualTo(true);
    assertThat(propertyValue(prodSources, "server.forward-headers-strategy"))
        .isEqualTo("framework");
  }

  private List<PropertySource<?>> load(String resourcePath) throws IOException {
    return loader.load(resourcePath, new ClassPathResource(resourcePath));
  }

  private Object propertyValue(List<PropertySource<?>> propertySources, String name) {
    return propertySources.stream()
        .map(source -> source.getProperty(name))
        .filter(value -> value != null)
        .findFirst()
        .orElse(null);
  }
}
