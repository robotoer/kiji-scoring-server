package org.kiji.scoring.server;

import java.net.URI;

import org.kiji.scoring.server.record.ServiceConfiguration;

/**
 * The service manager is responsible for coordinating the deployment/undeployment of services.
 */
public interface ServiceManager {
  <T> ServiceTask<T> deploy(
      final ServiceConfiguration configuration,
      final URI jarUri,
      final int instances
  );
  void undeployService(final String type);
  void undeployService(final ServiceTask service);
  void undeployServiceInstance(final String instanceId);

  void listServices();
  void listServiceInstances();
}
