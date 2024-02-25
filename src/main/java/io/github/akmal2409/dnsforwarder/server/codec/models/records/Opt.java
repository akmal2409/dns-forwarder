package io.github.akmal2409.dnsforwarder.server.codec.models.records;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class Opt extends ResourceRecord {

  private final List<Option> options;
  private final short udpPayloadSize;

  public Opt(String domainName, short udpPayloadSize, long ttl,
      List<Option> options) {
    super(domainName, null, ttl);
    this.udpPayloadSize = udpPayloadSize;
    this.options = options;
  }

  @Override
  public DnsType type() {
    return DnsType.OPT;
  }

  public List<Option> options() {
    return options;
  }

  public short udpPayloadSize() {
    return udpPayloadSize;
  }

  @Override
  public boolean cacheable() {
    // https://datatracker.ietf.org/doc/html/rfc6891#section-6
    return false; // ttl is ignored for this record
  }

  @Override
  public String toString() {
    return "Opt{" +
               "options=" + options +
               ", domainName='" + domainName + '\'' +
               ", udpPayloadSize=" + udpPayloadSize +
               ", ttl=" + ttl +
               '}';
  }

  @Override
  public short numericDnsClass() {
    return udpPayloadSize;
  }

  public static record Option(
      short code,
      short length,
      byte[] data
  ) {

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Option option = (Option) o;

      if (code != option.code) {
        return false;
      }
      if (length != option.length) {
        return false;
      }
      return Arrays.equals(data, option.data);
    }

    @Override
    public int hashCode() {
      int result = 0xff & code;
      result = 31 * result + (length & 0xff);
      result = 31 * result + Arrays.hashCode(data);
      return result;
    }

    @Override
    public String toString() {
      return "Option{" +
                 "code=" + code +
                 ", length=" + length +
                 ", data=" + new String(data, StandardCharsets.US_ASCII) +
                 '}';
    }
  }
}
