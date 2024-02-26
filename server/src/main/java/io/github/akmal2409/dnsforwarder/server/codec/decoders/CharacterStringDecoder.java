package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public record CharacterStringDecoder(
    byte[] data
) {

  /**
   * Contains length as the first byte following the actual character sequence
   *
   * @param offset where the length byte is located in the source bytes
   */
  public ParsedObject<String> decodeAt(int offset) {
    if (data.length - offset < 0) {
      throw new IllegalArgumentException("Cannot parse character string not enough bytes");
    }

    final int length = data[offset++];

    if (data.length - offset < length) {
      throw new IllegalArgumentException(
          "Cannot parse character string, not enough bytes. Expected "
              + length + " received " + (data.length - offset));
    }

    final String charString = new String(data, offset, length, StandardCharsets.US_ASCII);
    offset += length;

    return new ParsedObject<>(charString, offset);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CharacterStringDecoder that = (CharacterStringDecoder) o;

    return Arrays.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(data);
  }

  @Override
  public String toString() {
    return "CharacterStringDecoder{" +
               "data=" + Arrays.toString(data) +
               '}';
  }
}
