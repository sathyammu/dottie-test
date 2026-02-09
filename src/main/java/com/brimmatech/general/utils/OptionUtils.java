package com.brimmatech.general.utils;

import com.brimmatech.docflow.exception.TaskFailureException;
import com.brimmatech.docflow.v2.task.globalapi.SubmittableJobOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.jbock.util.Either;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskResult;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionUtils {

    public static Optional<Boolean> fromBoolean(Boolean value) {
        return value ? Optional.of(value) : Optional.empty();
    }

    public static Optional<String> fromNullOrEmpty(String value) {
        return !Strings.isNullOrEmpty(value) ? Optional.empty() : Optional.of(value);
    }

    public static  <T>  Optional<T> fromJsonNode(JsonNode value, Function<JsonNode, T> valueMapper) {
        return value.isNull() ? Optional.empty()
                : Optional.of(valueMapper.apply(value));
    }

    public static Optional<Integer> fromPositiveInt(Integer value) {
        return value > 0 ? Optional.of(value) : Optional.empty();
    }

    public static <T> Optional<T> fromSupplier(Supplier<T> supplier) {
        return Optional.ofNullable((supplier.get()));
    }

    public static <L, R> Either<L, R> eitherFromOptional(Optional<R> optional, Supplier<L> leftSupplier) {
        return optional.<Either<L, R>>map(Either::right)
                .orElseGet(() -> Either.left(leftSupplier.get()));
    }

    public static <L, R> void ifPresent(Either<L, R> either, Consumer<R> leftSupplier) {
        if(either.isRight()) {
            either.getRight().ifPresent(leftSupplier);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Optional<T> merge(Optional<T> first, Optional<T> second, BiFunction<T, T, T> mergeAction) {

        if (first.isPresent() && second.isPresent()) {
            return Optional.of(mergeAction.apply(first.get(), second.get()));
        } else if (first.isPresent()) {
            return first;
        } else return second;

    }
}
