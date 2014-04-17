package org.kiji.scoring.server.yarn;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Records;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.scoring.server.ServiceManager;
import org.kiji.scoring.server.ServiceManagerFactory;
import org.kiji.scoring.server.record.ServiceManagerConfiguration;

public class YarnServiceManagerFactory implements ServiceManagerFactory {
  private static final Logger LOG = LoggerFactory.getLogger(YarnServiceManagerFactory.class);

  private YarnClient mYarnClient;

  public YarnServiceManagerFactory(final YarnConfiguration yarnConf) {
    // Create yarnClient
    mYarnClient = YarnClient.createYarnClient();
    mYarnClient.init(yarnConf);
    mYarnClient.start();
  }

  public void launchApplicationMaster(
      final String appName,
      final String appCommand,
      final int appMemory,
      final int appCores
  ) throws IOException, YarnException, InterruptedException {
    // Create application via yarnClient
    final YarnClientApplication app = mYarnClient.createApplication();

    // Populate the submission context.
    final ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
    {
      // Set up the container launch context for the application master
      final ContainerLaunchContext appContainerContext = Records.newRecord(ContainerLaunchContext.class);
      {
        final String loggedCommand = String.format("%s 1>%s/stdout 2>%s/stdout", appCommand, ApplicationConstants.LOG_DIR_EXPANSION_VAR, ApplicationConstants.LOG_DIR_EXPANSION_VAR);
        appContainerContext.setCommands(Collections.singletonList(loggedCommand));

        // TODO: Setup required resources here (jars, env vars, local files, etc.).
      }

      // Finally, set-up ApplicationSubmissionContext for the application
      appContext.setApplicationName(appName);
      appContext.setAMContainerSpec(appContainerContext);
      appContext.setResource(Resource.newInstance(appMemory, appCores));
      appContext.setQueue("default");
    }

    // Submit application
    final ApplicationId appId = appContext.getApplicationId();
    System.out.println("Submitting application " + appId);
    mYarnClient.submitApplication(appContext);

    ApplicationReport appReport = mYarnClient.getApplicationReport(appId);
    YarnApplicationState appState = appReport.getYarnApplicationState();
    while (
        appState != YarnApplicationState.FINISHED &&
        appState != YarnApplicationState.KILLED &&
        appState != YarnApplicationState.FAILED
    ) {
      Thread.sleep(100);
      appReport = mYarnClient.getApplicationReport(appId);
      appState = appReport.getYarnApplicationState();
    }

    System.out.println(
        "Application " + appId + " finished with" +
            " state " + appState +
            " at " + appReport.getFinishTime());
  }

  @Override
  public ServiceManager start(
      final ServiceManagerConfiguration configuration
  ) throws IOException, InterruptedException, YarnException {
    launchApplicationMaster(
        configuration.getName(),
        configuration.getCommand(),
        configuration.getMemory(),
        configuration.getCores()
    );

//    return new YarnServiceManager(mYarnConf.get())
    return null;
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
