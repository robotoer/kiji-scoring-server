package org.kiji.scoring.server.record;

public class ServiceManagerConfiguration {
  private final String mName;
  private final int mMemory;
  private final int mPort;
  private final int mCores;
  private final String mCuratorAddress;

  public ServiceManagerConfiguration(
      final String name,
      final int memory,
      final int port,
      final int cores,
      final String curatorAddress
  ) {
    mName = name;
    mMemory = memory;
    mPort = port;
    mCores = cores;
    mCuratorAddress = curatorAddress;
  }

  public String getName() {
    return mName;
  }

  public int getMemory() {
    return mMemory;
  }

  public int getPort() {
    return mPort;
  }

  public int getCores() {
    return mCores;
  }

  public String getCuratorAddress() {
    return mCuratorAddress;
  }
}
