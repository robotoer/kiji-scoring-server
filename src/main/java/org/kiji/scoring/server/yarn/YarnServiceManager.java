package org.kiji.scoring.server.yarn;

import java.io.IOException;
import java.net.URL;

import org.kiji.scoring.server.record.ServiceConfiguration;
import org.kiji.scoring.server.ServiceManager;
import org.kiji.scoring.server.ServiceTask;

/**
 * Delegates calls to YarnServiceMaster via an Http pipe.
 */
public class YarnServiceManager implements ServiceManager {
  final private URL mMasterUrl;

  public YarnServiceManager(final URL masterUrl) {
    mMasterUrl = masterUrl;
  }

  @Override
  public <T> ServiceTask<T> deploy(
      final ServiceConfiguration configuration,
      final URL jarUri,
      final int instances
  ) throws IOException {
//    HttpURLConnection.mMasterUrl.openConnection()
//    IOUtils.toString(mMasterUrl.openStream())
    return null;
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
