package org.kiji.scoring.server;

import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ClientRMProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationRequest;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnRemoteException;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.apache.hadoop.yarn.util.Records;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.scoring.server.record.ServiceConfiguration;

public class ServiceTest {
  private static final Logger LOG = LoggerFactory.getLogger(ServiceTest.class);

  private MiniYARNCluster mCluster;

  public MiniYARNCluster getCluster() {
    return mCluster;
  }

  public YarnConfiguration getConfig() {
    return new YarnConfiguration(mCluster.getConfig());
  }

//  @Before
  public void startMiniYarnCluster() {
    final YarnConfiguration configuration = new YarnConfiguration();
    configuration.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 64);
    configuration.setClass(
        YarnConfiguration.RM_SCHEDULER,
        FifoScheduler.class,
        ResourceScheduler.class
    );

    mCluster = new MiniYARNCluster("tempTest", 10, 1, 1);
    mCluster.init(configuration);
    mCluster.start();
  }

//  @After
  public void stopMiniYarnCluster() {
    mCluster.stop();
  }

  @Test
  public void tempTest() throws YarnRemoteException {
    final String applicationName = "test-yarn-application";
    final String command = "echo hello world";
    final int applicationMemory = 64;

    startMiniYarnCluster();

    final YarnConfiguration baseConfig = getConfig();

    stopMiniYarnCluster();
  }




































  public static void test() throws Exception {
    // Deploy a service (java api).
    final ServiceConfiguration serviceConfiguration = null;
    final URL jarUrl = null;

    final ServiceManager serviceManager = null;

    serviceManager.deploy(
        // Service configuration.
        serviceConfiguration,

        // Jar url.
        jarUrl,

        // Number of instances.
        1
    );
    // Deploy a service (cli api).
    commandLine(
        "deploy-service",

        // Service configuration.
        "--type=" + serviceConfiguration.getType(),

        // Jar url.
        "--jar-url=" + jarUrl.toString(),

        // Number of instances.
        "--instances=1"
    );

    // Undeploy a service (java api).
    serviceManager.undeployService(
        "service-type"
    );
    serviceManager.undeployServiceInstance(
        "service-id"
    );
    // Undeploy a service (cli api).
    commandLine("undeploy-service", serviceConfiguration.getType());
    commandLine("undeploy-instance", "service-type.0");

    // List deployed services (java api).
    serviceManager.listServices();
    serviceManager.listServiceInstances();
    // List deployed services (cli api).
    commandLine("list-services");
    commandLine("list-service-instances");
  }

  private static void commandLine(final String... arguments) {
  }
}
