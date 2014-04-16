package org.kiji.scoring.server.yarn;

import org.kiji.scoring.server.ServiceTask;
import org.kiji.scoring.server.record.ServiceStatus;

/**
 * Provides a way to query a running service instance.
 */
public class YarnServiceTask<T> implements ServiceTask<T> {
  @Override
  public T apply(final T argument) {
    return null;
  }

  @Override
  public ServiceStatus status() {
    return null;
  }
}
