package io.github.akmal2409.dnsforwarder.server;

public class ServerNetworkException extends RuntimeException {

  public ServerNetworkException(String message) {
    super(message);
  }

  public ServerNetworkException(String message, Throwable cause) {
    super(message, cause);
  }
}
