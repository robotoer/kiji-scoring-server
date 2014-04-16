package org.kiji.scoring.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.kiji.scoring.server.record.ServiceManagerConfiguration;

public class TestServiceManagerFactory {
  @Before
  public void setup() {
    // Start MiniYarnCluster.
    ServiceTestUtils.startMiniYarnCluster();
  }

  @After
  public void cleanup() {
    // Teardown MiniYarnCluster.
    ServiceTestUtils.stopMiniYarnCluster();
  }

  @Test
  public void testStartStop() {
    final ServiceManager manager = ServiceManagerFactory.INSTANCE.start(null);

    // Validate state.

    ServiceManagerFactory.INSTANCE.stop(manager);

    // Validate state.
  }

  @Test
  public void testConnect() {
    ServiceManagerFactory.INSTANCE.connect(null);

    // Validate state.
  }

  @Test
  public void testStartOver() {
    final ServiceManagerConfiguration managerConfiguration = new ServiceManagerConfiguration();

    ServiceManagerFactory.INSTANCE.start(managerConfiguration);
    ServiceManagerFactory.INSTANCE.start(managerConfiguration);

    // Validate state.
  }
}
