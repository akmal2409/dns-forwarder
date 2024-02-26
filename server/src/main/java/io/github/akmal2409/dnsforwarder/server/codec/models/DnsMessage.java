package io.github.akmal2409.dnsforwarder.server.codec.models;

import io.github.akmal2409.dnsforwarder.server.codec.models.records.ResourceRecord;
import java.util.Arrays;
import java.util.Objects;

public record DnsMessage(
    DnsHeader header,
    DnsQuestion[] questions,
    ResourceRecord[] answers,
    ResourceRecord[] nameServers,
    ResourceRecord[] additional
) {

  public boolean isQuery() {
    return header.query();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DnsMessage that = (DnsMessage) o;

    if (!Objects.equals(header, that.header)) {
      return false;
    }
    if (!Arrays.equals(questions, that.questions)) {
      return false;
    }

    if (!Arrays.equals(answers, that.answers)) {
      return false;
    }

    if (!Arrays.equals(nameServers, that.nameServers)) {
      return false;
    }

    return Arrays.equals(additional, that.additional);
  }

  @Override
  public int hashCode() {
    int result = header != null ? header.hashCode() : 0;
    result = 31 * result + Arrays.hashCode(questions);
    result = 31 * result + Arrays.hashCode(answers);
    result = 31 * result + Arrays.hashCode(nameServers);
    result = 31 * result + Arrays.hashCode(additional);
    return result;
  }

  @Override
  public String toString() {
    return "DnsMessage{" +
               "header=" + header +
               ", questions=" + Arrays.toString(questions) +
               ", answers=" + Arrays.toString(answers) +
               ", nameServers=" + Arrays.toString(nameServers) +
               ", additional=" + Arrays.toString(additional) +
               '}';
  }
}
