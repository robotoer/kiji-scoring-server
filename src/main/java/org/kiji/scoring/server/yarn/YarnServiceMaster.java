package org.kiji.scoring.server.yarn;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

import com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.AMRMProtocol;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterRequest;
import org.apache.hadoop.yarn.api.protocolrecords.RegisterApplicationMasterResponse;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnRemoteException;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.scoring.server.record.ServiceConfiguration;
import org.kiji.scoring.server.ServiceManager;

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
  private static final Logger LOG = LoggerFactory.getLogger(YarnServiceMaster.class);

  public YarnServiceMaster() throws YarnRemoteException {
    // Register with ResourceManager.
    connectToResourceManager(null, null, 0, null);
  }

  private void connectToResourceManager(
      final Configuration configuration,
      final String appMasterHostname,
      final int appMasterRpcPort,
      final String appMasterTrackingUrl
  ) throws YarnRemoteException {
    final Map<String, String> environment = System.getenv();
    final String containerIdString = Preconditions.checkNotNull(
        environment.get(ApplicationConstants.AM_CONTAINER_ID_ENV),
        "ContainerId not set in the environment"
    );
    final ContainerId containerId = ConverterUtils.toContainerId(containerIdString);
    final ApplicationAttemptId appAttemptID = containerId.getApplicationAttemptId();


    // Connect to the Scheduler of the ResourceManager.
    final YarnConfiguration yarnConf = new YarnConfiguration(configuration);
    final InetSocketAddress rmAddress =
        NetUtils.createSocketAddr(yarnConf.get(
            YarnConfiguration.RM_SCHEDULER_ADDRESS,
            YarnConfiguration.DEFAULT_RM_SCHEDULER_ADDRESS
        ));
    LOG.info("Connecting to ResourceManager at " + rmAddress);
    final YarnRPC rpc = YarnRPC.create(yarnConf);
    final AMRMProtocol resourceManager =
        (AMRMProtocol) rpc.getProxy(AMRMProtocol.class, rmAddress, configuration);

    // Register the AM with the RM
    final RegisterApplicationMasterRequest appMasterRequest =
        Records.newRecord(RegisterApplicationMasterRequest.class);
    appMasterRequest.setApplicationAttemptId(appAttemptID);
    appMasterRequest.setHost(appMasterHostname);
    appMasterRequest.setRpcPort(appMasterRpcPort);
    appMasterRequest.setTrackingUrl(appMasterTrackingUrl);

    // The registration response is useful as it provides information about the
    // cluster.
    // Similar to the GetNewApplicationResponse in the client, it provides
    // information about the min/mx resource capabilities of the cluster that
    // would be needed by the ApplicationMaster when requesting for containers.
    final RegisterApplicationMasterResponse response =
        resourceManager.registerApplicationMaster(appMasterRequest);
  }

  @Override
  public void deploy(
      final ServiceConfiguration configuration,
      final URI jarUri,
      final int instances
  ) {
    // Allocate new resource containers to execute the service.
  }

  @Override
  public void undeployService(final String type) {
    // Get a handle to the required resource containers to kill their corresponding service.
  }

  @Override
  public void undeployServiceInstance(final String instanceId) {
    // Get a handle to the required resource containers to kill their corresponding service instance.
  }

  @Override
  public void listServices() {
    // Dump:
    //  - Services that the ServiceManager is aware of.
    //  - Actual status of the corresponding resource containers.
  }

  @Override
  public void listServiceInstances() {
    // Dump:
    //  - Instances that the Service is composed of.
    //  - Actual status of the corresponding service instances.
  }

  // ApplicationMaster logic
  public static void main(final String[] args) throws Exception {
    new YarnServiceMaster().listenToApi(0);
  }

  private void listenToApi(final int port) throws Exception {
    // Startup jetty.
    final Server server = new Server(port);

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

        undeployService(null);
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

    server.setHandler(handlers);
    server.start();


    // Setup a heartbeat to the ResourceManager.

  }
}
