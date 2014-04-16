package org.kiji.scoring.server;

import java.net.URI;

import org.kiji.scoring.server.record.ServiceManagerConfiguration;
import org.kiji.scoring.server.yarn.YarnServiceManagerFactory;

/**
 * Responsible for creating/starting/stopping service manager instances.
 */
public interface ServiceManagerFactory {
  public static ServiceManagerFactory INSTANCE = new YarnServiceManagerFactory();

  ServiceManager start(final ServiceManagerConfiguration managerConfiguration);
  ServiceManager connect(final URI managerUri);
  void stop(final ServiceManager manager);
  void stop(final URI managerUri);
}
