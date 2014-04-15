package org.kiji.scoring.server;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.SecurityInfo;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ClientRMProtocol;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.security.client.ClientRMSecurityInfo;
import org.apache.hadoop.yarn.service.Service;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

// Should be an ApplicationMaster.
public class YarnServiceManager implements ServiceManager {
  public static void main(final String[] args) {

  }
  public void launchApplicationMaster2(
      final String command,
      final URI jarUri
  ) {
    ClientRMProtocol applicationsManager;
    YarnConfiguration yarnConf = new YarnConfiguration(conf);
    InetSocketAddress rmAddress =
        NetUtils.createSocketAddr(yarnConf.get(
            YarnConfiguration.RM_ADDRESS,
            YarnConfiguration.DEFAULT_RM_ADDRESS));
    LOG.info("Connecting to ResourceManager at " + rmAddress);
    Configuration appsManagerServerConf = new Configuration(conf);
    appsManagerServerConf.setClass(
        YarnConfiguration.YARN_SECURITY_INFO,
        ClientRMSecurityInfo.class, SecurityInfo.class);
    applicationsManager = ((ClientRMProtocol) rpc.getProxy(
        ClientRMProtocol.class, rmAddress, appsManagerServerConf));
  }

  public void launchApplicationMaster(
      final String command,
      final URI jarUri
  ) throws IOException {
    // Create yarnClient
    YarnConfiguration conf = new YarnConfiguration();
    YarnClient yarnClient = YarnClient.createYarnClient();
    yarnClient.init(conf);
    yarnClient.start();

    // Create application via yarnClient
    YarnClientApplication app = yarnClient.createApplication();

    // Set up the container launch context for the application master
    ContainerLaunchContext amContainer =
        Records.newRecord(ContainerLaunchContext.class);
    amContainer.setCommands(
        Collections.singletonList(
            command +
            " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout" +
            " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr"
        )
    );

    // Setup jar for ApplicationMaster
    final Path jarPath = new Path(jarUri);
    LocalResource appMasterJar = Records.newRecord(LocalResource.class);
    setupAppMasterJar(jarPath, appMasterJar);
    amContainer.setLocalResources(
        Collections.singletonMap("simpleapp.jar", appMasterJar));

    // Setup CLASSPATH for ApplicationMaster
    Map<String, String> appMasterEnv = new HashMap<String, String>();
    setupAppMasterEnv(appMasterEnv);
    amContainer.setEnvironment(appMasterEnv);

    // Set up resource type requirements for ApplicationMaster
    Resource capability = Records.newRecord(Resource.class);
    capability.setMemory(256);
//    capability.setVirtualCores(1);

    // Finally, set-up ApplicationSubmissionContext for the application
    ApplicationSubmissionContext appContext =
        app.getApplicationSubmissionContext();
    appContext.setApplicationName("simple-yarn-app"); // application name
    appContext.setAMContainerSpec(amContainer);
    appContext.setResource(capability);
    appContext.setQueue("default"); // queue

    // Submit application
    ApplicationId appId = appContext.getApplicationId();
    System.out.println("Submitting application " + appId);
    yarnClient.submitApplication(appContext);

    ApplicationReport appReport = yarnClient.getApplicationReport(appId);
    YarnApplicationState appState = appReport.getYarnApplicationState();
    while (appState != YarnApplicationState.FINISHED &&
        appState != YarnApplicationState.KILLED &&
        appState != YarnApplicationState.FAILED) {
      Thread.sleep(100);
      appReport = yarnClient.getApplicationReport(appId);
      appState = appReport.getYarnApplicationState();
    }

    System.out.println(
        "Application " + appId + " finished with" +
            " state " + appState +
            " at " + appReport.getFinishTime());

  }

  private void setupAppMasterJar(Path jarPath, LocalResource appMasterJar) throws IOException {
    FileStatus jarStat = FileSystem.get(conf).getFileStatus(jarPath);
    appMasterJar.setResource(ConverterUtils.getYarnUrlFromPath(jarPath));
    appMasterJar.setSize(jarStat.getLen());
    appMasterJar.setTimestamp(jarStat.getModificationTime());
    appMasterJar.setType(LocalResourceType.FILE);
    appMasterJar.setVisibility(LocalResourceVisibility.PUBLIC);
  }

  private void setupAppMasterEnv(Map<String, String> appMasterEnv) {
    for (String c : conf.getStrings(
        YarnConfiguration.YARN_APPLICATION_CLASSPATH,
        YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH)) {
      Apps.addToEnvironment(appMasterEnv, ApplicationConstants.Environment.CLASSPATH.name(),
          c.trim());
    }
    Apps.addToEnvironment(appMasterEnv,
        ApplicationConstants.Environment.CLASSPATH.name(),
        ApplicationConstants.Environment.PWD.$() + File.separator + "*");
  }



  @Override
  public void deploy(
      final ServiceConfiguration configuration,
      final URI jarUri,
      final int instances
  ) {

  }

  @Override
  public void undeployService(final String type) {

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

  @Override
  public void commandLine(final String... arguments) {

  }
}
