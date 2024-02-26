package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedPointerLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedValueLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.ParsedDomainName;
import io.github.akmal2409.dnsforwarder.server.shared.CodecUtils;
import io.github.akmal2409.dnsforwarder.server.shared.CodecUtils.PointerLabel;
import io.github.akmal2409.dnsforwarder.server.shared.CodecUtils.ValueLabel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DnsDomainNameDecoderTest {

  @Test
  @DisplayName("Fails when the label sequence is not terminated")
  void failsOnInvalidLabelSequence() {
    final var labels = List.of(new ValueLabel("abc"),
        new ValueLabel("cba"));
    final var encoded = CodecUtils.encodedLabels(labels);
    encoded[encoded.length - 1] = (byte) 0xff;

    assertThatThrownBy(() -> new DnsDomainNameDecoder(encoded).decodeStartingAt(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Domain name cannot be parsed, failed to find terminated label sequence or a pointer");
  }

  @Test
  @DisplayName("Fails when reported length of a label doesn't match")
  void failsWhenLengthDoesntMatch() {
    final var labels = List.of(new ValueLabel("abc"),
        new ValueLabel("cba"));
    final var encoded = CodecUtils.encodedLabels(labels);

    encoded[4] = (byte) 0x4;

    assertThatThrownBy(() -> new DnsDomainNameDecoder(encoded).decodeStartingAt(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Malformed label length. Not enough bytes");
  }

  @Test
  @DisplayName("Fails when pointer is malformed")
  void failsWhenPointerIsMalformed() {
    final var labels = List.of(new ValueLabel("ab"),
        new PointerLabel(100));
    final var encoded = CodecUtils.encodedLabels(labels);

    final var withInvalidPointer = new byte[encoded.length - 1];
    System.arraycopy(encoded, 0, withInvalidPointer, 0, encoded.length - 1);

    assertThatThrownBy(() -> new DnsDomainNameDecoder(withInvalidPointer).decodeStartingAt(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Domain name cannot be parsed, failed to find terminated label sequence or a pointer");
  }

  @Test
  @DisplayName("Can parse valid label sequence")
  void parsesValidLabelSequence() {
    final var expectedLabels = List.of("mail", "google", "co", "uk", "1799");
    final var labels = expectedLabels.stream()
                           .map(value -> {
                             try {
                               var pointer = Integer.parseInt(value);
                               return new PointerLabel(pointer);
                             } catch (NumberFormatException ignored) {
                               return new ValueLabel(value);
                             }
                           }).toList();
    final var expectedLabelObjects = List.of(
        new DecodedValueLabel(3, "mail"),
        new DecodedValueLabel(8, "google"),
        new DecodedValueLabel(15, "co"),
        new DecodedValueLabel(18, "uk"),
        new DecodedPointerLabel(21, 1799)
    );
    final var encoded = CodecUtils.encodedLabels(labels);
    final var paddedData = new byte[encoded.length + 5]; // 3 at start, 2 at the end

    paddedData[0] = (byte) 0x5;
    paddedData[1] = (byte) 0x6;
    paddedData[2] = (byte) 0x7;
    System.arraycopy(encoded, 0, paddedData, 3, encoded.length);
    paddedData[encoded.length + 3] = (byte) 0x5;
    paddedData[encoded.length + 4] = (byte) 0x8;

    final var actualDomainName = new DnsDomainNameDecoder(paddedData).decodeStartingAt(3);

    assertThat(actualDomainName.startOffset()).isEqualTo(3);
    assertThat(actualDomainName.endOffset()).isEqualTo(encoded.length + 3);


    assertThat(actualDomainName.labels())
        .usingRecursiveComparison()
        .isEqualTo(expectedLabelObjects);
  }


  @Test
  @DisplayName("Should parse root domain name")
  void shouldParseRootDomainName() {
    // root domain name consists of 1 null byte
    final var bytes = new byte[1];
    final var expectedParsed = new ParsedDomainName(0, 1,
        List.of(new DecodedValueLabel(0, null)));

    final var actualParsed = new DnsDomainNameDecoder(bytes).decodeStartingAt(0);

    assertThat(actualParsed)
        .usingRecursiveComparison()
        .isEqualTo(expectedParsed);
  }
}
