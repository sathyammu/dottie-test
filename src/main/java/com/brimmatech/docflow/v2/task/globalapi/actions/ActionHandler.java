package com.brimmatech.docflow.v2.task.globalapi.actions;

import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;

import java.util.Map;

/**
 * Generic action to be performed.
 */
public interface ActionHandler {
    /**
     * Perform the action described by actionSpec for the given task and optional context map.
     *
     * @param actionSpec the right-hand side of the action token, e.g. "noLockObtainedTopic" for "mail:noLockObtainedTopic"
     * @param task the task for which the action is invoked
     * @param context arbitrary context (e.g. appendedResult JSON or parsed DTOs)
     * @return SUCCESS or FAILURE
     */
    ActionResult perform(String actionSpec, Task task, Map<String, Object> context);
}
