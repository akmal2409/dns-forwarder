package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import io.github.akmal2409.dnsforwarder.server.codec.models.records.ResourceRecord;

public interface ResourceRecordDecoder {

  ParsedObject<ResourceRecord> decodeStartingAt(int offset);
}
