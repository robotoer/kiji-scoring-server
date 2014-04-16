package org.kiji.scoring.server.yarn;

import java.net.URI;

import org.kiji.scoring.server.record.ServiceConfiguration;
import org.kiji.scoring.server.ServiceManager;
import org.kiji.scoring.server.ServiceTask;

/**
 * Delegates calls to YarnServiceMaster via an Http pipe.
 */
public class YarnServiceManager implements ServiceManager {
  final private URI mMasterUri;

  public YarnServiceManager(final URI masterUri) {
    mMasterUri = masterUri;
  }

  @Override
  public <T> ServiceTask<T> deploy(
      final ServiceConfiguration configuration,
      final URI jarUri,
      final int instances
  ) {
    mMasterUri.
  }

  @Override
  public void undeployService(final String type) {

  }

  @Override
  public void undeployService(final ServiceTask service) {

  }

  @Override
  public void undeployServiceInstance(final String instanceId) {

  }

  @Override
  public void listServices() {

  }

  @Override
  public void listServiceInstances() {

  }
}
