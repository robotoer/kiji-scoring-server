package org.kiji.scoring.server;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.kiji.scoring.server.record.ServiceManagerConfiguration;

public class TestServiceManager {
//  private ServiceManager mManager;
//  private ServiceManagerFactory mServiceManagerFactory;
//
//  @Before
//  public void setup() throws IOException {
//    // Start MiniYarnCluster.
////    ServiceTest.startMiniYarnCluster();
//
//    // Start the ServiceManager.
//    mServiceManagerFactory = new ServiceManagerFactory(getConfig());
//    mManager = mServiceManagerFactory.start(new ServiceManagerConfiguration(port, name, command, memory, cores));
//  }
//
//  @After
//  public void cleanup() {
//    // Stop the ServiceManager.
//    new ServiceManagerFactory(getConfig()).stop(mManager);
//
//    // Teardown MiniYarnCluster.
////    ServiceTest.stopMiniYarnCluster(null);
//  }
//
//  @Test
//  public void testDeployUndeploy() throws IOException {
//    mManager.deploy(null, null, 0);
//    mManager.deploy(null, null, 0);
//
//    // Validate state.
//    mManager.listServices();
//    mManager.listServiceInstances();
//
//
//    mManager.undeployService("");
//
//    // Validate state.
//    mManager.listServices();
//    mManager.listServiceInstances();
//
//
//    mManager.undeployServiceInstance("");
//
//    // Validate state.
//    mManager.listServices();
//    mManager.listServiceInstances();
//  }
//
//  @Test
//  public void testDeployOver() throws IOException {
//    mManager.deploy(null, null, 0);
//    mManager.deploy(null, null, 0);
//
//    // Validate state.
//    mManager.listServices();
//    mManager.listServiceInstances();
//  }
//
//  @Test
//  public void testUndeployNonExistentService() {
//    mManager.undeployService("");
//
//    // Validate state.
//    mManager.listServices();
//    mManager.listServiceInstances();
//  }
//
//  public void testUndeployNonExistentInstance() {
//    mManager.undeployServiceInstance("");
//
//    // Validate state.
//    mManager.listServices();
//    mManager.listServiceInstances();
//  }
//
//  @Test
//  public void testList() throws IOException {
//    mManager.deploy(null, null, 0);
//    mManager.deploy(null, null, 0);
//
//    mManager.listServices();
//    mManager.listServiceInstances();
//  }
}
