package io.github.akmal2409.dnsforwarder.server.codec.models;

public record DnsQuestion(
    String name, // domain name
    DnsType type,
    DnsClass queryClass
) {

}
