package io.github.akmal2409.dnsforwarder.server.mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import io.github.akmal2409.dnsforwarder.server.mock.DnsStringCodec;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DnsStringCodecTest {

  DnsStringCodec codec = new DnsStringCodec();

  @Test
  @DisplayName("Can decode valid string")
  void canDecodeValidString() {
    byte[] googleBytes = "google".getBytes(StandardCharsets.US_ASCII);
    byte[] comBytes = "com".getBytes(StandardCharsets.US_ASCII);

    byte[] bytes = {0x6, googleBytes[0], googleBytes[1], googleBytes[2], googleBytes[3],
        googleBytes[4], googleBytes[5], 0x3, comBytes[0], comBytes[1], comBytes[2], 0x0};

    assertEquals("google.com", codec.decode(bytes));
  }

  @Test
  @DisplayName("Returns null if the byte array empty during decoding")
  void nullStringWhenDecodeByteArrayEmpty() {
    assertNull(codec.decode(new byte[0]));
  }

  @Test
  @DisplayName("Throws IllegalArgumentException when the last byte is non-zero")
  void exceptionWhenDecodingLastByteNonZero() {
    assertThrowsExactly(IllegalArgumentException.class, () ->
                                                            codec.decode(new byte[]{0x1}));
  }

  @Test
  @DisplayName("Throws IllegalArgumentException if the byte count indicates more than available chars")
  void decodingExceptionWhenNotEnoughChars() {
    byte[] comBytes = "com".getBytes(StandardCharsets.US_ASCII);

    byte[] bytes = {0x4, comBytes[0], comBytes[1], comBytes[2], 0x0};

    assertThrowsExactly(IllegalArgumentException.class, () ->
                           codec.decode(bytes));
  }
}
