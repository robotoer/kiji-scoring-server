package org.kiji.scoring.server;

import org.kiji.scoring.server.record.ServiceStatus;

/**
 * Represents a connection to a service executor.
 */
public interface ServiceTask<T> {
  T apply(final T argument);

  ServiceStatus status();
}
