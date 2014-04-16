package org.kiji.scoring.server.yarn;

import org.kiji.scoring.server.ServiceTask;
import org.kiji.scoring.server.record.ServiceStatus;

/**
 * Responsible for providing the score/status interfaces of a service instance within a YARN
 * container.
 */
public class YarnServiceContainer implements ServiceTask<Object> {
  @Override
  public Object apply(final Object argument) {
    return null;
  }

  @Override
  public ServiceStatus status() {
    return null;
  }

  public static void main(final String[] args) {
    // Parse out launch arguments/configuration for the service instance execution.
  }
}
