package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import static io.github.akmal2409.dnsforwarder.server.codec.decoders.CodecConstants.MIN_OPT_OPTION_LENGTH;
import static io.github.akmal2409.dnsforwarder.server.common.ByteUtils.read2BytesUnsignedAsShort;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.CharacterStringDecoder;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedValueLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.ParsedDomainName;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.ParsedObject;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import io.github.akmal2409.dnsforwarder.server.codec.models.LabelOffsetCollection;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Opt.Option;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public interface RDataDecoder {

  private static void checkHasEnoughBytes(byte[] data, int offset, int dataLength) {
    if (data.length - offset < dataLength) {
      throw new IllegalArgumentException(
          "Cannot parse rdata expected at least " + dataLength + " bytes from offset " + offset
              + " received " + (data.length - offset));
    }
  }

  static RDataDecoder newInstanceFor(DnsType dnsType, LabelOffsetCollection labelOffsetCollection) {

    return switch (dnsType) {
      case CNAME -> new DomainNameRdataDecoder(labelOffsetCollection,
          (domainName, dnsClass, ttl, labels) -> new Cname(domainName, dnsClass, ttl,
              String.join(".", labels)));
      case A -> new ARecordRdataDecoder();
      case OPT -> new OptRDataDecoder();
      case HIINFO -> new HiInfoRdataDecoder();
      case NS -> new DomainNameRdataDecoder(labelOffsetCollection,
          (domainName, dnsClass, ttl, labels) -> new NS(domainName, dnsClass, ttl,
              String.join(".", labels)));
      case null, default -> throw new IllegalArgumentException(
          String.format("Unsupported dns type %s, no rdata decoder", dnsType));
    };
  }

  /**
   * Used to construct different instances of resource records because some may require
   * decompression, while others simply parsing of rData.
   *
   * @param domainName  domain name this record belongs to
   * @param dnsClass    dns class TODO: dns class might be payload size for OPT
   * @param dnsType     dns type (can be UDP requestors payload size)
   * @param ttl         time to live
   * @param rDataLength length of the rData in bytes
   * @param startOffset start offset in the data byte array
   * @param data        raw bytes containing rData
   */
  ResourceRecord decode(String domainName, short dnsClass, DnsType dnsType, long ttl,
      int rDataLength, int startOffset, byte[] data);

  @FunctionalInterface
  interface SingleDomainNameRecordFactory {

    ResourceRecord apply(String domainName, DnsClass dnsClass, long ttl, List<String> labels);
  }

  record DomainNameRdataDecoder(LabelOffsetCollection labelOffsetCollection,
                                SingleDomainNameRecordFactory recordFactory) implements
      RDataDecoder {

    @Override
    public ResourceRecord decode(String sourceDomainName, short dnsClass, DnsType type, long ttl,
        int rDataLength, int startOffset, byte[] data) {
      checkHasEnoughBytes(data, startOffset, rDataLength);

      final var domainNameDecoder = new DnsDomainNameDecoder(data);
      final ParsedDomainName domainName = domainNameDecoder.decodeStartingAt(startOffset);

      final List<DecodedValueLabel> valueLabels = labelOffsetCollection.put(domainName);
      final var labels = valueLabels.stream().map(DecodedValueLabel::value).toList();

      return recordFactory.apply(sourceDomainName, DnsClass.from(dnsClass), ttl, labels);
    }
  }


  record HiInfoRdataDecoder() implements RDataDecoder {

    @Override
    public ResourceRecord decode(String domainName, short dnsClass, DnsType type, long ttl,
        int rDataLength, int startOffset, byte[] data) {
      checkHasEnoughBytes(data, startOffset, rDataLength);
      final var charStringDecoder = new CharacterStringDecoder(data);
      final ParsedObject<String> cpu = charStringDecoder.decodeAt(startOffset);
      final ParsedObject<String> os = charStringDecoder.decodeAt(cpu.nextByteOffset);

      return new HInfo(domainName, DnsClass.from(dnsClass), ttl, cpu.item, os.item);
    }
  }

  record ARecordRdataDecoder() implements RDataDecoder {

    @Override
    public ResourceRecord decode(String domainName, short dnsClass, DnsType type, long ttl,
        int rDataLength, int startOffset, byte[] data) {
      checkHasEnoughBytes(data, startOffset, rDataLength);

      if (rDataLength != 4) {
        throw new IllegalArgumentException("Expected 32 bits long rdata with address");
      }

      try {
        final var ipBytes = new byte[4];
        System.arraycopy(data, startOffset, ipBytes, 0, ipBytes.length);
        return new ARecord(domainName, DnsClass.from(dnsClass), ttl,
            (Inet4Address) InetAddress.getByAddress(ipBytes));
      } catch (UnknownHostException | ClassCastException e) {
        throw new IllegalArgumentException("Invalid IP address supplied", e);
      }
    }
  }

  record OptRDataDecoder() implements RDataDecoder {

    @Override
    public ResourceRecord decode(String domainName, short dnsClass, DnsType type, long ttl,
        int rDataLength, int startOffset, byte[] data) {
      checkHasEnoughBytes(data, startOffset, rDataLength);
      var options = new ArrayList<Option>();

      final var optionsEnd = startOffset + rDataLength;
      while (startOffset < optionsEnd) {
        final ParsedObject<Option> parsedOption = parseOption(data, startOffset);
        options.add(parsedOption.item);
        startOffset = parsedOption.nextByteOffset;
      }

      return new Opt(domainName, dnsClass, ttl, options);
    }

    private ParsedObject<Option> parseOption(byte[] data, int offset) {
      if (data.length - offset < MIN_OPT_OPTION_LENGTH) {
        throw new IllegalArgumentException(
            "Cannot parse option, expected 8 bytes. Received " + (data.length - offset));
      }

      final short optionCode = read2BytesUnsignedAsShort(data, offset);
      offset += 2;

      final short optionLength = read2BytesUnsignedAsShort(data, offset);
      offset += 2;

      if (data.length - offset < optionLength) {
        throw new IllegalArgumentException(
            "Expected " + optionLength + " bytes for options but have " + (data.length - offset));
      }

      final var optionData = new byte[optionLength];

      System.arraycopy(data, offset, optionData, 0, optionLength);
      offset += optionLength;

      return new ParsedObject<>(new Option(optionCode, optionLength, optionData), offset);
    }
  }
}
