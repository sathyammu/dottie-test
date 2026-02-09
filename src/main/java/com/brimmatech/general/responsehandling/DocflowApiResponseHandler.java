package com.brimmatech.general.responsehandling;

import com.brimmatech.general.errorhandling.DocflowApiExceptionHandler;
import com.brimmatech.general.errorhandling.ResponseBuilderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;
import java.util.stream.Stream;

@RestControllerAdvice
@Order(2)
@NoArgsConstructor @Slf4j public class DocflowApiResponseHandler implements ResponseBodyAdvice<Object> {
    private static final String SUCCESS = "SUCCESS";
    private static final String ERROR = "ERROR";

    @Autowired ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !returnType.getContainingClass().equals(DocflowApiExceptionHandler.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        Object responseObject;
        log.debug("selectedContentType:{}", selectedContentType);
        if (body instanceof byte[]) {
            return body;
        }
        if (response instanceof ServletServerHttpResponse servletResponse) {
            int status = servletResponse.getServletResponse().getStatus();
            if (status == HttpStatus.NO_CONTENT.value()) {
                return null;
            }
        }
        //TODO: verify for pdf also
        if (body instanceof ResponseMessage) {
            responseObject = body;
        } else if (Stream.of(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML)
                .anyMatch(o -> selectedContentType.toString().startsWith(o.toString()))) {
            responseObject = body;
        } else if (selectedContentType.toString().contains("application/openmetrics-text")) {
            responseObject = body;
        } else if (request.getURI() != null && request.getURI().getPath().contains("/api-docs")) {
            // This is to handle swagger documentation related requests and responses.
            if (body instanceof String) {
                try {
                    responseObject = objectMapper.readValue((String) body, Map.class);
                } catch (JsonProcessingException exception) {
                    throw new ResponseBuilderException("Not able to build response for swagger documentation",
                            exception);
                }
            } else {
                responseObject = body;
            }
        } else {
            HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
            responseObject =
                    ResponseMessage.builder()
                            .code(servletResponse.getStatus())
                            .message(servletResponse.getStatus() < 300 ? SUCCESS : ERROR)
                            .data(body)
                            .build();

            return responseObject;

        }
        return responseObject;
    }
}
