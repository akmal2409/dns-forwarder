package io.github.akmal2409.dnsforwarder.server.codec.encoders;

public class CharacterStringEncoder {

  public byte[] encode(String charString) {
    final var bytes = new byte[charString.length() + 1];

    bytes[0] = (byte) charString.length();

    for (int i = 0; i < charString.length(); i++) {
      bytes[i + 1] = (byte) charString.charAt(i);
    }

    return bytes;
  }
}
