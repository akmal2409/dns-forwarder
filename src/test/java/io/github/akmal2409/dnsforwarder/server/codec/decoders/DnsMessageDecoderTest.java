package io.github.akmal2409.dnsforwarder.server.codec.decoders;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsClass;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsHeader;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsMessage;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsQuestion;
import io.github.akmal2409.dnsforwarder.server.codec.models.DnsType;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ARecord;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Cname;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.HInfo;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Opt;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.Opt.Option;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ResourceRecord;
import io.github.akmal2409.dnsforwarder.server.mock.DnsPacketGenerator;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DnsMessageDecoderTest {

  @Test
  @DisplayName("Parses header correctly")
  void canParseHeaderCorrectly() {
    final var expected = new DnsMessage(
        new DnsHeader((short) 1111, true, (byte) 1, true, true, true, true, false, (byte) 1, 0, 0,
            0, 0), new DnsQuestion[0], new ResourceRecord[0], new ResourceRecord[0],
        new ResourceRecord[0]);

    final var query = DnsPacketGenerator.createMessage(c -> {
      c.id(expected.header().id());
      c.opcode(expected.header().opcode());
      c.recursionDesired(expected.header().recursionDesired());
      c.query(expected.header().query());
      c.authoritativeAnswer(expected.header().authoritativeAnswer());
      c.truncation(expected.header().truncation());
      c.recursionAvailable(expected.header().recursionAvailable());
      c.responseCode(expected.header().responseCode());
    });

    final var decoder = DnsMessageDecoder.fromBytes(query);
    final DnsMessage actualMessage = decoder.decode();

    assertThat(actualMessage).usingRecursiveComparison()
        .ignoringFields("questions", "answers", "nameServers", "additional").isEqualTo(expected);
  }

  @Test
  @DisplayName("Can parse complete DNS query with 1 query entry")
  void canParseCompleteDnsQueryWith1Query() {
    final var expectedQuestions = new DnsQuestion[]{
        new DnsQuestion("google.com", DnsType.A, DnsClass.IN),
        new DnsQuestion("mail.google.com", DnsType.MX, DnsClass.IN)};

    final var expected = new DnsMessage(
        new DnsHeader((short) 1111, true, (byte) 0, false, false, true, false, false, (byte) 0, 2,
            0, 0, 0), expectedQuestions, new ResourceRecord[0], new ResourceRecord[0],
        new ResourceRecord[0]);

    final var query = DnsPacketGenerator.createMessage(c -> {
      c.id(expected.header().id());
      c.opcode(expected.header().opcode());
      c.recursionDesired(expected.header().recursionDesired());
      c.query(expected.header().query());
      c.questions(Arrays.asList(expectedQuestions));
    });

    final var decoder = DnsMessageDecoder.fromBytes(query);
    final DnsMessage actualMessage = decoder.decode();

    assertThat(actualMessage).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  @DisplayName("Can parse complete A record answers")
  void canParseAnswerWithARecord() throws UnknownHostException {
    final var expectedAnswers = List.<ResourceRecord>of(new ARecord("google.com", DnsClass.IN, 100,
        (Inet4Address) Inet4Address.getByName("10.189.11.12")),
        new ARecord("google.com", DnsClass.IN, 100,
            (Inet4Address) Inet4Address.getByName("10.189.11.13")));

    canParseAnswers(expectedAnswers);
  }

  @Test
  @DisplayName("Can parse complete Cname answers")
  void canParseCNameAnswers() {
    final var expectedAnswers = List.<ResourceRecord>of(
        new Cname("gslb.google.com", DnsClass.IN, 110, "google.com"),
        new Cname("gslb.backup.google.com", DnsClass.IN, 110, "backup.google.com")
    );

    canParseAnswers(expectedAnswers);
  }

  @Test
  @DisplayName("Can parse complete HInfo answers")
  void canParseHInfoAnswers() {
    final var expectedAnswers = List.<ResourceRecord>of(
        new HInfo("google.com", DnsClass.IN, 120, "arm64", "Linux"),
        new HInfo("mail.google.com", DnsClass.IN, 110, "arm64", "Darwin")
    );

    canParseAnswers(expectedAnswers);
  }

  @Test
  @DisplayName("Can parse complete Opt answers")
  void canParseOptAnswers() {
    final var expectedAnswers = List.<ResourceRecord>of(
       new Opt("google.com", (short) 110, 90, List.of(
           new Option((short) 110, (short) 6, new byte[]{0x1, 0x2, 0x3, 0x4, 0x5, 0x6}),
           new Option((short) 112, (short) 5, new byte[]{0x6, 0x7, 0x8, 0x9, 0x10})
       )),
        new Opt("google.com", (short) 111, 10, List.of(
            new Option((short) 111, (short) 3, new byte[]{0x1, 0x2, 0x3})
        ))
    );

    canParseAnswers(expectedAnswers);
  }

  private static void canParseAnswers(List<ResourceRecord> expectedAnswers) {
    final var questions = new DnsQuestion[]{new DnsQuestion("google.com", DnsType.A, DnsClass.IN)};

    final var expected = new DnsMessage(answerHeader(1, 2, 0, 0), questions,
        expectedAnswers.toArray(new ResourceRecord[0]),
        new ResourceRecord[0], new ResourceRecord[0]);

    final var messageBytes = DnsPacketGenerator.createMessage(
        c -> c.id(expected.header().id())
                 .opcode(expected.header().opcode())
                 .authoritativeAnswer(expected.header().authoritativeAnswer())
                 .recursionDesired(expected.header().recursionDesired())
                 .recursionAvailable(expected.header().recursionAvailable())
                 .query(expected.header().query())
                 .questions(Arrays.asList(questions))
                 .answers(expectedAnswers));

    final var actual = DnsMessageDecoder.fromBytes(messageBytes).decode();

    assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(expected);
  }

  private static DnsHeader answerHeader(int questions, int answers, int nameservers,
      int additional) {
    return new DnsHeader((short) 1111, false, (byte) 0, true, false, true, true, false, (byte) 0,
        questions, answers, nameservers, additional);
  }

}
