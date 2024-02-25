package io.github.akmal2409.dnsforwarder.server.codec.models;

public enum DnsQuestionClass {
  ANY("Any", 255);

  final String name;
  final int value;

  DnsQuestionClass(String name, int value) {
    this.name = name;
    this.value = value;
  }
}
