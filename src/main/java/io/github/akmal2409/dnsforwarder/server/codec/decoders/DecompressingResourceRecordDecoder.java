package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import static io.github.akmal2409.dnsforwarder.server.codec.decoders.CodecConstants.MIN_RESOURCE_RECORD_LENGTH;
import static io.github.akmal2409.dnsforwarder.server.common.ByteUtils.read2BytesAsInt;
import static io.github.akmal2409.dnsforwarder.server.common.ByteUtils.read2BytesUnsignedAsShort;
import static io.github.akmal2409.dnsforwarder.server.common.ByteUtils.read4BytesUnsignedAsLong;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedValueLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.ParsedDomainName;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import io.github.akmal2409.dnsforwarder.server.codec.models.LabelOffsetCollection;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.RDataDecoder;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ResourceRecord;
import java.util.List;

public class DecompressingResourceRecordDecoder implements ResourceRecordDecoder {

  private final byte[] data;
  private final LabelOffsetCollection labelOffsetCollection;
  private final DnsDomainNameDecoder domainNameDecoder;

  public DecompressingResourceRecordDecoder(byte[] data,
      LabelOffsetCollection labelOffsetCollection) {
    this.data = data;
    this.labelOffsetCollection = labelOffsetCollection;
    this.domainNameDecoder = new DnsDomainNameDecoder(data);
  }

  @Override
  public ParsedObject<ResourceRecord> decodeStartingAt(int offset) {
    final ParsedObject<List<String>> domainName = parsedDomainNameAt(offset);

    offset = domainName.nextByteOffset; // next byte after domain name
    if (this.data.length - offset < MIN_RESOURCE_RECORD_LENGTH) {
      throw new IllegalArgumentException(
          "Unable to parse resource record. Smaller than " + MIN_RESOURCE_RECORD_LENGTH + " bytes");
    }

    final short typeCode = read2BytesUnsignedAsShort(data, offset);
    offset += 2;
    final DnsType type = DnsType.from(typeCode);

    final short dnsClass = read2BytesUnsignedAsShort(data, offset);
    offset += 2;

    final long ttl = read4BytesUnsignedAsLong(data, offset);
    offset += 4;

    final int rDataLength = read2BytesAsInt(data, offset);
    offset += 2;

    final var resourceRecord = RDataDecoder.newInstanceFor(type, labelOffsetCollection)
                                   .decode(String.join(".", domainName.item), dnsClass,
                                       type, ttl, rDataLength, offset, data);

    offset += rDataLength;
    return new ParsedObject<>(resourceRecord, offset);
  }

  private ParsedObject<List<String>> parsedDomainNameAt(int offset) {
    final ParsedDomainName domainName = domainNameDecoder.decodeStartingAt(offset);
    final List<DecodedValueLabel> valueLabels = labelOffsetCollection.put(domainName);

    return new ParsedObject<>(valueLabels.stream()
                                  .map(DecodedValueLabel::value)
                                  .toList(),
        domainName.endOffset());
  }
}
