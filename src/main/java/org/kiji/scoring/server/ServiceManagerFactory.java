package org.kiji.scoring.server;

import java.io.IOException;
import java.net.URI;

import org.apache.hadoop.yarn.exceptions.YarnException;

import org.kiji.scoring.server.record.ServiceManagerConfiguration;
import org.kiji.scoring.server.yarn.YarnServiceManagerFactory;

/**
 * Responsible for creating/starting/stopping service manager instances.
 */
public interface ServiceManagerFactory {
  ServiceManager start(final ServiceManagerConfiguration managerConfiguration) throws IOException, InterruptedException, YarnException;
  ServiceManager connect(final URI managerUri);
  void stop(final ServiceManager manager);
  void stop(final URI managerUri);
}
