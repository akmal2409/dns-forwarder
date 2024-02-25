package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;

public final class HInfo extends ResourceRecord {

  private final String cpu;
  private final String os;

  public HInfo(String domainName, DnsClass dnsClass, long ttl, String cpu, String os) {
    super(domainName, dnsClass, ttl);
    this.cpu = cpu;
    this.os = os;
  }

  @Override
  public boolean compressable() {
    return false;
  }

  @Override
  public DnsType type() {
    return DnsType.HIINFO;
  }

  public String cpu() {
    return cpu;
  }

  public String os() {
    return os;
  }
}
