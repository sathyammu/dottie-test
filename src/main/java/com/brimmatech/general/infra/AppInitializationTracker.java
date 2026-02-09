package com.brimmatech.general.infra;


import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component public class AppInitializationTracker implements SmartLifecycle {
    private boolean running = false;

    @Override public void start() {
        // Initialization logic
        System.out.println("Application initialization completed");
        running = true;
    }

    @Override public void stop() {
        running = false;
    }

    @Override public boolean isRunning() {
        return running;
    }

    @Override public boolean isAutoStartup() {
        return true;
    }

    @Override public int getPhase() {
        return Integer.MAX_VALUE; // Ensure this starts last
    }
}