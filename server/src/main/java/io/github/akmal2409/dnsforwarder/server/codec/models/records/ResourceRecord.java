package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;

public abstract sealed class ResourceRecord permits ARecord, Cname, HInfo, NS, Opt {

  public final String domainName;
  public final DnsClass dnsClass;
  public final long ttl;

  protected ResourceRecord(String domainName, DnsClass dnsClass, long ttl) {
    this.domainName = domainName;
    this.dnsClass = dnsClass;
    this.ttl = ttl;
  }

  public boolean compressable() {
    return false;
  }

  public abstract DnsType type();

  public boolean cacheable() {
    return true;
  }

  public short numericDnsClass() {
    return dnsClass.numericValue;
  }
}
