package org.kiji.scoring.server;

import java.util.Map;

/**
 * Represents a way to retrieve the currently deployed services.
 */
public interface ServiceDirectory {
  ServiceManager getServiceManager();

  void registerServiceManager(final ServiceManager manager);

  Map<String, ServiceTask<?>> getServices();

  void registerService(final String serviceId, final ServiceManager manager);
  void registerServices(final Map<String, ServiceManager> managers);
}
