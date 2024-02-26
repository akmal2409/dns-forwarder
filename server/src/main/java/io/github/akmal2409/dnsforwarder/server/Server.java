package io.github.akmal2409.dnsforwarder.server;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsMessageDecoder;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsQuestion;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ARecord;
import io.github.akmal2409.dnsforwarder.server.mock.DnsPacketGenerator;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {

  private static final Logger logger = LoggerFactory.getLogger(Server.class);

  private static final int MAX_PORT_NUMBER = (1 << 15) - 1;

  private final String host;
  private final int port;
  private final AtomicBoolean running;
  private final Thread runningThread;
  private final CountDownLatch shutdownLatch;
  private final byte[] buffer;


  private Server(String host, int port, int bufferSize) {
    if (port < 0 || port > MAX_PORT_NUMBER) {
      throw new IllegalArgumentException("Invalid port passed. Expected value between 0 and 65535");
    }
    this.host = Objects.requireNonNull(host, "host cannot be null");
    this.port = port;
    this.running = new AtomicBoolean(false);
    this.runningThread = Thread.ofVirtual().unstarted(this::beginListening);
    this.shutdownLatch = new CountDownLatch(1);
    this.buffer = new byte[bufferSize];
  }

  public static Server atPort(int port) {
    return new Server("127.0.0.1", port, 1024);
  }

  public static void main(String[] args) throws InterruptedException {
    final int serverPort = parseServerPort(args);

    try {
      Server.atPort(serverPort).start().await();
    } catch (Exception e) {
      logger.error("Err", e);
    }
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

  public Server start() {
    if (!running.compareAndSet(false, true)) {
      throw new IllegalStateException("The server is already running or has been shutdown");
    }

    this.runningThread.start();
    return this;
  }

  private void beginListening() {
    try (final var socket = new DatagramSocket(this.port)) {

      logger.info("Started DNS forwarder at port {}. Listening to packets...", this.port);

      while (!Thread.currentThread().isInterrupted()) {
        final var packet = new DatagramPacket(this.buffer, this.buffer.length);
        socket.receive(packet);

        logger.info("Received packet from {}:{}", packet.getAddress(), packet.getPort());
        var message = DnsMessageDecoder.fromBytes(packet.getData()).decode();

//        final var mockData = DnsPacketGenerator.createMessage(c -> {
//          c.id(message.header().id());
//          c.query(false);
//          c.question(new DnsQuestion("google.com", DnsType.A, DnsClass.IN));
//          try {
//            c.answers(
//                List.of(
//                    new ARecord("google.com", DnsClass.IN, 110,
//                        (Inet4Address) InetAddress.getByName("10.23.110.1")),
//                    new ARecord("google.com", DnsClass.IN, 110,
//                        (Inet4Address) InetAddress.getByName("10.23.110.2")),
//                    new ARecord("google.com", DnsClass.IN, 110,
//                        (Inet4Address) InetAddress.getByName("10.23.110.3"))
//                )
//            );
//          } catch (UnknownHostException e) {
//            throw new RuntimeException(e);
//          }
//        });

//        final var mockPacket = new DatagramPacket(mockData, mockData.length,
//            packet.getSocketAddress());

        logger.info("Parsed packet {}",
            message);

//        socket.send(mockPacket);

        // Remove after
        try (final var gSocket = new DatagramSocket(new InetSocketAddress(8888))) {
          final var buff = new byte[2048];
          final var googleOutPkt = new DatagramPacket(packet.getData(), packet.getLength(),
              InetAddress.getByName("8.8.8.8"), 53);

          gSocket.send(googleOutPkt);
          final var googleInPkt = new DatagramPacket(buff, buff.length);
          gSocket.receive(googleInPkt);

          logger.info("Parsed google packet {}",
              DnsMessageDecoder.fromBytes(googleInPkt.getData()).decode());

          googleInPkt.setAddress(packet.getAddress());
          googleInPkt.setPort(packet.getPort());
          socket.send(googleInPkt);
        }

      }

    } catch (IOException e) {

      switch (e) {
        case PortUnreachableException ignored -> logger.debug("Connected to unreachable port", e);
        case IOException io -> {
          logger.error("IO exception of a socket", io);
          throw new ServerNetworkException("Server crashed due to socket exception", e);
        }
      }

    } catch (Throwable e) {
      logger.error("Unknown error", e);
      throw e;
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

  public void await() throws InterruptedException {
    this.shutdownLatch.await();
  }
}
