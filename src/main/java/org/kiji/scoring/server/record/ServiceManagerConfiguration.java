package org.kiji.scoring.server.record;

public class ServiceManagerConfiguration {
  private final String mName;
  private final String mCommand;
  private final int mMemory;
  private final int mPort;

  public ServiceManagerConfiguration(
      final String name,
      final String command,
      final int memory,
      final int port
  ) {
    mPort = port;
    mName = name;
    mCommand = command;
    mMemory = memory;
  }

  public String getName() {
    return mName;
  }

  public String getCommand() {
    return mCommand;
  }

  public int getMemory() {
    return mMemory;
  }

  public int getPort() {
    return mPort;
  }
}
