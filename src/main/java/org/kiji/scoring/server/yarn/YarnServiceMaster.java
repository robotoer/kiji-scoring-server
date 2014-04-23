package org.kiji.scoring.server.yarn;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

import com.google.common.base.Objects;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.InstanceSerializer;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.NMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.scoring.server.ServiceManager;
import org.kiji.scoring.server.ServiceTask;
import org.kiji.scoring.server.record.ServiceConfiguration;

// Should be an ApplicationMaster.
//
// ApplicationManager:
//  - Setup:
//    - Integrate/connect to ResourceManager
//  - Normal:
//    - Listen to http admin APIs
//  - Cleanup:
//    - Deregister from ResourceManager
//
//  - Operations:
//    -
public class YarnServiceMaster implements ServiceManager {
  public static final String YARN_SERVICE_MANAGER_JAVA_FLAGS = "-Xmx256M";
  private static final Logger LOG = LoggerFactory.getLogger(YarnServiceMaster.class);

  public static final String BASE_SERVICE_DISCOVERY_PATH = "/org/kiji/services/";
  public static final int YARN_HEARTBEAT_INTERVAL_MS = 500;
  private static final String CURATOR_SERVICE_NAME = "service-master";
  private static final RetryPolicy CURATOR_RETRY_POLICY = new ExponentialBackoffRetry(1000, 3);

  // Yarn fields.
  private final AMRMClientAsync<AMRMClient.ContainerRequest> mResourceManagerClient;
  private final NMClient mNodeManagerClient;

  // Curator fields.
  private final CuratorFramework mCuratorClient;
  private final InstanceSerializer<ServiceMasterDetails> mJsonSerializer;

  // Jetty fields.
  private final Server mServer;
  private final ServiceInstance<ServiceMasterDetails> mThisInstance;

  public YarnServiceMaster(
      final String masterId,
      final String masterAddress,
      final int masterPort,
      final String curatorUrl,
      final YarnConfiguration yarnConf
  ) throws Exception {
    // Setup Yarn.
    {
      // Initialize ResourceManager and NodeManager. AMRMClientAsync is used here because it runs a
      // heartbeat thread.
      mResourceManagerClient = AMRMClientAsync.createAMRMClientAsync(YARN_HEARTBEAT_INTERVAL_MS, null);
      mResourceManagerClient.init(yarnConf);
      mResourceManagerClient.start();

      // TODO: Should these be closed?
      mNodeManagerClient = NMClient.createNMClient();
      mNodeManagerClient.init(yarnConf);
      mNodeManagerClient.start();
    }


    // Setup ServiceProvider.
    {
      // Setup a zookeeper connection.
      mCuratorClient =
          CuratorFrameworkFactory.newClient(curatorUrl, CURATOR_RETRY_POLICY);

      mThisInstance = ServiceInstance
          .<ServiceMasterDetails>builder()
          .id(masterId)
          .name(CURATOR_SERVICE_NAME)
          .address(masterAddress)
          .port(masterPort)
          .build();

      mJsonSerializer =
          new JsonInstanceSerializer<ServiceMasterDetails>(ServiceMasterDetails.class);
    }


    // Setup Jetty.
    {
      mServer = new Server(masterPort);

      final HandlerList handlers = new HandlerList();

      // deploy
      handlers.addHandler(new AbstractHandler() {
        @Override
        public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
        ) throws IOException, ServletException {
          if (!target.equalsIgnoreCase("/deploy")) {
            return;
          }
          baseRequest.setHandled(true);

          deploy(null, null, 0);
        }
      });

      // undeploy
      handlers.addHandler(new AbstractHandler() {
        @Override
        public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
        ) throws IOException, ServletException {
          if (!target.equalsIgnoreCase("/undeploy-service")) {
            return;
          }
          baseRequest.setHandled(true);

          undeployService((String) null);
        }
      });
      handlers.addHandler(new AbstractHandler() {
        @Override
        public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
        ) throws IOException, ServletException {
          if (!target.equalsIgnoreCase("/undeploy-instance")) {
            return;
          }
          baseRequest.setHandled(true);

          undeployServiceInstance(null);
        }
      });

      // list
      handlers.addHandler(new AbstractHandler() {
        @Override
        public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
        ) throws IOException, ServletException {
          if (!target.equalsIgnoreCase("/list-services")) {
            return;
          }
          baseRequest.setHandled(true);

          listServices();
        }
      });
      handlers.addHandler(new AbstractHandler() {
        @Override
        public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
        ) throws IOException, ServletException {
          if (!target.equalsIgnoreCase("/list-instances")) {
            return;
          }
          baseRequest.setHandled(true);

          listServiceInstances();
        }
      });

      mServer.setHandler(handlers);
    }
  }

  private ServiceDiscovery<ServiceMasterDetails> getServiceDiscovery() {
    return ServiceDiscoveryBuilder
        .builder(ServiceMasterDetails.class)
        .client(mCuratorClient)
        .basePath(BASE_SERVICE_DISCOVERY_PATH)
        .serializer(mJsonSerializer)
        .thisInstance(mThisInstance)
        .build();
  }

  public void start() throws Exception {
    // Startup jetty.
    mServer.start();

    // Register with ResourceManager.
    // TODO: Are these supposed to not be blank values?
    LOG.info("Registering YarnServiceMaster...");
    mResourceManagerClient.registerApplicationMaster("", 0, "");

    // Register with Curator's service discovery mechanism.
    final ServiceDiscovery<ServiceMasterDetails> serviceDiscovery = getServiceDiscovery();
    try {
      // Does this actually register the application master?
      serviceDiscovery.start();
      serviceDiscovery.registerService(mThisInstance);
    } finally {
      serviceDiscovery.close();
    }
  }

  public void join() throws InterruptedException {
    mServer.join();
  }

  public void stop() throws Exception {
    // Unregister with Curator's service discovery mechanism.
    final ServiceDiscovery<ServiceMasterDetails> serviceDiscovery = getServiceDiscovery();
    try {
      serviceDiscovery.start();
      serviceDiscovery.unregisterService(mThisInstance);
    } finally {
      serviceDiscovery.close();
    }

    // Shutdown Jetty.
    mServer.stop();

    // Unregister with ResourceManager.
    LOG.info("Unregistering YarnServiceMaster...");
    mResourceManagerClient.unregisterApplicationMaster(FinalApplicationStatus.SUCCEEDED, "", "");
  }

  @Override
  public <T> ServiceTask<T> deploy(
      final ServiceConfiguration configuration,
      final URL jarUri,
      final int instances
  ) {
    // Allocate new resource containers to execute the service.
    LOG.info("Received a deploy call!");
    return null;
  }

  @Override
  public void undeployService(final String type) {
    // Get a handle to the required resource containers to kill their corresponding service.
    LOG.info("Received an undeploy call!");
  }

  @Override
  public void undeployService(final ServiceTask service) {
    undeployService(service.getType());
  }

  @Override
  public void undeployServiceInstance(final String instanceId) {
    // Get a handle to the required resource containers to kill their corresponding service instance.
    LOG.info("Received an undeploy call!");
  }

  @Override
  public void listServices() {
    // Dump:
    //  - Services that the ServiceManager is aware of.
    //  - Actual status of the corresponding resource containers.
    LOG.info("Received a list call!");
  }

  @Override
  public void listServiceInstances() {
    // Dump:
    //  - Instances that the Service is composed of.
    //  - Actual status of the corresponding service instances.
    LOG.info("Received a list call!");
  }

  // ApplicationMaster logic
  public static void main(final String[] args) throws Exception {
    final YarnConfiguration yarnConf = new YarnConfiguration();
    final String masterAddress = getHostname();

    // Parse cli arguments.
    final String masterId = args[0];
    final int masterPort = Integer.parseInt(args[1]);
    final String curatorAddress = args[2];
//    final String masterId = "service-master-1";
//    final int masterPort = 8080;
//    final String curatorAddress = "";

    final YarnServiceMaster serviceMaster = new YarnServiceMaster(
        masterId,
        masterAddress,
        masterPort,
        curatorAddress,
        yarnConf
    );
    LOG.info("Starting %s...", serviceMaster.toString());
    serviceMaster.start();
    serviceMaster.join();
  }

  private static String getHostname() {
    try {
      final String result = InetAddress.getLocalHost().getHostName();
      if (StringUtils.isNotEmpty(result)) {
        return result;
      }
    } catch (UnknownHostException e) {
      // failed;  try alternate means.
      LOG.warn("Failed to resolve hostname from ip address. Is your DNS setup correctly?");
    }

    // try environment properties.
    final String windowsHostname = System.getenv("COMPUTERNAME");
    if (windowsHostname != null) {
      return windowsHostname;
    }
    final String linuxHostname = System.getenv("HOSTNAME");
    if (linuxHostname != null) {
      return linuxHostname;
    }

    // undetermined.
    return null;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("mResourceManagerClient", mResourceManagerClient)
        .add("mNodeManagerClient", mNodeManagerClient)
        .add("mCuratorClient", mCuratorClient)
        .add("mJsonSerializer", mJsonSerializer)
        .add("mServer", mServer)
        .add("mThisInstance", mThisInstance)
        .toString();
  }

  public static String prepareArgs(
      final String masterName,
      final int masterPort,
      final String curatorAddress
  ) {
    return String.format("%s %d %s", masterName, masterPort, curatorAddress);
  }

  public static class ServiceMasterDetails {
  }
}
