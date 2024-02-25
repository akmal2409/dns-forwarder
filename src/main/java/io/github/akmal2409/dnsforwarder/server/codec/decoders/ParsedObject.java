package io.github.akmal2409.dnsforwarder.server.codec.decoders;

public class ParsedObject<T> {

  public final T item;
  public final int nextByteOffset;

  public ParsedObject(T item, int nextByteOffset) {
    this.item = item;
    this.nextByteOffset = nextByteOffset;
  }
}
