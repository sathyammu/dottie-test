package com.brimmatech.docflow.v2.task.metrics;

import java.util.LinkedList;
import java.util.Queue;

public class MovingAverage {
    private final int windowSize;
    private final Queue<Long> window;
    private long sum;

    public MovingAverage(int windowSize) {
        this.windowSize = windowSize;
        this.window = new LinkedList<>();
        this.sum = 0;
    }

    public void add(long value) {
        sum += value;
        window.add(value);
        if (window.size() > windowSize) {
            sum -= window.poll();
        }
    }

    public double getAverage() {
        if (window.isEmpty()) {
            return 0;
        }
        return (double) sum / window.size();
    }
}
