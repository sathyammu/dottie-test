package com.brimmatech.general.errorhandling;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.exception.DocFlowDataProcessingException;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.exception.UnAuthorizedException;
import com.brimmatech.general.errorhandling.notifications.ExceptionOccurredNotifier;
import com.brimmatech.general.responsehandling.ResponseMessage;
import com.brimmatech.general.types.ThrowingSupplier;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
@Order(1)
public class DocflowApiExceptionHandler {

    private final ExceptionOccurredNotifier exceptionOccurredNotifier;
    private final TenantEntityRepository tenantEntityRepository;



    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ResponseMessage> handleBadRequestException(HttpClientErrorException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status(exception.getStatusCode())
                .body(
                        ResponseMessage.builder()
                                .code(exception.getStatusCode().value())
                                .message(exception.getStatusText())
                                .data(null)
                                .build());
    }

    @ExceptionHandler(ResponseBuilderException.class)
    public ResponseEntity<ResponseMessage> handleResponseBuilderException(ResponseBuilderException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ResponseMessage.builder()
                                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .message(exception.getMessage())
                                .data(null)
                                .build());

    }

    @ExceptionHandler(DocflowDataException.class)
    public ResponseEntity<ResponseMessage<Object>> handleDocFlowDataException(DocflowDataException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ResponseMessage.builder()
                                .code(HttpStatus.BAD_REQUEST.value())
                                .message(exception.getMessage())
                                .build());

    }

    @ExceptionHandler(DocFlowDataProcessingException.class)
    public ResponseEntity<ResponseMessage> handleDocFlowDateProcessingException(
            DocFlowDataProcessingException exception) {
        log.error(exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ResponseMessage.builder()
                                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .message(exception.getMessage())
                                .data(null)
                                .build());

    }

    @ExceptionHandler(ValliaAuthenticationException.class)
    public ResponseEntity<ResponseMessage> handleDocFlowDateProcessingException(
            ValliaAuthenticationException exception) {
        log.error("Got Exception:{}", exception.getMessage(), exception);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ResponseMessage.builder()
                                .code(HttpStatus.UNAUTHORIZED.value())
                                .message(exception.getMessage())
                                .data(null)
                                .build());

    }

    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<ResponseMessage> handleResponseBuilderException(UnAuthorizedException exception) {
        log.error("Got Exception:{}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ResponseMessage.builder()
                                .code(HttpStatus.UNAUTHORIZED.value())
                                .message(exception.getMessage())
                                .data(null)
                                .build());

    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body("Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseMessage> handleGenericException(Exception exception) {
        log.error("Got Exception:{}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ResponseMessage.builder()
                                .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .message(exception.getMessage())
                                .data(null)
                                .build());

    }

    private List<Error> convertConstraintViolationsToErrors(Set<ConstraintViolation<?>> constraintViolations) {
        List<Error> errors = new ArrayList<>();
        for (ConstraintViolation<?> constraintViolation : constraintViolations) {
            Error error = new Error();
            error.setMessage(constraintViolation.getMessage());
            error.setFieldName(constraintViolation.getPropertyPath().toString());
            errors.add(error);
        }
        return errors;
    }

    private void notifyException(Exception exception, TenantEntity requestingTenant, HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String requestParams = request.getQueryString();
        exceptionOccurredNotifier.notifyException(exception, requestingTenant, requestPath, requestParams);
    }

    public static class ErrorResponse {
        private String message;
        private List<Error> errors;
        // Getters and setters
    }

    @Data
    public static class Error {
        private String message;
        private String fieldName;
        // Getters and setters
    }

}
