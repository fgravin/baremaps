package io.gazetteer.common.cli;

public class StopWatch {

  private long timestamp = System.currentTimeMillis();

  public long lap() {
    long t = timestamp;
    timestamp = System.currentTimeMillis();
    return timestamp - t;
  }
}