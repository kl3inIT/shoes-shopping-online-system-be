package com.sba.ssos.controller.advice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sba.ssos.configuration.I18nConfig;
import com.sba.ssos.exception.base.InternalServerErrorException;
import com.sba.ssos.utils.LocaleUtils;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;
  private String vietnameseInternalServerMessage;

  @BeforeEach
  void setUp() {
    I18nConfig i18nConfig = new I18nConfig();
    LocaleUtils localeUtils = new LocaleUtils(i18nConfig.messageSource());
    AcceptHeaderLocaleResolver localeResolver =
        (AcceptHeaderLocaleResolver) i18nConfig.localeResolver();

    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler(localeUtils))
            .setLocaleResolver(localeResolver)
            .build();
    vietnameseInternalServerMessage =
        i18nConfig
            .messageSource()
            .getMessage("error.internal.server", null, Locale.forLanguageTag("vi"));
  }

  @Test
  void localizedErrorUsesTranslatedMessageInsteadOfRawMessageKey() throws Exception {
    mockMvc
        .perform(get("/test/internal-server-error").header("Accept-Language", "vi"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.detail").value(vietnameseInternalServerMessage))
        .andExpect(jsonPath("$.message").value(vietnameseInternalServerMessage))
        .andExpect(jsonPath("$.messageKey").doesNotExist());
  }

  @RestController
  static class TestController {

    @GetMapping("/test/internal-server-error")
    void throwInternalServerError() {
      throw new InternalServerErrorException();
    }
  }
}
