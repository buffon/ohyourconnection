package com.harry.mysql.util;

/**
 * Created by chenyehui on 16/11/26.
 */
public class ByteUtil {

    public static byte[] mergeBytes(byte[] a, byte[] b){
        byte[] res = new byte[a.length + b.length];
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }

    public byte read(byte[] bytes) {
        return bytes[0];
    }

    public static int readUB2(byte[] bytes) {
        int i = bytes[0] & 0xff;
        i |= bytes[1] & 0xff << 8;
        return i;
    }

    public static int readUB3(byte[] bytes) {
        int i = bytes[0] & 0xff;
        i |= bytes[1] & 0xff << 8;
        i |= bytes[2] & 0xff << 16;
        return i;
    }

    public static long readUB4(byte[] bytes) {
        int i = bytes[0] & 0xff;
        i |= bytes[1] & 0xff << 8;
        i |= bytes[2] & 0xff << 16;
        i |= bytes[3] & 0xff << 24;
        return i;
    }
}
