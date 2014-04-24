package org.kiji.scoring.server;

import java.net.URL;

import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.server.MiniYARNCluster;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.ResourceScheduler;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.fifo.FifoScheduler;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.scoring.server.record.ServiceConfiguration;
import org.kiji.scoring.server.record.ServiceManagerConfiguration;
import org.kiji.scoring.server.yarn.YarnServiceManagerFactory;
import org.kiji.scoring.server.yarn.YarnServiceMaster;

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
  public void startMiniYarnCluster() throws Exception {
    final YarnConfiguration configuration = new YarnConfiguration();
    configuration.setInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB, 64);
    configuration.setClass(
        YarnConfiguration.RM_SCHEDULER,
        FifoScheduler.class,
        ResourceScheduler.class
    );

    mCluster = new MiniYARNCluster("tempTest", 2, 1, 1);
    mCluster.init(configuration);
    mCluster.start();
  }

//  @After
  public void stopMiniYarnCluster() {
    mCluster.stop();
  }

  public static void main(final String[] args) throws Exception {
//    // Setup ServiceProvider.
//    {
//      // Setup a zookeeper connection.
//      mCuratorClient =
//          CuratorFrameworkFactory.newClient(curatorUrl, CURATOR_RETRY_POLICY);
//
//      mThisInstance = ServiceInstance
//          .<YarnServiceMaster.ServiceMasterDetails>builder()
//          .id(masterId)
//          .name(CURATOR_SERVICE_NAME)
//          .address(masterAddress)
//          .port(masterPort)
//          .build();
//
//      mJsonSerializer =
//          new JsonInstanceSerializer<YarnServiceMaster.ServiceMasterDetails>(YarnServiceMaster.ServiceMasterDetails.class);
//    }
//
//    // Register with Curator's service discovery mechanism.
//    ServiceDiscoveryBuilder
//        .builder(YarnServiceMaster.ServiceMasterDetails.class)
//        .client(mCuratorClient)
//        .basePath(BASE_SERVICE_DISCOVERY_PATH)
//        .serializer(mJsonSerializer)
//        .thisInstance(mThisInstance)
//        .build();
//
//    final ServiceDiscovery<YarnServiceMaster.ServiceMasterDetails> serviceDiscovery = getServiceDiscovery();
//    try {
//      // Does this actually register the application master?
//      serviceDiscovery.start();
//      serviceDiscovery.registerService(mThisInstance);
//    } finally {
//      serviceDiscovery.close();
//    }

    simple(new YarnConfiguration());
  }

  public static void simple(final YarnConfiguration baseConfig) throws Exception {
    final String appName = "test-yarn-application";
//    final String appCommand = "echo hello world";
    final int appMemory = 256;
    final int appPort = 8080;
    final int appCores = 1;
    final String curatorAddress = "localhost:2181";

    final YarnServiceManagerFactory managerFactory = new YarnServiceManagerFactory(baseConfig);

    final ServiceManagerConfiguration managerConfiguration =
        new ServiceManagerConfiguration(appName, appMemory, appPort, appCores, curatorAddress);
    final ServiceManager manager = managerFactory.start(managerConfiguration);
//    manager.listServices();
  }

  @Test
  public void tempTest() throws Exception {
//    final String appName = "test-yarn-application";
//    final String appCommand = "echo hello world";
//    final int appMemory = 64;
//    final int appPort = 8080;
//    final int appCores = 1;

    startMiniYarnCluster();

//    final YarnConfiguration baseConfig = getConfig();
//    final YarnServiceManagerFactory managerFactory = new YarnServiceManagerFactory(baseConfig);
//
//    final ServiceManagerConfiguration managerConfiguration =
//        new ServiceManagerConfiguration(appName, appCommand, appMemory, appPort, appCores);
//    final ServiceManager manager = managerFactory.start(managerConfiguration);
////    manager.listServices();

    simple(getConfig());

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
