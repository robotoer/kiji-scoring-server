package org.kiji.scoring.server;

import java.util.Map;

public class ZookeeperServiceDirectory implements ServiceDirectory {
  @Override
  public ServiceManager getServiceManager() {
    return null;
  }

  @Override
  public void registerServiceManager(final ServiceManager manager) {
    manager.getAdminUrl();
  }

  @Override
  public Map<String, ServiceTask<?>> getServices() {
    return null;
  }

  @Override
  public void registerService(final String serviceId, final ServiceManager manager) {

  }

  @Override
  public void registerServices(final Map<String, ServiceManager> managers) {

  }
}
