package io.github.akmal2409.dnsforwarder.server.codec.models;

import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedPointerLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.DecodedValueLabel;
import io.github.akmal2409.dnsforwarder.server.codec.decoders.DnsDomainNameDecoder.ParsedDomainName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The class is useful when decompressing the labels. When reading the DNS message and parsing the
 * labels one of two cases can happen: Case 1) It's a simple label sequence, then we can record its
 * offset in a collection so that we can reference it later on Case 2) It's a pointer to a label we
 * previously encountered, then we can simply lookup given the offset the label(s) associated with
 * that offset.
 */
public class LabelOffsetCollection {

  // primary label pointers point to the start of the label sequence
  private final Map<Integer, List<DecodedValueLabel>> primaryLabelPointers;
  // intermediary labels' offsets will be recorded here, so that we can decompress
  // by just looking up the mapping to the original label
  private final Map<Integer, SecondaryPointer> secondaryLabelPointers;
  public LabelOffsetCollection() {
    this.primaryLabelPointers = new HashMap<>();
    this.secondaryLabelPointers = new HashMap<>();
  }

  /**
   * Given a pointer, returns the complete label sequence associated with that pointer. Useful when
   * decompressing the message.
   *
   * @param pointer offset pointing to the message elements before
   */
  public Optional<List<DecodedValueLabel>> getByPointer(int pointer) {
    final var secondaryPointer = secondaryLabelPointers.get(pointer);

    if (secondaryPointer == null) {
      return Optional.empty();
    }
    final var allLabels = primaryLabelPointers.get(secondaryPointer.primaryPointer);
    final var labels = allLabels
                           .subList(secondaryPointer.labelIndex, allLabels.size());

    return Optional.of(Collections.unmodifiableList(labels));
  }

  /**
   * Saves the offset to label mapping to the collection.
   *
   * @param labels                   labels with their offsets, their order should match the order
   *                                 in the message
   * @param labelSequenceStartOffset the offset at which the label sequence starts (including the
   *                                 length byte)
   */
  public void put(int labelSequenceStartOffset,
      List<DecodedValueLabel> labels) {
    for (int i = 0; i < labels.size(); i++) {
      final var valueLabel = labels.get(i);
      secondaryLabelPointers.put(valueLabel.offset(),
          new SecondaryPointer(labelSequenceStartOffset, i));
    }

    primaryLabelPointers.put(labelSequenceStartOffset, labels);
  }

  public List<DecodedValueLabel> put(ParsedDomainName domainName) {
    final var valueLabels = new ArrayList<DecodedValueLabel>();

    boolean containsPointer = false;

    for (DecodedLabel decodedLabel : domainName.labels()) {
      if (containsPointer) {
        break;
      }

      switch (decodedLabel) {
        case DecodedValueLabel valueLabel -> valueLabels.add(valueLabel);
        case DecodedPointerLabel pointerLabel -> {
          containsPointer = true;
          final List<DecodedValueLabel> referencedValues = this.getByPointer(
              pointerLabel.pointer()).orElseThrow(() -> new IllegalArgumentException(
              String.format("Pointer %d is referencing unknown labels at offset %d",
                  pointerLabel.pointer(), pointerLabel.offset())));
          final SecondaryPointer secondaryPointer = this.secondaryLabelPointers.get(
              pointerLabel.pointer());

          this.primaryLabelPointers.computeIfAbsent(pointerLabel.pointer(),
              ignored -> referencedValues);

          this.secondaryLabelPointers.put(pointerLabel.offset(), secondaryPointer);

          valueLabels.addAll(referencedValues);
        }
      }
    }

    this.put(domainName.startOffset(), valueLabels);

    return valueLabels;
  }

  public record SecondaryPointer(int primaryPointer, int labelIndex) {

  }
}
