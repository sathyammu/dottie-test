package com.brimmatech.general.config;

import com.arakelian.jq.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.apache.commons.codec.digest.DigestUtils;
import org.modelmapper.ModelMapper;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
@Slf4j
@EnableAspectJAutoProxy
public class TemplateConfig {

    @Autowired
    ObjectMapper objectMapper;

    @Bean
    JqLibrary jqLibrary() {
        return ImmutableJqLibrary.of();

    }

    @Bean
    Function<String, String> md5HashFor() {
        return DigestUtils::md2Hex;
    }

    @Bean
    BiFunction<String, String, String> applyJQ() {
        return (inputJson, jqProgram) -> {
            @val
            JsonNode json;
            try {
                json = objectMapper.readTree(inputJson);
                final var responseString = objectMapper.writeValueAsString(json);

                final JqRequest request = ImmutableJqRequest.builder()
                        .lib(jqLibrary())
                        .input(responseString)
                        .filter(jqProgram)
                        .build();

                JqResponse execution = request.execute();
                if (execution.hasErrors()) {
                    System.out.println(String.join(",", execution.getErrors()));
                    return responseString;
                } else {
                    return execution.getOutput();
                }

            } catch (JsonProcessingException e) {
                log.error("Got exception", e);
                return null;
            }

        };
    }

    @Bean
    public OpenApiCustomizer addGlobalHeaders() {
        return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations()
                .forEach(operation -> {
                    Parameter header = new Parameter()
                            .in("header")
                            .name("x-api-Key")
                            .description("A Global Custom Header")
                            .required(true);
                    operation.addParametersItem(header);
                }));
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    @Bean
    public Supplier<String> nowUtc() {
        return () -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("UTC")));
    }

    @Bean
    public Supplier<LocalDateTime> nowUtcLocalDateTime() {
        return () -> ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime();
    }

    @Bean
    public Supplier<ZonedDateTime> nowUtcZoned() {
        return () -> ZonedDateTime.now(ZoneId.of("UTC"));
    }

    @Bean XmlMapper xmlMapper() {

        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return xmlMapper;
    }


    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }

    @Getter
    public enum TOPICS {
        UPDATE_ENCOMPASS("UPDATE_ENCOMPASS", "encompass_updated"),
        NOOP_SUPERVISOR("NOOP_SUPERVISOR", null),
        END_RUN("END_RUN", null);


        private final String name;
        private final String resourceName;

        TOPICS(String name, String resourceName) {
            this.name = name;
            this.resourceName = resourceName;
        }
    }

}
