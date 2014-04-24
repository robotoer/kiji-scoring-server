package org.kiji.scoring.server.yarn;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.URL;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.Apps;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kiji.scoring.server.ServiceManager;
import org.kiji.scoring.server.ServiceManagerFactory;
import org.kiji.scoring.server.record.ServiceManagerConfiguration;

public class YarnServiceManagerFactory implements ServiceManagerFactory {
  private static final Logger LOG = LoggerFactory.getLogger(YarnServiceManagerFactory.class);
  public static final String APPLICATION_MASTER_NAME = "service-master-1";

  private YarnClient mYarnClient;

  public YarnServiceManagerFactory(final YarnConfiguration yarnConf) {
    // Create yarnClient
    mYarnClient = YarnClient.createYarnClient();
    mYarnClient.init(yarnConf);
    mYarnClient.start();
  }

  public void launchApplicationMaster(
      final String appName,
      final int appMemory,
      final int appCores,
      final int appAdminPort,
      final String curatorAddress
  ) throws IOException, YarnException, InterruptedException {
    // Create application via yarnClient
    final YarnClientApplication app = mYarnClient.createApplication();

    // Populate the submission context.
    final ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
    {
      // Set up the container launch context for the application master
      final ContainerLaunchContext appContainerContext = Records.newRecord(ContainerLaunchContext.class);
      {
        final Configuration config = mYarnClient.getConfig();

//        final String loggedCommand = String.format(
////            "$JAVA_HOME/bin/java %s %s 1>%s/stdout 2>%s/stdout",
//            "${JAVA_HOME}/bin/java %s %s",
//            YarnServiceMaster.YARN_SERVICE_MASTER_JAVA_FLAGS,
//            YarnServiceMaster.class.getName()//,
////            appCommand,
////            ApplicationConstants.LOG_DIR_EXPANSION_VAR,
////            ApplicationConstants.LOG_DIR_EXPANSION_VAR
//        );
        final String loggedCommand = String.format(
            "${JAVA_HOME}/bin/java %s %s %s",
            YarnServiceMaster.YARN_SERVICE_MASTER_JAVA_FLAGS,
            YarnServiceMaster.class.getName(),
            YarnServiceMaster.prepareArgs(APPLICATION_MASTER_NAME, appAdminPort, curatorAddress)
        );
        LOG.info("Launching service application master with command: {}", loggedCommand);
        appContainerContext.setCommands(Collections.singletonList(loggedCommand));

        // Copy classpath of client.
        final String classpath = System.getProperty("java.class.path");

        final Map<String, LocalResource> localResources = Maps.newHashMap();
        final Map<String, String> masterEnvVars = Maps.newHashMap();
        for (final String classpathEntry : classpath.split(File.pathSeparator)) {
          if (!classpathEntry.isEmpty()) {
            final Path entryPath = new Path(classpathEntry);
            final FileStatus entryFileStatus =
                FileSystem.getLocal(config).getFileStatus(entryPath);
//            LOG.info("Adding {}", entryFileStatus);
            final URL yarnUrlFromPath = ConverterUtils.getYarnUrlFromPath(entryPath);
            localResources.put(
                entryPath.getName(),
                LocalResource.newInstance(
                    yarnUrlFromPath,
                    LocalResourceType.FILE,
                    LocalResourceVisibility.PUBLIC,
                    entryFileStatus.getLen(),
                    entryFileStatus.getModificationTime()
                )
            );
            yarnUrlFromPath.setScheme("file");
            LOG.debug("Adding {}", localResources.get(entryPath.getName()));
            // TODO: Does this help?
            Apps.addToEnvironment(
                masterEnvVars,
                ApplicationConstants.Environment.CLASSPATH.name(),
                classpathEntry.trim()
            );
          } else {
            LOG.warn("Blank classpath entry found!");
          }
        }

        final String[] yarnConfigClasspath = config.getStrings(
            YarnConfiguration.YARN_APPLICATION_CLASSPATH,
            YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH
        );
        for (String classpathEntry : yarnConfigClasspath) {
          Apps.addToEnvironment(
              masterEnvVars,
              ApplicationConstants.Environment.CLASSPATH.name(),
              classpathEntry.trim()
          );
        }

        Apps.addToEnvironment(
            masterEnvVars,
            ApplicationConstants.Environment.CLASSPATH.name(),
            ApplicationConstants.Environment.PWD.$() + File.separator + "*"
        );

        appContainerContext.setLocalResources(localResources);
        appContainerContext.setEnvironment(masterEnvVars);

        appContainerContext.setTokens();
        mYarnClient.getAMRMToken().
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
        String.format(
            "Application %s finished with state %s at %d",
            appId,
            appState,
            appReport.getFinishTime()
        )
    );
  }

  private void setupAppMasterEnv(Map<String, String> appMasterEnv) { }

  @Override
  public ServiceManager start(
      final ServiceManagerConfiguration configuration
  ) throws IOException, InterruptedException, YarnException {
    launchApplicationMaster(
        configuration.getName(),
        configuration.getMemory(),
        configuration.getCores(),
        configuration.getPort(),
        configuration.getCuratorAddress()
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
