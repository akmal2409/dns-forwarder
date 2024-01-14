package io.github.akmal2409;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {

  private static final int MAX_PORT_NUMBER = (1 << 15) - 1;

  private final int port;
  private final AtomicBoolean running;
  private final Thread runningThread;
  private final CountDownLatch shutdownLatch;
  private final byte[] buffer;


  private Server(int port, int bufferSize) {
    if (port < 0 || port > MAX_PORT_NUMBER) {
      throw new IllegalArgumentException("Invalid port passed. Expected value between 0 and 65535");
    }
    this.port = port;
    this.running = new AtomicBoolean(false);
    this.runningThread = Thread.ofVirtual().unstarted(this::beginListening);
    this.runningThread.setDaemon(false);
    this.shutdownLatch = new CountDownLatch(1);
    this.buffer = new byte[bufferSize];
  }

  public void start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("The server is already running or has been shutdown");
    }

    this.runningThread.start();
  }

  private void beginListening() {
    try (final var socket = new DatagramSocket(this.port)) {

      while (!Thread.currentThread().isInterrupted()) {
        final var packet = new DatagramPacket(this.buffer, this.buffer.length);
        socket.receive(packet);
        System.out.println("Received packet");
      }

    } catch (SocketException e) {
      throw new StartupFailedException("Could not bind socket", e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      this.shutdownLatch.countDown();
    }
  }

  /**
   * Shuts down the server if its running and hasn't been requested to shutdown. If the timeout is
   * greater than 0 then the client will await for the server to shutdown. Otherwise you may pass 0
   * if you don't want to wait.
   *
   * @param timeout number of units of time to wait
   * @param unit    unit of time
   * @return flag whether the await succeeded or not
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  public boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
    if (!this.running.compareAndSet(true, false)) {
      throw new IllegalStateException(
          "Cannot shutdown server because it is either off or being shutdown");
    }
    this.runningThread.interrupt();
    return this.shutdownLatch.await(timeout, unit);
  }

  public static void main(String[] args) {
    final int serverPort = parseServerPort(args);

    new Server(serverPort, 1024).start();
  }

  private static int parseServerPort(String[] args) {
    if (args.length == 0 || !args[0].startsWith("--port")) {
      return 0;
    } else {
      String port;

      if (args[0].contains("=")) {
        port = args[0].split("=")[1];
      } else if (args.length > 1) {
        port = args[1];
      } else {
        throw new IllegalArgumentException(
            "Cannot parse server port. Expected --port=xxx or --port xxx");
      }

      try {
        return Integer.parseInt(port);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Cannot parse non-numeric port. " + port);
      }
    }
  }
}
