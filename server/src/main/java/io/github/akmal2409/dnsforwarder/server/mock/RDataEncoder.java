package io.github.akmal2409.dnsforwarder.server.mock;

import static io.github.akmal2409.dnsforwarder.server.common.ByteArrayUtils.merge;

import io.github.akmal2409.dnsforwarder.server.codec.encoders.CharacterStringEncoder;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ARecord;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Cname;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.HInfo;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.NS;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Opt;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Opt.Option;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ResourceRecord;
import io.github.akmal2409.dnsforwarder.server.common.ByteUtils;

class RDataEncoder {

  private static final CharacterStringEncoder charStringEncoder = new CharacterStringEncoder();
  private static final DnsStringCodec dnsStringCodec = new DnsStringCodec();

  public static byte[] encode(ResourceRecord resourceRecord) {
    return switch (resourceRecord) {
      case ARecord aRecord -> aRecord.address().getAddress();
      case Cname cname -> dnsStringCodec.encode(cname.domain());
      case HInfo hInfo -> encodeHInfo(hInfo);
      case Opt opt -> encodeOpt(opt);
      case NS ns -> dnsStringCodec.encode(ns.nameServerDomain());
      case null -> throw new IllegalArgumentException(
          "Unsupported resource record for encoding " + resourceRecord.getClass().getName());
    };
  }

  private static byte[] encodeOpt(Opt opt) {
    var totalSize = 0;

    for (Option option : opt.options()) {
      totalSize += 4 + option.data().length;
    }

    final var data = new byte[totalSize];
    int offset = 0;

    for (Option option : opt.options()) {
      ByteUtils.writeShort(data, offset, 1, option.code());
      offset += 2;
      ByteUtils.writeShort(data, offset, 1, (short) option.data().length);
      offset += 2;

      System.arraycopy(option.data(), 0, data, offset, option.data().length);
      offset += option.data().length;
    }

    return data;
  }

  private static byte[] encodeHInfo(HInfo hInfo) {
    final var cpuBytes = charStringEncoder.encode(hInfo.cpu());
    final var osBytes = charStringEncoder.encode(hInfo.os());

    return merge(cpuBytes, osBytes);
  }


}
