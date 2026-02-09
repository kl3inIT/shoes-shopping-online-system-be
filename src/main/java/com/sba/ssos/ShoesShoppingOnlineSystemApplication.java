package com.sba.ssos;

import com.sba.ssos.configuration.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableJpaAuditing
public class ShoesShoppingOnlineSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoesShoppingOnlineSystemApplication.class, args);
    }
}
