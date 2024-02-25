package io.github.akmal2409.dnsforwarder.server.mock;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * DNS utilises US-ASCII charset only. Each string consists of labels separated by a dot.
 */
class DnsStringCodec {

  public String decode(byte[] bytes) {
    if (bytes.length == 0) {
      return null;
    } else if (bytes[bytes.length - 1] != 0) {
      throw new IllegalArgumentException("String bytes should be terminated with a null byte");
    }

    final StringBuilder sb = new StringBuilder();

    int i = 0;

    while (i < bytes.length - 1) {
      final int labelByteCount = bytes[i];

      if (labelByteCount > bytes.length - i) {
        throw new IllegalArgumentException("Not enough bytes to decode string");
      }
      i++;

      sb.append(new String(bytes, i, labelByteCount, StandardCharsets.US_ASCII));
      sb.append('.');
      i += labelByteCount;
    }

    if (i != bytes.length - 1) {
      throw new IllegalArgumentException("Invalid string supplied. Could not parse");
    }

    sb.deleteCharAt(sb.length() - 1);

    return sb.toString();
  }

  /**
   * Encoding string to a compliant format involves converting every character to the ASCII
   * representation and prefixing each label with the byte count. So the structure is following: <1
   * byte indicating length of following string in bytes> <ascii encoded string>
   *
   * @param text to encode, must only contain ASCII letters and a dot.
   */
  public byte[] encode(String text) {
    final var labels = text.split("\\.");
    final int totalLength = Arrays.stream(labels)
                                .map(String::length)
                                .reduce(0, Integer::sum);

    // 1 byte for a an empty byte at the end of the sequence to indicate the end
    final byte[] bytes = new byte[totalLength + labels.length + 1];
    int i = 0;

    for (String label : labels) {
      bytes[i++] = (byte) label.length();

      for (byte ch : label.getBytes(StandardCharsets.US_ASCII)) {
        bytes[i++] = ch;
      }
    }

    return bytes;
  }

}
