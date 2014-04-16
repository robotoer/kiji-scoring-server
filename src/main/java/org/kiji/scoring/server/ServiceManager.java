package org.kiji.scoring.server;

import java.io.IOException;
import java.net.URL;

import org.kiji.scoring.server.record.ServiceConfiguration;

/**
 * The service manager is responsible for coordinating the deployment/undeployment of services.
 */
public interface ServiceManager {
  <T> ServiceTask<T> deploy(
      final ServiceConfiguration configuration,
      final URL jarUri,
      final int instances
  ) throws IOException;
  void undeployService(final String type);
  void undeployService(final ServiceTask service);
  void undeployServiceInstance(final String instanceId);

  void listServices();
  void listServiceInstances();
}
