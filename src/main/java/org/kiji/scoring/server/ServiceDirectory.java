package org.kiji.scoring.server;

import java.util.List;

/**
 * Represents a way to retrieve the currently deployed services.
 */
public interface ServiceDirectory {
  List<ServiceTask<?>> getServices();

  <T> ServiceTask<T> getService(final String serviceId);
}
