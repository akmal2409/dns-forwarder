package io.github.akmal2409.dnsforwarder.server.codec.models;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc1035">RFC 1035 Header Format</a>
 *
 * @param id                  of the query, copied into reply
 * @param query               true if a query, false if answer
 * @param opcode              4-bit field 0=QUERY 1=IQUERY 2=STATUS rest is reserved
 * @param authoritativeAnswer if the server is authoritative server
 * @param truncation          if the response has been truncated
 * @param recursionDesired    if the caller prefers the recursive query
 * @param recursionAvailable  if the server supports recursive queries
 * @param reserved            reserved bit
 * @param responseCode        4-bit field
 * @param questionCount       number of questions
 * @param answerCount         number of answers
 * @param nameServerCount     number of authoritative name server RRs
 * @param additionalCount     number of additional RRs
 */
public record DnsHeader(
    short id,
    boolean query,
    byte opcode,
    boolean authoritativeAnswer,
    boolean truncation,
    boolean recursionDesired,
    boolean recursionAvailable,
    boolean reserved,
    byte responseCode,
    int questionCount,
    int answerCount,
    int nameServerCount,
    int additionalCount
) {

}
