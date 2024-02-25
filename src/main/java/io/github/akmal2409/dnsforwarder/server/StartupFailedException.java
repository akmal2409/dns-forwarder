package io.github.akmal2409.dnsforwarder.server;

public class StartupFailedException extends RuntimeException {

  public StartupFailedException(String message) {
    super(message);
  }

  public StartupFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
