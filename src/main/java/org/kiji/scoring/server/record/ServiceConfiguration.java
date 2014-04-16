package org.kiji.scoring.server.record;

public class ServiceConfiguration {
  private final String mType;
  private final String[] mCommand;

  public ServiceConfiguration(
      final String type,
      final String[] command
  ) {
    mType = type;
    mCommand = command;
  }

  public String getType() {
    return mType;
  }

  public String[] getCommand() {
    return mCommand;
  }
}
