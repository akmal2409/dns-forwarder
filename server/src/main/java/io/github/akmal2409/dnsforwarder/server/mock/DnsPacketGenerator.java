package io.github.akmal2409.dnsforwarder.server.mock;

import io.github.akmal2409.dnsforwarder.server.codec.models.DnsQuestion;
import io.github.akmal2409.dnsforwarder.server.codec.models.records.ResourceRecord;
import io.github.akmal2409.dnsforwarder.server.common.ByteArrayUtils;
import io.github.akmal2409.dnsforwarder.server.common.ByteUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class DnsPacketGenerator {

  private static final DnsStringCodec codec = new DnsStringCodec();
  private static final Random random = new Random();

  public static byte[] createMessage(Consumer<MessageCustomizer> customizer) {
    final var packetProps = new MessageCustomizer();

    customizer.accept(packetProps);

    final var header = createHeader(packetProps);

    List<byte[]> questionBytes = new ArrayList<>();
    int messageBytesLength = header.length;

    for (DnsQuestion question : packetProps.questions) {
      questionBytes.add(createQuestion(question));
      messageBytesLength += questionBytes.getLast().length;
    }

    final List<byte[]> answersBytes = createResourceRecords(packetProps.answers);
    final List<byte[]> nameServerBytes = createResourceRecords(packetProps.nameServers);
    final List<byte[]> additionalBytes = createResourceRecords(packetProps.additional);

    messageBytesLength += ByteArrayUtils.totalSizeOf(answersBytes)
                              + ByteArrayUtils.totalSizeOf(nameServerBytes)
                              + ByteArrayUtils.totalSizeOf(additionalBytes);

    byte[] data = new byte[messageBytesLength];

    System.arraycopy(header, 0, data, 0, header.length);

    ByteArrayUtils.copyByteArrayList(
        Stream.of(questionBytes, answersBytes, nameServerBytes, additionalBytes)
            .flatMap(List::stream)
            .toList(), data, header.length
    );

    return data;
  }

  public static List<byte[]> createResourceRecords(List<ResourceRecord> resourceRecords) {
    final List<byte[]> byteList = new ArrayList<>();

    for (ResourceRecord resourceRecord : resourceRecords) {
      // always present name, type, class, ttl, rdLength
      // variable rdata
      final byte[] encodedName = codec.encode(resourceRecord.domainName);
      final short type = resourceRecord.type().numericValue;
      final short dnsClass = resourceRecord.numericDnsClass();
      final int ttl = (int) resourceRecord.ttl;
      final byte[] rData = RDataEncoder.encode(resourceRecord);

      final byte[] data = new byte[encodedName.length + 10 + rData.length];

      System.arraycopy(encodedName, 0, data, 0, encodedName.length);

      ByteUtils.writeShort(data, encodedName.length, 1, type);
      ByteUtils.writeShort(data, encodedName.length + 2, 1, dnsClass);
      ByteUtils.writeInt(data, encodedName.length + 4, 1, ttl);
      ByteUtils.writeShort(data, encodedName.length + 8, 1, (short) rData.length);

      System.arraycopy(rData, 0, data, encodedName.length + 10, rData.length);

      byteList.add(data);
    }

    return byteList;
  }

  public static byte[] createQuestion(DnsQuestion question) {
    final byte[] questionName = codec.encode(question.name());

    final byte[] data = new byte[questionName.length + 4];

    System.arraycopy(questionName, 0, data, 0, questionName.length);

    set2Bytes(question.type().numericValue, data, questionName.length, questionName.length + 1);
    set2Bytes(question.queryClass().numericValue, data, questionName.length + 2,
        questionName.length + 3);

    return data;
  }

  public static byte[] createHeader(MessageCustomizer customizer) {
    byte[] header = new byte[12];
    set2Bytes(customizer.id, header, 0, 1);

    if (!customizer.query) {
      header[2] = (byte) (1 << 7);
    }

    header[2] |= (byte) ((0xf & customizer.opcode) << 3);

    if (customizer.authoritativeAnswer) {
      header[2] |= (1 << 2);
    }
    if (customizer.truncation) {
      header[2] |= (1 << 1);
    }
    if (customizer.recursionDesired) {
      header[2] |= 1;
    }

    if (customizer.recursionAvailable) {
      header[3] = (byte) (1 << 7);
    }

    header[3] |= (byte) (customizer.responseCode & 0xf);

    set2Bytes(customizer.questions.size(), header, 4, 5);
    set2Bytes(customizer.answers.size(), header, 6, 7);
    set2Bytes(customizer.nameServers.size(), header, 8, 9);
    set2Bytes(customizer.additional.size(), header, 10, 11);

    return header;
  }

  public static void set2Bytes(int data, byte[] bytes, int firstPart, int secondPart) {
    bytes[firstPart] = (byte) ((0xff00 & data) >> 8);
    bytes[secondPart] = (byte) (0xff & data);
  }

  public static class MessageCustomizer {

    private final List<DnsQuestion> questions;
    private final List<ResourceRecord> answers;
    private final List<ResourceRecord> nameServers;
    private final List<ResourceRecord> additional;
    private short id = (short) (random.nextInt() & 0xffff);
    private boolean query = random.nextBoolean();
    private byte opcode;
    private boolean authoritativeAnswer;
    private boolean truncation;
    private boolean recursionDesired;
    private boolean recursionAvailable;
    private byte responseCode;

    public MessageCustomizer() {
      this.questions = new ArrayList<>();
      this.answers = new ArrayList<>();
      this.nameServers = new ArrayList<>();
      this.additional = new ArrayList<>();
    }

    public MessageCustomizer id(short id) {
      this.id = id;
      return this;
    }

    public MessageCustomizer query(boolean query) {
      this.query = query;
      return this;
    }

    public MessageCustomizer opcode(byte opcode) {
      this.opcode = opcode;
      return this;
    }

    public MessageCustomizer authoritativeAnswer(boolean answer) {
      this.authoritativeAnswer = answer;
      return this;
    }

    public MessageCustomizer truncation(boolean truncation) {
      this.truncation = truncation;
      return this;
    }

    public MessageCustomizer recursionDesired(boolean recursionDesired) {
      this.recursionDesired = recursionDesired;
      return this;
    }

    public MessageCustomizer recursionAvailable(boolean available) {
      this.recursionAvailable = available;
      return this;
    }

    public MessageCustomizer responseCode(byte code) {
      this.responseCode = code;
      return this;
    }

    public MessageCustomizer question(DnsQuestion question) {
      this.questions.add(question);
      return this;
    }

    public MessageCustomizer questions(List<DnsQuestion> questions) {
      questions.forEach(this::question);
      return this;
    }

    public MessageCustomizer answer(ResourceRecord answer) {
      this.answers.add(answer);
      return this;
    }

    public MessageCustomizer answers(Iterable<ResourceRecord> answers) {
      answers.forEach(this::answer);
      return this;
    }

    public MessageCustomizer nameServer(ResourceRecord nameServer) {
      this.nameServers.add(nameServer);
      return this;
    }

    public MessageCustomizer additional(ResourceRecord resourceRecord) {
      this.additional.add(resourceRecord);
      return this;
    }
  }
}
