package io.github.akmal2409.dnsforwarder.server.codec.models;


public enum DnsType {
  A((short) 1), NS((short) 2), MD((short) 3), MF((short) 4), CNAME((short) 5), SOA((short) 6),
  MB((short) 7), MG((short) 8), MR((short) 9), NULL((short) 10),
  WKS((short) 11), PTR((short) 12), HIINFO((short) 13), MINFO((short) 14),
  MX((short) 15), TXT((short) 16), OPT((short) 41),

  // Query Types only
  AXFR((short) 252), MAILB((short) 253), MAILA((short) 254), ANY((short) 255);

  public final short numericValue;

  DnsType(short value) {
    this.numericValue = value;
  }

  public static DnsType from(short value) {
    for (DnsType type : values()) {
      if (type.numericValue == value) {
        return type;
      }
    }

    return null;
  }
}
