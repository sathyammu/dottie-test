package com.brimmatech.general.utils;

import com.azure.core.exception.HttpResponseException;
import com.brimmatech.docflow.exception.TaskFailureException;
import com.brimmatech.docflow.v2.models.SpringAiChatMemory;
import com.brimmatech.docflow.v2.repositories.SpringAiChatMemoryRepository;
import com.brimmatech.general.types.ThrowingSupplier;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jbock.util.Either;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.logging.log4j.util.Strings;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmUtils {

    private static final int NUM_RETRIES = 3;
    public final Supplier<LocalDateTime> nowUtcLocalDateTime;
    private final ObjectMapper objectMapper;
    private final SpringAiChatMemoryRepository springAiChatMemoryRepository;

    public <T> Either<TaskFailureException, T> retriedResultFetcher(String conversationId,
                                                                    CheckedFunction<Either<TaskFailureException, T>> work,

                                                                    IsWorkDone<T> isWorkDone) {
        int retries = 0;
        Either<TaskFailureException, T> currentResult = null;
        boolean isDone = false;
        AtomicReference<List<Message>> messageAtomicReference = new AtomicReference<>(new ArrayList<>());
        while (retries < NUM_RETRIES && !isDone) {
            try {
                currentResult = work.apply(messageAtomicReference.get());
            } catch (RuntimeException e) {
                if (e.getCause() instanceof JacksonException jacksonException) {
                    log.debug("Got exception while processing LLM output", jacksonException);
                    messageAtomicReference.set(List.of(UserMessage.builder()
                            .text(String.format(
                                    "Your response is not valid JSON. Please reply with valid json. The exception " +
                                            "details" +
                                            " are : %s",
                                    jacksonException.getMessage()))
                            .build()));
                    retries++;
                    continue;
                } else if (e instanceof HttpResponseException httpResponseException) {
                    log.debug("Got exception while contacting LLM", e);
                    return Either.left(new TaskFailureException(httpResponseException));
                } else {
                    return Either.left(new TaskFailureException(e.getMessage(), e.getClass().getName()));
                }
            } catch (JacksonException e) {
                log.debug("Got exception while processing LLM output", e);
                messageAtomicReference.set(List.of(UserMessage.builder()
                        .text(String.format(
                                "Your response is not valid JSON. Please reply with valid json. The exception details" +
                                        " are : %s",
                                e.getMessage()))
                        .build()));
                retries++;
                continue;
            }
            isDone = Objects.isNull(currentResult) || currentResult.fold(l -> true, v -> {
                var thisFeedbackMessages = new ArrayList<>(messageAtomicReference.get());
                thisFeedbackMessages.add(new AssistantMessage(ThrowingSupplier.getCapturingExceptions(() -> objectMapper.writeValueAsString(
                        v)).orElse(
                        Strings.EMPTY)));

                WorkResultReview reviewResult = isWorkDone.apply(thisFeedbackMessages, v);

                if (!reviewResult.isDone &&
                        Objects.nonNull(reviewResult.message) &&
                        !reviewResult.message.isEmpty() &&
                        reviewResult.message.getLast().getMessageType().equals(MessageType.USER)) {
                    saveLatestMessage(reviewResult.message.getLast(), conversationId);
                }

                messageAtomicReference.set(reviewResult.message);
                return reviewResult.isDone;
            });
            log.debug("Got {} from isDone. Retry count: {}", isDone, retries);
            retries++;
        }

        return currentResult;
    }

    private void saveLatestMessage(Message last, String conversationId) {
        val newConversation = new SpringAiChatMemory();
        newConversation.setType(MessageType.USER.name());
        newConversation.setContent(last.getText());
        newConversation.setConversationId(conversationId);
        newConversation.setTimestamp(nowUtcLocalDateTime.get());
        springAiChatMemoryRepository.save(newConversation);
    }

    @FunctionalInterface
    public interface CheckedFunction<T> {
        T apply(List<Message> messages) throws JsonProcessingException, HttpResponseException;
    }

    @FunctionalInterface
    public interface IsWorkDone<T> {
        WorkResultReview apply(List<Message> messages, T result);
    }


    @Builder
    public record WorkResultReview(List<Message> message, boolean isDone) {
    }
}
