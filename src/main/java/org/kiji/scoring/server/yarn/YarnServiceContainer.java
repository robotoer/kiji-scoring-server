package org.kiji.scoring.server.yarn;

import java.io.IOException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.UriSpec;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletHandler;

import org.kiji.scoring.server.ServiceTask;
import org.kiji.scoring.server.record.ServiceStatus;
//import org.kiji.scoring.server.servlets.GenericScoringServlet;

/**
 * Responsible for providing the score/status interfaces of a service instance within a YARN
 * container.
 */
public class YarnServiceContainer implements ServiceTask<Object> {
  public static final String APPLY_PATH = "/apply";

  @Override
  public Object apply(final Object argument) {
    return null;
  }

  @Override
  public ServiceStatus status() {
    return null;
  }

  @Override
  public String getType() {
    return null;
  }

  public static class ServiceContainerDetails {
    private final URL mBaseUrl;

    public ServiceContainerDetails(final URL baseUrl) {
      mBaseUrl = baseUrl;
    }

    public URL getBaseUrl() {
      return mBaseUrl;
    }
  }

  public static void main(final String[] args) throws Exception {
    // Parse out launch arguments/configuration for the service instance execution.
    final int port = 8080;
    final String serviceName = "";
    final String zookeeperConnectionString = "";
    final URL baseUrl = null;

    // Startup jetty.
    final Server server = new Server(port);
    {
      final HandlerList handlers = new HandlerList();

      // apply
      final ServletHandler servletHandler = new ServletHandler();
//      servletHandler.addServletWithMapping(GenericScoringServlet.class, APPLY_PATH);
      servletHandler.addServletWithMapping("org.kiji.scoring.server.servlets.GenericScoringServlet", APPLY_PATH);
      handlers.addHandler(servletHandler);

      // status
      handlers.addHandler(new AbstractHandler() {
        @Override
        public void handle(
            final String target,
            final Request baseRequest,
            final HttpServletRequest request,
            final HttpServletResponse response
        ) throws IOException, ServletException {
          if (!target.equalsIgnoreCase("/status")) {
            return;
          }
          baseRequest.setHandled(true);
        }
      });

      server.setHandler(handlers);
      server.start();
    }

    // Register the service.
    {
      final RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
      final CuratorFramework client =
          CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
      client.start();

      // in a real application, you'd have a convention of some kind for the URI layout
      final UriSpec uriSpec = new UriSpec("{scheme}://foo.com:{port}");

      final ServiceInstance<ServiceContainerDetails> thisInstance = ServiceInstance
          .<ServiceContainerDetails>builder()
          .name(serviceName)
          .payload(new ServiceContainerDetails(baseUrl))
          .port(port) // in a real application, you'd use a common port
          .uriSpec(uriSpec)
          .build();

      // if you mark your payload class with @JsonRootName the provided JsonInstanceSerializer will work
      final JsonInstanceSerializer<ServiceContainerDetails> serializer =
          new JsonInstanceSerializer<ServiceContainerDetails>(ServiceContainerDetails.class);

      final ServiceDiscovery<ServiceContainerDetails> serviceDiscovery = ServiceDiscoveryBuilder
          .builder(ServiceContainerDetails.class)
          .client(client)
          .basePath(YarnServiceMaster.BASE_SERVICE_DISCOVERY_PATH)
          .serializer(serializer)
          .thisInstance(thisInstance)
          .build();
    }

    // Heartbeat? or:
    server.join();
  }
}
