package com.brimmatech.docflow.v2.task.globalapi.actions;

import java.util.Optional;

public final class ActionSpec {
    private final String type;
    private final String name;

    private ActionSpec(String type, String name) {
        this.type = type;
        this.name = name;
    }

    public String type() { return type; }
    public String name() { return name; }

    public static Optional<ActionSpec> parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Optional.empty();
        String[] parts = raw.split(":", 2);
        if (parts.length == 1) {
            return Optional.of(new ActionSpec(parts[0].trim(), ""));
        }
        return Optional.of(new ActionSpec(parts[0].trim(), parts[1].trim()));
    }

    @Override
    public String toString() {
        return "ActionSpec{" + type + ":" + name + '}';
    }
}
