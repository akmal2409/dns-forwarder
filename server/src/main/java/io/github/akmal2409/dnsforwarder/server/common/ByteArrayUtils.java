package io.github.akmal2409.dnsforwarder.server.common;

import java.util.Arrays;
import java.util.List;

public final class ByteArrayUtils {

  private ByteArrayUtils() {
  }

  public static byte[] merge(byte[]... byteArrays) {
    final var totalSize = Arrays.stream(byteArrays)
                              .map(arr -> arr.length)
                              .mapToInt(len -> len)
                              .sum();
    final var mergedBytes = new byte[totalSize];

    int offset = 0;

    for (byte[] byteArray : byteArrays) {
      System.arraycopy(byteArray, 0, mergedBytes,
          offset, byteArray.length);
      offset += byteArray.length;
    }

    return mergedBytes;
  }

  public static int totalSizeOf(List<byte[]> byteList) {
    return byteList.stream()
               .mapToInt(bytes -> bytes.length)
               .sum();
  }

  public static void copyByteArrayList(List<byte[]> byteList, byte[] data, int offset) {
    for (byte[] bytes : byteList) {
      System.arraycopy(bytes, 0, data, offset, bytes.length);
      offset += bytes.length;
    }
  }
}
