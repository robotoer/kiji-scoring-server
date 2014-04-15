package org.kiji.scoring.server;

// Should be a Container.
public interface ServiceContainer {
  void setup();
  void close();

  Object apply(final Object argument);
  void status();
}
