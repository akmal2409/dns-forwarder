package io.github.akmal2409.dnsforwarder.server.common;

public class ByteUtils {

  private ByteUtils() {
  }

  public static int read2BytesAsInt(byte[] bytes, int start) {
    return ((bytes[start] & 0xff) << 8) | (0xff & bytes[start + 1]);
  }

  public static short read2BytesUnsignedAsShort(byte[] bytes, int start) {
    return (short) (
        ((bytes[start] & 0xff) << 8) | (0xff & bytes[start + 1])
    );
  }

  public static long read4BytesUnsignedAsLong(byte[] bytes, int start) {
    return ((long) (bytes[start] & 0xff) << 24)
               | ((long) (bytes[start + 1] & 0xff) << 16)
               | ((long) (bytes[start + 2] & 0xff) << 8)
               | (long) (bytes[start + 3] & 0xff);
  }

  public static int readInt(byte[] bytes, int start) {
    int num = 0;

    for (int i = start; i < Math.min(start + 4, bytes.length); i++) {
      num |= (0xff & bytes[i]) << ((3 - (i - start)) * Byte.SIZE);
    }

    return num;
  }

  public static long readLong(byte[] bytes, int start) {
    long num = 0L;

    for (int i = start; i < Math.min(start + 8, bytes.length); i++) {
      num |= ((long) (0xff & bytes[i])) << ((3 - (i - start)) * Byte.SIZE);
    }

    return num;
  }

  public static boolean isBitSet(byte b, int bitIdx) {
    return (b & (1 << bitIdx)) != 0;
  }

  public static void writeShort(byte[] bytes, int offset, int direction, short value) {
    bytes[offset] = (byte) (value >>> 8);
    bytes[offset + direction] = (byte) value;
  }

  public static void writeInt(byte[] bytes, int offset, int direction, int value) {
    for (int i = 0; i < Integer.BYTES; i++) {
      bytes[offset + (Integer.BYTES - i - 1) * direction] = (byte) value;
      value >>>= 8;
    }
  }
}
