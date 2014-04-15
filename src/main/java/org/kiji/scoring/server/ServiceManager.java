package org.kiji.scoring.server;

import java.net.URI;

/**
 * The service manager is responsible for coordinating the deployment/undeployment of services.
 */
public interface ServiceManager {
  void deploy(
      final ServiceConfiguration configuration,
      final URI jarUri,
      final int instances
  );
  void undeployService(final String type);
  void undeployServiceInstance(final String instanceId);

  void listServices();
  void listServiceInstances();

  void commandLine(final String... arguments);
}
