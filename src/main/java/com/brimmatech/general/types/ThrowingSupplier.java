package com.brimmatech.general.types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingSupplier<T> extends Supplier<Optional<T>> {
    Logger log = LoggerFactory.getLogger(ThrowingSupplier.class);

    static <U> Optional<U> getCapturingExceptions(ThrowingSupplier<U> supplier) {
        try {
            return Optional.ofNullable(supplier.getOrThrow());
        } catch (Exception  e) {
            log.debug("getCapturingExceptions: Captured exception", e);
            return Optional.empty();
        }
    }

    @Override
    default Optional<T> get() {
        return getCapturingExceptions(this);
    }

    T getOrThrow() throws Exception;
}