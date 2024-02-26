package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import static io.github.akmal2409.dnsforwarder.server.common.ByteArrayUtils.merge;
import static io.github.akmal2409.dnsforwarder.server.shared.CodecUtils.encodeCharacterString;
import static io.github.akmal2409.dnsforwarder.server.shared.CodecUtils.encodeOptions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedValueLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.ParsedDomainName;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import io.github.akmal2409.dnsforwarder.server.codec.models.LabelOffsetCollection;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Opt.Option;
import io.github.akmal2409.dnsforwarder.server.shared.CodecUtils;
import io.github.akmal2409.dnsforwarder.server.shared.CodecUtils.ValueLabel;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RDataDecoderTest {

  @Mock
  LabelOffsetCollection labelOffsetCollection;

  @Captor
  ArgumentCaptor<ParsedDomainName> parsedDomainNameCaptor;

  @Test
  @DisplayName("Single domain name in rdata decoder works")
  void testDomainNameRdataDecodingWorks() {
    final byte[] rData = CodecUtils.encodedLabels(
        List.of(new ValueLabel("google"), new ValueLabel("com")));
    final var expectedCname = new Cname("mail.google.com", DnsClass.IN, 10, "google.com");
    final List<DecodedLabel> decodedLabels = List.of(new DecodedValueLabel(0, "google"),
        new DecodedValueLabel(7, "com"));
    final var expectedParsedDomain = new ParsedDomainName(0, rData.length,
        decodedLabels);
    final var decoder = RDataDecoder.newInstanceFor(DnsType.CNAME, labelOffsetCollection);

    when(labelOffsetCollection.put(any(ParsedDomainName.class))).thenReturn(
        decodedLabels.stream().map(DecodedValueLabel.class::cast).toList());

    final var actual = decoder.decode("mail.google.com", (short) DnsClass.IN.numericValue, DnsType.CNAME,
        10, rData.length, 0, rData);

    verify(labelOffsetCollection).put(parsedDomainNameCaptor.capture());

    assertThat(parsedDomainNameCaptor.getValue())
        .usingRecursiveComparison()
        .isEqualTo(expectedParsedDomain);

    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(expectedCname);
  }

  @Test
  @DisplayName("Can decode HiInfo Rdata")
  void testDecodesHiInfo() {
    final var expected = new HInfo("google.com", DnsClass.IN, 10, "arm64", "Debian 2");
    final var osEncoded = encodeCharacterString(expected.os());
    final var cpuEncoded = encodeCharacterString(expected.cpu());
    final var encodedRdata = merge(cpuEncoded, osEncoded);

    final var decoder = RDataDecoder.newInstanceFor(DnsType.HIINFO, new LabelOffsetCollection());
    final var actual = decoder.decode(expected.domainName, (short) expected.dnsClass.numericValue,
        expected.type(), expected.ttl, encodedRdata.length, 0, encodedRdata);

    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("Can decode ARecord")
  void testDecodesARecord() throws UnknownHostException {
    final var expectedIp = (Inet4Address) Inet4Address.getByName("127.0.0.1");
    final var expected = new ARecord("google.com", DnsClass.IN, 10,
        expectedIp);
    final var encodedRData = expectedIp.getAddress();

    final var actual = RDataDecoder.newInstanceFor(DnsType.A,
        new LabelOffsetCollection()).decode(expected.domainName,
        (short) expected.dnsClass.numericValue, expected.type(), expected.ttl,
        encodedRData.length, 0, encodedRData);

    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  @Test
  @DisplayName("Can decode OPT")
  void testDecodesOpt() {
    final var options = List.of(
        new Option((short) 10, (short) 3, new byte[]{(byte) 0x1, (byte) 0x2, (byte) 0x3}),
        new Option((short) 11, (short) 4,
            new byte[]{(byte) 0x1, (byte) 0x2, (byte) 0x3, (byte) 0x4}),
        new Option((short) 12, (short) 2, new byte[]{(byte) 0xAF, (byte) 0xAB})
    );

    final var rData = encodeOptions(options);

    final var expected = new Opt(
        "google.com", (short) 512, 10, options
    );

    final var actual = RDataDecoder.newInstanceFor(DnsType.OPT,
        new LabelOffsetCollection()).decode(expected.domainName,
        (short)512, expected.type(), expected.ttl,
        rData.length, 0, rData);

    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }
}
