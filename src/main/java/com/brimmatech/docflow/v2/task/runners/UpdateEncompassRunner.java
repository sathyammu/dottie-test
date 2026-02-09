package com.brimmatech.docflow.v2.task.runners;

import com.brimmatech.docflow.v2.task.RunnerInjections;
import com.brimmatech.general.config.TemplateConfig.TOPICS;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UpdateEncompassRunner extends BaseRunner {
    public UpdateEncompassRunner(RunnerInjections runnerInjections) {
    super(runnerInjections);
  }

  public void start() {
    try {
      this.sequentialTaskHandler(TOPICS.UPDATE_ENCOMPASS, 5).start(5, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
