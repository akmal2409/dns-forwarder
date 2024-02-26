package io.github.akmal2409.dnsforwarder.server.codec;

public final class DnsUtils {

  public static final int POINTER_MASK = 3 << 6;

  private DnsUtils() {
    throw new IllegalStateException("Cannot instantiate a utility class");
  }

  public static boolean isLabelPointer(byte b) {
    return ((b & 0xff) & POINTER_MASK) == POINTER_MASK;
  }
}
