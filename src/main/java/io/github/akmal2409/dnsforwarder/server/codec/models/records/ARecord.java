package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import java.net.Inet4Address;

public final class ARecord extends ResourceRecord {

  private final Inet4Address address;

  public ARecord(String domainName, DnsClass dnsClass,
      long ttl, Inet4Address address) {
    super(domainName, dnsClass, ttl);
    this.address = address;
  }

  @Override
  public DnsType type() {
    return DnsType.A;
  }

  public Inet4Address address() {
    return this.address;
  }

  @Override
  public String toString() {
    return "ARecord{" +
               "address=" + address +
               ", domainName='" + domainName + '\'' +
               ", dnsClass=" + dnsClass +
               ", ttl=" + ttl +
               '}';
  }
}
