package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import io.github.akmal2409.dnsforwarder.server.codec.DnsUtils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the whole byte sequence and does not mutate it. Has a single method to parse a DNS label
 * from an arbitrary byte offset. The implementation supports pointers as well and returns them to
 * the  client.
 */
public class DnsDomainNameDecoder {


  private final byte[] data;

  public DnsDomainNameDecoder(byte[] data) {
    this.data = data;
  }

  // returns either position of a null-byte or last byte of a pointer
  private static int findTextEndFrom(byte[] bytes, int offset) {
    int start = offset;

    while (start < bytes.length && bytes[start] != 0 && !DnsUtils.isLabelPointer(bytes[start])) {
      start++;
    }

    if (start < bytes.length && DnsUtils.isLabelPointer(bytes[start])) {
      start++; // because pointer is 2 bytes long
    }

    return start;
  }

  public ParsedDomainName decodeStartingAt(int offset) {
    final int domainNameEnd = findTextEndFrom(data, offset);
    if (domainNameEnd == data.length) {
      throw new IllegalArgumentException(
          "Domain name cannot be parsed, failed to find terminated label sequence or a pointer");
    }

    final int labelsStartOffset = offset;
    final var labels = new ArrayList<DecodedLabel>();

    if (offset == domainNameEnd) {
      // root domain name
      return new ParsedDomainName(labelsStartOffset, domainNameEnd + 1,
          List.of(new DecodedValueLabel(labelsStartOffset, null)));
    }

    while (offset < domainNameEnd) {

      if (DnsUtils.isLabelPointer(data[offset])) {
        if (domainNameEnd - offset + 1 < 2) {
          throw new IllegalArgumentException("Pointer cannot be parsed. Not a 2 byte sequence");
        }
        final byte msb = (byte) ((data[offset] & 0xff) & ((1 << 6) - 1));
        final int pointer = (msb << Byte.SIZE) | (data[offset + 1] & 0xff);
        labels.add(new DecodedPointerLabel(offset, pointer));

        offset += 2;

        if (offset < domainNameEnd) {
          throw new IllegalArgumentException(
              "Declared label sequence with a pointer contains extra data after it");
        }
      } else {
        ParsedObject<String> label = nextLabel(offset);
        labels.add(new DecodedValueLabel(offset, label.item));
        offset = label.nextByteOffset;
      }
    }

    return new ParsedDomainName(labelsStartOffset, domainNameEnd + 1, labels);
  }

  private ParsedObject<String> nextLabel(int offset) {
    if (offset == this.data.length - 2) {
      throw new IllegalArgumentException("Malformed label, cannot parse");
    } else if (this.data[offset] == 0) {
      throw new IllegalArgumentException("End of the label sequence");
    }

    final int length = this.data[offset++] & 0xff;
    if (length > this.data.length - offset - 1) {
      throw new IllegalArgumentException("Malformed label length. Not enough bytes");
    }

    final byte[] letterBytes = new byte[length];
    System.arraycopy(data, offset, letterBytes, 0, length);

    offset += length;
    return new ParsedObject<>(new String(letterBytes, StandardCharsets.US_ASCII), offset);
  }

  public static sealed interface DecodedLabel {

    int offset();
  }

  public static record DecodedValueLabel(int offset, String value) implements DecodedLabel {

  }

  public static record DecodedPointerLabel(int offset, int pointer) implements DecodedLabel {

  }

  public static record ParsedDomainName(
      int startOffset, // where the first length octet of a first label is
      int endOffset, // exclusive, pointing to the next non-label byte (i.e. one after null byte)
      List<DecodedLabel> labels
  ) {

  }
}
