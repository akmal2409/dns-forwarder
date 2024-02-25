package io.github.akmal2409.dnsforwarder.server.shared;

import io.github.akmal2409.dnsforwarder.server.codec.models.records.Opt.Option;
import io.github.akmal2409.dnsforwarder.server.common.ByteUtils;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CodecUtils {

  public static sealed interface Label {

  }

  public static record ValueLabel(String text) implements Label {

  }

  public static record PointerLabel(int pointer) implements Label {

  }

  private CodecUtils() {
  }

  public static byte[] encodedLabels(List<? extends Label> labels) {
    int pointerSize = 0; // bytes taken by pointers
    int labelSize = 0; // bytes taken by value labels

    for (Label label : labels) {
      switch (label) {
        case ValueLabel value -> labelSize += 1 + value.text.length();
        case PointerLabel ignored -> pointerSize += 2;
      }
    }
    // a pointer sequence doesn't need to terminate with a null byte
    final int byteCount = pointerSize > 0 ? pointerSize + labelSize : labelSize + 1;
    final byte[] data = new byte[byteCount];
    int byteIdx = 0;

    for (Label label : labels) {
      switch (label) {
        case ValueLabel value -> {
          data[byteIdx++] = (byte) value.text.length();
          System.arraycopy(value.text.getBytes(StandardCharsets.US_ASCII), 0,
              data, byteIdx, value.text.length());
          byteIdx += value.text.length();
        }
        case PointerLabel pointer -> {
          data[byteIdx++] = (byte) ((0xff & pointer.pointer) | (3 << 6));
          data[byteIdx++] = (byte) (0xff & (pointer.pointer >> Byte.SIZE));
        }
      }
    }
    return data;
  }

  public static byte[] encodeCharacterString(String text) {
    final var bytes = new byte[text.length() + 1];
    bytes[0] = (byte) text.length();
    System.arraycopy(text.getBytes(StandardCharsets.US_ASCII), 0, bytes, 1, bytes.length - 1);
    return bytes;
  }

  public static byte[] encodeResourceRecord(String domainName, short dnsClass, short dnsType, int ttl, byte[] rData) {
    final var encodedDomain = encodedLabels(List.of(new ValueLabel("google"), new ValueLabel("com")));
    final var bytes = new byte[encodedDomain.length + 10 + rData.length];
    System.arraycopy(encodedDomain, 0, bytes, 0, encodedDomain.length);

    ByteUtils.writeShort(bytes, encodedDomain.length, 1, dnsType);
    ByteUtils.writeShort(bytes, encodedDomain.length + 2, 1, dnsClass);
    ByteUtils.writeInt(bytes, encodedDomain.length + 4, 1, ttl);
    ByteUtils.writeShort(bytes, encodedDomain.length + 8, 1, (short) rData.length);

    System.arraycopy(rData, 0, bytes, encodedDomain.length + 10, rData.length);

    return bytes;
  }

  public static byte[] encodeOptions(List<Option> options) {
    final var totalSize = options.stream()
                              .map(opt -> opt.length() + 4)
                              .mapToInt(val -> val)
                              .sum();
    final var bytes = new byte[totalSize];

    int offset = 0;

    for (Option option: options) {
      ByteUtils.writeShort(bytes, offset, 1, option.code());
      offset += 2;
      ByteUtils.writeShort(bytes, offset, 1, option.length());
      offset += 2;

      System.arraycopy(option.data(), 0, bytes, offset, option.length());

      offset += option.length();
    }

    return bytes;
  }
}
