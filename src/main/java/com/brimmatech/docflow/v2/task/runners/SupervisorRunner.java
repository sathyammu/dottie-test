package com.brimmatech.docflow.v2.task.runners;

import com.brimmatech.docflow.v2.task.RunnerInjections;
import com.brimmatech.general.config.TemplateConfig.TOPICS;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class SupervisorRunner extends BaseRunner {

  public SupervisorRunner(RunnerInjections injections) {
    super(injections);
  }

  public void start() {
    try {
      this.sequentialTaskHandler(TOPICS.NOOP_SUPERVISOR, 1).start(5, TimeUnit.SECONDS);
    } catch (InterruptedException | TimeoutException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
