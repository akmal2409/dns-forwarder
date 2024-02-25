package io.github.akmal2409.dnsforwarder.server.codec.decoders;

public final class CodecConstants {

  public static final int MIN_RESOURCE_RECORD_LENGTH = 10;
  public static final int MIN_OPT_OPTION_LENGTH = 4;

  private CodecConstants() {
    throw new IllegalArgumentException("Cannot instantiate a utility class");
  }
}
