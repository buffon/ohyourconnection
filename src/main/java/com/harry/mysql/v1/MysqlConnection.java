package com.harry.mysql.v1;

import com.harry.mysql.util.ClientFlag;
import com.harry.mysql.util.SecurityUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author chenyehui
 */
@Deprecated
public class MysqlConnection {

    static class Position {
        Integer position;

        public Position(Integer position) {
            this.position = position;
        }
    }

    private static final String HOST = "localhost";
    private static final Integer PORT = 3306;
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static final String DB = "JobHunter";

    private static final long MAX_PACKET_SIZE = 1024 * 1024 * 16;

    private static final byte[] FILLER = new byte[23];

    private static final byte[] EMPTY_BYTES = new byte[0];

    public static void main(String[] args) throws Exception {
        Socket socket = new Socket(HOST, PORT);
        // socket input
        InputStream inputStream = socket.getInputStream();
        byte[] packageLengthBytes = new byte[3];
        inputStream.read(packageLengthBytes);


        byte[] packageIdBytes = new byte[1];
        inputStream.read(packageIdBytes);

        int length = readUB3(packageLengthBytes);
        System.out.println("packageLength " + length);
        int packageId = packageIdBytes[0] & 0xff;
        System.out.println("packageId " + packageId);

        byte[] bodyBytes = new byte[length];
        inputStream.read(bodyBytes);

        byte protocolVersion = bodyBytes[0];
        System.out.println("protocolVersion " + protocolVersion);

        Integer position = 1;
        Position p = new Position(position);

        byte[] serverVersion = readBytesWithNull(bodyBytes, p);
        position = p.position;
        System.out.println("serverVersion length " + serverVersion.length);

        byte[] threadIdBytes = new byte[4];
        System.arraycopy(bodyBytes, position, threadIdBytes, 0, 4);

        long threadId = readUB4(threadIdBytes);
        System.out.println("threadId " + threadId);

        position = position + 4;
        p.position = position;

        byte[] seedBytes = readBytesWithNull(bodyBytes, p);
        position = p.position;
        System.out.println("seed length " + seedBytes.length);


        byte[] serverCapabilitiesBytes = new byte[2];
        System.arraycopy(bodyBytes, position, serverCapabilitiesBytes, 0, 2);
        Integer serverCapabilities = readUB2(serverCapabilitiesBytes);
        System.out.println("serverCapabilities " + serverCapabilities);
        position = position + 2;

        byte[] serverCharsetIndexBytes = new byte[1];
        System.arraycopy(bodyBytes, position, serverCharsetIndexBytes, 0, 1);
        System.out.println("serverCharsetIndex " + serverCharsetIndexBytes[0]);
        position = position + 1;

        byte[] serverStatusBytes = new byte[2];
        System.arraycopy(bodyBytes, position, serverStatusBytes, 0, 2);
        Integer serverStatus = readUB2(serverStatusBytes);
        System.out.println("serverStatus " + serverStatus);
        position = position + 2;

        position = position + 13;

        p.position = position;

        byte[] restOfScrambleBuffBytes = readBytesWithNull(bodyBytes, p);
        position = p.position;

        System.out.println("restOfScrambleBuff length " + restOfScrambleBuffBytes.length);

        System.out.println("Mysql First Package analyze OK.");


        //socket output
        OutputStream outputStream = socket.getOutputStream();

        byte[] authSeed = new byte[seedBytes.length + restOfScrambleBuffBytes.length];
        System.arraycopy(seedBytes, 0, authSeed, 0, seedBytes.length);
        System.arraycopy(restOfScrambleBuffBytes, 0, authSeed, seedBytes.length, restOfScrambleBuffBytes.length);

        byte[] pass = SecurityUtil.scramble411(PASSWORD.getBytes(), authSeed);

        int size = 32;
        size += USERNAME.length() + 1;
//        size += getLength(pass);
        size += 1;
        size += DB.length() + 1;
        writeUB3(outputStream, size);

        outputStream.write(1);

        long clientFlag = ClientFlag.getClientFlags();
        writeUB4(outputStream, clientFlag);
        writeUB4(outputStream, MAX_PACKET_SIZE);

        outputStream.write(serverCharsetIndexBytes[0]);
        outputStream.write(FILLER);

        outputStream.write(USERNAME.getBytes());
        outputStream.write((byte) 0);

//        writeWithLength(outputStream, pass);
        outputStream.write((byte) 0);

        outputStream.write(DB.getBytes());
        outputStream.write((byte) 0);
        outputStream.flush();
        System.out.println("Send Username and Password for Auth");


//        byte[] resBytes = new byte[0];
        byte[] packageLengthBytes2 = new byte[3];
        inputStream.read(packageLengthBytes2);

        byte[] packageIdBytes2 = new byte[1];
        inputStream.read(packageIdBytes2);

        int resByte = inputStream.read();
        if (resByte == 0x00) {
            System.out.println("Auth Success");
        } else if (resByte == 0xff) {
            System.out.println("Auth Fail");
        } else {
            System.out.println("Unknown Error");
        }
    }

    // figure out why.
    public static final int getLength(byte[] src) {
        int length = src.length;
        if (length < 251) {
            return 1 + length;
        } else if (length < 0x10000L) {
            return 3 + length;
        } else if (length < 0x1000000L) {
            return 4 + length;
        } else {
            return 9 + length;
        }
    }

    public static final void writeWithLength(OutputStream outputStream, byte[] src) throws Exception {
        int length = src.length;
        if (length < 251) {
            outputStream.write((byte) length);
        } else if (length < 0x10000L) {
            outputStream.write((byte) 252);
            writeUB2(outputStream, length);
        } else if (length < 0x1000000L) {
            outputStream.write((byte) 253);
            writeUB3(outputStream, length);
        } else {
            outputStream.write((byte) 254);
            writeLong(outputStream, length);
        }
        outputStream.write(src);
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

    public static final void writeUB2(OutputStream outputStream, long l) throws Exception {
        byte[] bytes = {(byte) (l & 0xff), (byte) (l >>> 8)};
        outputStream.write(bytes);
    }

    public static final void writeUB3(OutputStream outputStream, long l) throws Exception {
        byte[] bytes = {(byte) (l & 0xff), (byte) (l >>> 8), (byte) (l >>> 16)};
        outputStream.write(bytes);
    }

    public static final void writeUB4(OutputStream outputStream, long l) throws Exception {
        byte[] bytes = {(byte) (l & 0xff), (byte) (l >>> 8), (byte) (l >>> 16), (byte) (l >>> 24)};
        outputStream.write(bytes);
    }

    public static final void writeLong(OutputStream outputStream, long l) throws Exception {
        outputStream.write((byte) (l & 0xff));
        outputStream.write((byte) (l >>> 8));
        outputStream.write((byte) (l >>> 16));
        outputStream.write((byte) (l >>> 24));
        outputStream.write((byte) (l >>> 32));
        outputStream.write((byte) (l >>> 40));
        outputStream.write((byte) (l >>> 48));
        outputStream.write((byte) (l >>> 56));
    }

    public static byte[] readBytesWithNull(byte[] data, Position p) {
        Integer position = p.position;
        Integer length = data.length;
        final byte[] b = data;
        if (position >= length) {
            return EMPTY_BYTES;
        }
        int offset = -1;
        for (int i = position; i < length; i++) {
            if (b[i] == 0) {
                offset = i;
                break;
            }
        }
        switch (offset) {
            case -1:
                byte[] ab1 = new byte[length - position];
                System.arraycopy(b, position, ab1, 0, ab1.length);
                position = length;
                p.position = position;
                return ab1;
            case 0:
                position++;
                p.position = position;
                return EMPTY_BYTES;
            default:
                byte[] ab2 = new byte[offset - position];
                System.arraycopy(b, position, ab2, 0, ab2.length);
                position = offset + 1;
                p.position = position;
                return ab2;
        }
    }
}
