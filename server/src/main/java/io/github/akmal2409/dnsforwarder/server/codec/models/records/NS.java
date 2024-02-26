package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;

public final class NS extends ResourceRecord {
  private final String nameServerDomain;

  public NS(String domainName, DnsClass dnsClass, long ttl,
      String nameServerDomain) {
    super(domainName, dnsClass, ttl);
    this.nameServerDomain = nameServerDomain;
  }

  @Override
  public DnsType type() {
    return DnsType.NS;
  }

  public String nameServerDomain() {
    return this.nameServerDomain;
  }
}
