package org.kiji.scoring.server.yarn;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ClientRMProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.protocolrecords.SubmitApplicationRequest;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.ipc.YarnRPC;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.scoring.server.record.ServiceConfiguration;
import org.kiji.scoring.server.ServiceManager;
import org.kiji.scoring.server.ServiceManagerFactory;
import org.kiji.scoring.server.record.ServiceManagerConfiguration;

public class YarnServiceManagerFactory implements ServiceManagerFactory {
  private static final Logger LOG = LoggerFactory.getLogger(YarnServiceManagerFactory.class);

  public void launchApplicationMaster(
      final String command,
      final URI jarUri,
      final Configuration configuration,
      final FileSystem fs,
      final String applicationName,
      final int applicationMemory) throws IOException {
    final ClientRMProtocol applicationsManager;
    {
      final YarnConfiguration yarnConf = new YarnConfiguration(configuration);
      final InetSocketAddress rmAddress = NetUtils.createSocketAddr(
          yarnConf.get(YarnConfiguration.RM_ADDRESS, YarnConfiguration.DEFAULT_RM_ADDRESS)
      );
      LOG.info("Connecting to ResourceManager at " + rmAddress);

      final Configuration appsManagerServerConf;
      {
        appsManagerServerConf = new Configuration(configuration);
        // TODO: See if we can support YARN security.
//        appsManagerServerConf.setClass(
//            YarnConfiguration.YARN_SECURITY_INFO,
//            ClientRMSecurityInfo.class, SecurityInfo.class
//        );
      }
      final YarnRPC rpc = YarnRPC.create(yarnConf);
      applicationsManager = ((ClientRMProtocol) rpc.getProxy(
          ClientRMProtocol.class, rmAddress, appsManagerServerConf));
    }

    final ApplicationId applicationId;
    {
      final GetNewApplicationRequest request = Records.newRecord(GetNewApplicationRequest.class);
      final GetNewApplicationResponse response = applicationsManager.getNewApplication(request);
      applicationId = response.getApplicationId();
      LOG.info("Got new ApplicationId=" + applicationId);
    }

    final ApplicationSubmissionContext applicationContext;
    {
      // Create a new ApplicationSubmissionContext
      applicationContext = Records.newRecord(ApplicationSubmissionContext.class);
      // set the ApplicationId
      applicationContext.setApplicationId(applicationId);
      // set the application name
      applicationContext.setApplicationName(applicationName);

      // Create a new container launch context for the AM's container
      final ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);

      // Define the local resources required
      final Map<String, LocalResource> localResources = Maps.newHashMap();
      // Lets assume the jar we need for our ApplicationMaster is available in
      // HDFS at a certain known path to us and we want to make it available to
      // the ApplicationMaster in the launched container
      final Path jarPath = new Path(jarUri); // <- known path to jar file
      final FileStatus jarStatus = fs.getFileStatus(jarPath);
      final LocalResource amJarResource = Records.newRecord(LocalResource.class);
      amJarResource.setType(LocalResourceType.FILE);
      amJarResource.setVisibility(LocalResourceVisibility.APPLICATION);
      amJarResource.setResource(ConverterUtils.getYarnUrlFromPath(jarPath));
      amJarResource.setTimestamp(jarStatus.getModificationTime());
      amJarResource.setSize(jarStatus.getLen());
      amContainer.setLocalResources(localResources);

      localResources.put("AppMaster.jar", amJarResource);

      // Set up the environment needed for the launch context
      final Map<String, String> env = Maps.newHashMap();
      // For example, we could setup the classpath needed.
      String classPathEnv = "$CLASSPATH:./*:";
      env.put("CLASSPATH", classPathEnv);
      amContainer.setEnvironment(env);

      // Construct the command to be executed on the launched container
      final ArrayList<String> commands = Lists.newArrayList(
          command +
              " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
              " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
      );

      // Set the command array into the container spec
      amContainer.setCommands(commands);

      // Define the resource requirements for the container
      final Resource capability = Records.newRecord(Resource.class);
      capability.setMemory(applicationMemory);
      amContainer.setResource(capability);

      // Set the container launch content into the ApplicationSubmissionContext
      applicationContext.setAMContainerSpec(amContainer);
    }


    // Create the request to send to the ApplicationsManager
    final SubmitApplicationRequest applicationRequest =
        Records.newRecord(SubmitApplicationRequest.class);
    applicationRequest.setApplicationSubmissionContext(applicationContext);

    // Submit the application to the ApplicationsManager
    applicationsManager.submitApplication(applicationRequest);
  }

  public static void main(final String[] args) throws IOException {
    // TODO: Parse these from the command line.
    new YarnServiceManagerFactory().launchApplicationMaster(
        null, null, null, null, null, 0
    );
  }

  public static void test() {
    // Deploy a service (java api).
    final ServiceConfiguration serviceConfiguration = null;
    final URI jarUri = null;

    final ServiceManager serviceManager = null;

    serviceManager.deploy(
        // Service configuration.
        serviceConfiguration,

        // Jar uri.
        jarUri,

        // Number of instances.
        1
    );
    // Deploy a service (cli api).
    commandLine(
        "deploy-service",

        // Service configuration.
        "--type=" + serviceConfiguration.getType(),

        // Jar uri.
        "--jar-uri=" + jarUri.toString(),

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

  @Override
  public ServiceManager start(final ServiceManagerConfiguration configuration) throws IOException {
    launchApplicationMaster(null, null, null, null, null, 0);
  }

  @Override
  public ServiceManager connect(final URI managerUri) {
    return null;
  }

  @Override
  public void stop(final ServiceManager manager) {

  }

  @Override
  public void stop(final URI managerUri) {

  }
}
