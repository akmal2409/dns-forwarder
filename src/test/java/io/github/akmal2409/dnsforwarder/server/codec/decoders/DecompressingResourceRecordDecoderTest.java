package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.DecompressingResourceRecordDecoder;
import io.github.akmal2409.dnsforwarder.server.codec.models.LabelOffsetCollection;
import io.github.akmal2409.dnsforwarder.server.shared.CodecUtils;
import io.github.akmal2409.dnsforwarder.server.shared.CodecUtils.ValueLabel;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DecompressingResourceRecordDecoderTest {
  @Test
  @DisplayName("Throws exception when not enough data")
  void throwsExceptionWhenNotEnoughData() {
    final var domainLabels = List.of(
        new ValueLabel("www"), new ValueLabel("google"),
        new ValueLabel("com")
    );
    final var encodedLabels = CodecUtils.encodedLabels(domainLabels);

    final var bytes = new byte[encodedLabels.length];

    final var decoder = new DecompressingResourceRecordDecoder(encodedLabels, new LabelOffsetCollection());

    assertThatThrownBy(() -> decoder.decodeStartingAt(0))
        .isInstanceOf(IllegalArgumentException.class);
  }

}
