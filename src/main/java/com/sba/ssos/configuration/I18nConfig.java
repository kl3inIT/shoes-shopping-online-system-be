package com.sba.ssos.configuration;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class I18nConfig {

  /**
   * Configures the message source for internationalization.
   *
   * <p>This method sets up a ResourceBundleMessageSource to load messages from the "messages"
   * properties file.
   *
   * @return a MessageSource object configured for internationalization
   */
  @Bean
  public MessageSource messageSource() {
    ReloadableResourceBundleMessageSource messageSource =
        new ReloadableResourceBundleMessageSource();
    messageSource.setBasename("classpath:i18n/messages");
    messageSource.setDefaultEncoding("UTF-8");
    messageSource.setCacheSeconds(3600);
    messageSource.setFallbackToSystemLocale(false);
    return messageSource;
  }

  @Bean
  public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
    resolver.setDefaultLocale(Locale.ENGLISH);
    return resolver;
  }

  /**
   * Configures the LocalValidatorFactoryBean for validation.
   *
   * <p>This method sets up a LocalValidatorFactoryBean to use the message source for validation
   * messages.
   *
   * @return a LocalValidatorFactoryBean object configured for validation
   */
  @Bean
  public LocalValidatorFactoryBean getValidator() {
    LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
    factory.setValidationMessageSource(messageSource());
    return factory;
  }
}
