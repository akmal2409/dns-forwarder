package io.github.akmal2409.dnsforwarder.server.codec.models;

/**
 * Model representing the DNS classes
 * <a href="https://datatracker.ietf.org/doc/html/rfc1035#section-3.2.4">RFC 1035</a>
 */
public enum DnsClass {

  IN("Internet", (short) 1), CS("CSNET", (short) 2, true),
  CH("CHAOS", (short) 3), HS("Hesiod", (short) 4);

  public final String name;
  public final short numericValue;
  public final boolean obsolete;

  DnsClass(String name, short value, boolean obsolete) {
    this.name = name;
    this.numericValue = value;
    this.obsolete = obsolete;
  }

  DnsClass(String name, short value) {
    this.name = name;
    this.numericValue = value;
    this.obsolete = false;
  }

  public static DnsClass from(short code) {
    for (DnsClass clazz : values()) {
      if (clazz.numericValue == code) {
        return clazz;
      }
    }

    return null;
  }
}
