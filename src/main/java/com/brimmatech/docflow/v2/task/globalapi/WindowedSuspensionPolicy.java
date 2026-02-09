package com.brimmatech.docflow.v2.task.globalapi;

import com.brimmatech.docflow.v2.task.dto.CreationArgs;
import com.brimmatech.general.utils.DelaySpec;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WindowedSuspensionPolicy implements SuspensionPolicy {

    public static final class Window {
        final Duration startOffset;
        final Optional<Duration> lengthDurationOptional;
        final Duration period;
        final String successAction;
        final String failureAction;

        Window(Duration startOffset, Optional<Duration> lengthDurationOptional, Duration period,
               String successAction, String failureAction) {
            this.startOffset = startOffset;
            this.lengthDurationOptional = lengthDurationOptional;
            this.period = period;
            this.successAction = successAction;
            this.failureAction = failureAction;
        }

        Optional<Duration> endOffset() {
            return lengthDurationOptional.map(startOffset::plus);
        }

        boolean contains(Duration elapsed) {
            boolean afterStart = elapsed.compareTo(startOffset) >= 0;
            if (!afterStart) return false;
            Optional<Duration> end = endOffset();
            return end.map(e -> elapsed.compareTo(e) < 0).orElse(true);
        }
    }

    private final List<Window> windows;
    private final boolean carryLastIndefinitely; // "it needs to go on"
    private final int maxSuspensions;            // 0 or negative => no count cap

    public WindowedSuspensionPolicy(List<Window> windows,
                                    boolean carryLastIndefinitely,
                                    int maxSuspensions) {
        if (windows.isEmpty()) throw new IllegalArgumentException("At least one window required");
        this.windows = List.copyOf(windows);
        this.carryLastIndefinitely = carryLastIndefinitely;
        this.maxSuspensions = maxSuspensions;
    }

    @Override
    public Optional<Duration> nextDelay(Instant epoch, Instant now, int nextCount) {
        if (maxSuspensions > 0 && nextCount > maxSuspensions) return Optional.empty();

        Duration elapsed = Duration.between(epoch, now);
        Optional<Window> w = windowFor(elapsed);
        if (w.isEmpty()) {
            if (!carryLastIndefinitely) return Optional.empty();
            // After the last finite window → continue using the last window’s period forever
            return Optional.of(windows.getLast().period);
        }
        return Optional.of(w.get().period);
    }

    @Override
    public Optional<String> actionForResult(boolean success, Instant epoch, Instant when) {
        Duration elapsed = Duration.between(epoch, when);
        Optional<Window> w = windowFor(elapsed);
        Window window = w.orElse(windows.getLast());
        return Optional.of(success ? window.successAction : window.failureAction);
    }

    private Optional<Window> windowFor(Duration elapsed) {
        for (Window w : windows) {
            if (w.contains(elapsed)) return Optional.of(w);
        }
        return Optional.empty();
    }

    public static WindowedSuspensionPolicy from(CreationArgs.SuspensionSpecConfig cfg,
                                                boolean carryLastIndefinitely,
                                                int maxSuspensions) {
        List<Window> out = new ArrayList<>();
        Duration offset = Duration.ZERO;
        for (CreationArgs.SuspensionSpecConfig.Entry e : cfg.getEntries()) {
            DelaySpec d = DelaySpec.parse(e.getDelaySpec());
            Window w = new Window(
                    offset,
                    d.window(),
                    d.period(),
                    e.getSuccessAction(),
                    e.getFailureAction());
            out.add(w);
            if (d.window().isPresent()) {
                offset = offset.plus(d.window().get());
            } else {
                break;
            }
        }
        return new WindowedSuspensionPolicy(out, carryLastIndefinitely, maxSuspensions);
    }
}
