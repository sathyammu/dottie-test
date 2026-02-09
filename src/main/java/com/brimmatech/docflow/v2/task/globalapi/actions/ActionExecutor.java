package com.brimmatech.docflow.v2.task.globalapi.actions;

import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry-based executor. Register handlers by type (e.g. "mail").
 */

@Service
public class ActionExecutor {

    private final Map<String, ActionHandler> handlers = new HashMap<>();

    public void register(String type, ActionHandler handler) {
        handlers.put(type, handler);
    }

    public Optional<ActionResult> perform(String rawAction, Task task, Map<String, Object> context) {
        Optional<ActionSpec> specOpt = ActionSpec.parse(rawAction);
        if (specOpt.isEmpty()) return Optional.empty();

        ActionSpec spec = specOpt.get();
        ActionHandler handler = handlers.get(spec.type());
        if (handler == null) return Optional.of(ActionResult.FAILURE);

        return Optional.of(handler.perform(spec.name(), task, context));
    }
}
