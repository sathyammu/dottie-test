package com.brimmatech.apps.deapi;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@EnableJpaRepositories("com.brimmatech.*")
@EntityScan("com.brimmatech.*")
@EnableAutoConfiguration()
@ComponentScans({
        @ComponentScan(basePackages = "com.brimmatech.general"),
        @ComponentScan(basePackages = "com.brimmatech.apps.deapi"),
        @ComponentScan(basePackages = "com.brimmatech.mcp"),
        @ComponentScan(basePackages = "com.brimmatech.docflow"),
        @ComponentScan(basePackages = "com.brimmatech.encompass"),
        @ComponentScan(basePackages = "com.brimmatech.saas"),
        @ComponentScan(basePackages = "com.brimmatech.princeton.dottie")


})
@Slf4j
public class DottieApiApplication extends SpringBootServletInitializer {

    @Value("${springdoc.swagger-ui.server-item-url}")
    private String applicationContextPath;

        public static void main(String[] args) {
                System.setProperty("spring.config.name", "de-api");
                val ctx = SpringApplication.run(DottieApiApplication.class, args);
                String cache = ctx.getEnvironment().getProperty("spring.thymeleaf.cache");
                log.info("Thymeleaf cache = {}", cache);
    }

    @Bean("jsonPathParseContext")
    ParseContext jsonPathParseContext() {
        Configuration build = Configuration
                .builder()
                .jsonProvider(new JacksonJsonProvider())
                .mappingProvider((new JacksonMappingProvider()))
                .build();
        ParseContext using = JsonPath.using(build);
        return using;
    }

    @Bean
    OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url(applicationContextPath))
                .info(new Info().title("Vallia DocIntel"))
                // Components section defines Security Scheme "mySecretHeader"
                .components(new Components()
                        .addSecuritySchemes("Content-Type", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Content-Type")))
                // AddSecurityItem section applies created scheme globally
                .addSecurityItem(new SecurityRequirement()
                        .addList("Content-Type"));
    }

}
