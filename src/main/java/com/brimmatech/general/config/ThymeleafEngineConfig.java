package com.brimmatech.general.config;

import com.brimmatech.docflow.classification.assistants.SystemPromptProvider;
import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wimdeblauwe.htmx.spring.boot.thymeleaf.HtmxDialect;
import lombok.RequiredArgsConstructor;
import lombok.val;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;
import java.util.function.Function;

@Configuration
@RequiredArgsConstructor
public class ThymeleafEngineConfig {

    private final SpringResourceTemplateResolver templateResolver;
    private final ObjectMapper objectMapper;

    @Bean
    SpringTemplateEngine templateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new HtmxDialect(objectMapper));
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    @Bean
    TemplateEngine textBasedTemplateEngine(StringTemplateResolver textModeTemplateResolver) {
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(textModeTemplateResolver);
        return templateEngine;
    }

    @Bean
    StringTemplateResolver textModeTemplateResolver() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.TEXT);
        return resolver;
    }

    @Bean
    Function<SystemPromptProvider.TemplatedPromptMeta<?>, String> promptProcessor(TemplateEngine textBasedTemplateEngine) {
        return tTemplatedPromptMeta -> {
            val
                    contextAsMap =
                    ThrowingSupplier.getCapturingExceptions(() -> objectMapper.treeToValue(objectMapper.valueToTree(
                                    tTemplatedPromptMeta.templateContext()),
                            new TypeReference<Map<String, Object>>() {
                            })).orElse(Map.of());
            Context context = new Context();
            context.setVariables(contextAsMap);
            return textBasedTemplateEngine.process(tTemplatedPromptMeta.promptTemplateText(), context);
        };
    }

}
