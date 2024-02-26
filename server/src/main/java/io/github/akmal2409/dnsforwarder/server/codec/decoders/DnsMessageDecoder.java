package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import static io.github.akmal2409.dnsforwarder.server.common.ByteUtils.isBitSet;
import static io.github.akmal2409.dnsforwarder.server.common.ByteUtils.read2BytesAsInt;
import static io.github.akmal2409.dnsforwarder.server.common.ByteUtils.read2BytesUnsignedAsShort;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedPointerLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedValueLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.ParsedDomainName;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsHeader;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsMessage;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsQuestion;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import io.github.akmal2409.dnsforwarder.server.codec.models.LabelOffsetCollection;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ResourceRecord;
import java.util.ArrayList;
import java.util.List;

public class DnsMessageDecoder {

  private final byte[] data;
  private final DnsDomainNameDecoder domainNameDecoder;
  private final ResourceRecordDecoder resourceRecordDecoder;
  private final LabelOffsetCollection labelOffsetCollection;
  private int offset;

  private DnsMessageDecoder(byte[] data) {
    this.data = data;
    this.labelOffsetCollection = new LabelOffsetCollection();
    this.offset = 0;
    this.domainNameDecoder = new DnsDomainNameDecoder(data);
    this.resourceRecordDecoder = new DecompressingResourceRecordDecoder(data,
        labelOffsetCollection);
  }

  public static DnsMessageDecoder fromBytes(byte[] data) {
    return new DnsMessageDecoder(data);
  }

  public DnsMessage decode() {
    final var header = parseHeader();
    final var questions = new DnsQuestion[header.questionCount()];

    if (header.questionCount() > 0) {
      for (int i = 0; i < header.questionCount(); i++) {
        questions[i] = parseQuestion();
      }
    }

    final var answers = parseKResourceRecords(header.answerCount());
    final var nameServers = parseKResourceRecords(header.nameServerCount());
    final var additional = parseKResourceRecords(header.additionalCount());

    return new DnsMessage(
        header,
        questions,
        answers, nameServers, additional
    );
  }

  private DnsHeader parseHeader() {
    if (data.length < 12) {
      throw new IllegalArgumentException("Header must be 12 bytes. Supplied " + data.length);
    }

    final short id = read2BytesUnsignedAsShort(data, 0);
    final boolean query = !isBitSet(data[2], 7);
    final byte opcode = (byte) ((data[2] >> 3) & 0xf);
    final boolean authoritativeAns = isBitSet(data[2], 2);
    final boolean truncation = isBitSet(data[2], 1);
    final boolean recursionDesired = isBitSet(data[2], 0);

    final boolean recursionAvailable = isBitSet(data[3], 7);
    final byte responseCode = (byte) (data[3] & 0xf);
    final int questionCount = read2BytesAsInt(data, 4);
    final int answerCount = read2BytesAsInt(data, 6);
    final int nsCount = read2BytesAsInt(data, 8);
    final int addCount = read2BytesAsInt(data, 10);

    this.offset += 12;
    return new DnsHeader(
        id, query, opcode, authoritativeAns, truncation, recursionDesired, recursionAvailable,
        false,
        responseCode, questionCount, answerCount, nsCount, addCount
    );
  }

  private DnsQuestion parseQuestion() {
    final ParsedDomainName decodedDomainName = domainNameDecoder.decodeStartingAt(offset);
    final List<DecodedValueLabel> valueLabels = new ArrayList<>();
    final List<String> labelStrings = new ArrayList<>();

    for (DecodedLabel label : decodedDomainName.labels()) {
      switch (label) {
        case DecodedPointerLabel ignored ->
            throw new IllegalArgumentException("Pointer passed in a question");
        case DecodedValueLabel valueLabel -> {
          valueLabels.add(valueLabel);
          labelStrings.add(valueLabel.value());
        }
      }
    }

    labelOffsetCollection.put(decodedDomainName.startOffset(),
        valueLabels);

    offset = decodedDomainName.endOffset();

    if (offset + 4 > data.length) {
      throw new IllegalArgumentException(
          "Malformed question. Expected at least 4 more bytes for QNAME and QTYPE. Received excess of "
              + (
              offset + 4 - data.length));
    }

    final short typeCode = read2BytesUnsignedAsShort(data, offset);
    final short classCode = read2BytesUnsignedAsShort(data, offset + 2);
    offset += 4;

    return new DnsQuestion(
        String.join(".", labelStrings), DnsType.from(typeCode), DnsClass.from(classCode));
  }

  private ResourceRecord[] parseKResourceRecords(int numRecords) {
    final var records = new ResourceRecord[numRecords];

    for (int i = 0; i < numRecords; i++) {
      final ParsedObject<ResourceRecord> parsedRecord = resourceRecordDecoder.decodeStartingAt(
          offset);

      records[i] = parsedRecord.item;
      offset = parsedRecord.nextByteOffset; // next byte after the resource record
    }

    return records;
  }
}
