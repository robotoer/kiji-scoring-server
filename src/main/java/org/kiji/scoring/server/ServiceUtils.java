package org.kiji.scoring.server;

import java.net.URI;

public class ServiceUtils {
  public static void main(final String[] args) {
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
    serviceManager.commandLine(
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
    serviceManager.commandLine("undeploy-service", serviceConfiguration.getType());
    serviceManager.commandLine("undeploy-instance", "service-type.0");

    // List deployed services (java api).
    serviceManager.listServices();
    serviceManager.listServiceInstances();
    // List deployed services (cli api).
    serviceManager.commandLine("list-services");
    serviceManager.commandLine("list-service-instances");
  }
}