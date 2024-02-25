package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;

public final class Cname extends ResourceRecord {

  private final String domain;

  public Cname(String domainName, DnsClass dnsClass, long ttl, String domain) {
    super(domainName, dnsClass, ttl);
    this.domain = domain;
  }

  @Override
  public boolean compressable() {
    return true;
  }

  @Override
  public DnsType type() {
    return DnsType.CNAME;
  }

  public String domain() {
    return domain;
  }
}
