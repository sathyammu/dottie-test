package com.brimmatech.docflow.v2.task.listeners;

import com.brimmatech.docflow.v2.services.TaskUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.TaskDecision;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class BatchedItemCollector<T> {
    protected TaskUtils taskUtils;

    @Autowired
    public final void setTaskUtils(TaskUtils taskUtils) {
        this.taskUtils = taskUtils;
    }
    BlockingQueue<T> queue = new LinkedBlockingQueue<>(); // Really the 'batch size' *
    // We need a explicit lock here as we are having `addAll` which might be
    // interleaved across threads.
    private final ReentrantLock additionLock = new ReentrantLock(true);

    public void addAll(Collection<T> items) {
        additionLock.lock();
        try {
            items.forEach(t -> {
                try {
                    queue.put(t);
                } catch (InterruptedException e) {
                    log.error("Unable to add item {} to queue", t, e);
                }
            });
        } catch (Exception e) {
            log.error("Got Exception while adding collection to queue", e);
        } finally {
            additionLock.unlock();
        }

    }

    public abstract void onComplete(String topic, String trace, Map<Task, TaskDecision> decisions);

    public ArrayList<T> drainToList() {
        val candidates = new ArrayList<T>();
        queue.drainTo(candidates);
        return candidates;
    }
}
