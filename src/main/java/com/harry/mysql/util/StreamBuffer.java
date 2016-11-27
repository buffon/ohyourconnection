package com.harry.mysql.util;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by chenyehui on 16/11/25.
 */
public class StreamBuffer {

    public byte[] packageLengthBytes = new byte[3];

    public Integer packageLength = 0;

    public Integer packageIdByte;

    public byte[] body;

    public int position = 0;

    private InputStream inputStream;

    private OutputStream outputStream;


    /**
     * first package
     */
    public byte protocolVersion;

    public byte[] serverVersion;

    public byte[] threadIdBytes;

    public long threadId;

    public byte[] seedBytes;

    public byte[] serverCapabilitiesBytes;

    public Integer serverCapabilities;

    public byte serverCharsetIndexByte;

    public byte[] serverStatusBytes;

    public Integer serverStatus;

    public byte[] restOfScrambleBuffBytes;

    public StreamBuffer(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void analyzeHeader() throws Exception {
        inputStream.read(packageLengthBytes);
        packageLength = ByteUtil.readUB3(packageLengthBytes);
        packageIdByte = inputStream.read();

        byte[] bodyBytes = new byte[packageLength];
        inputStream.read(bodyBytes);
        body = bodyBytes;
    }

    public void retrieveFirstPackage() throws Exception {
        protocolVersion = body[position++];
        serverVersion = readBytesWithNull();
        threadId = readUB4();
        seedBytes = readBytesWithNull();

        serverCapabilities = readUB2();
        serverCharsetIndexByte = readByte();

        serverStatus = readUB2();
        move(13);
        restOfScrambleBuffBytes = readBytesWithNull();

        position = 0; // reset position = 0;

        System.out.println("Mysql First Package analyze OK.");
    }

    public void auth(String username, String password, String db) throws Exception {
        byte[] authSeed = ByteUtil.mergeBytes(seedBytes, restOfScrambleBuffBytes);

        byte[] pass = null;
        if (password == null || password.equals("")) {
//            pass = [0];
        } else {
            pass = SecurityUtil.scramble411(password.getBytes(), authSeed);
        }

        int size = 32;
        size += username.length() + 1;

        if (password == null || password.equals("")) {
            size += 1;
        } else {
            size += getPassLength(pass);
        }
        size += db.length() + 1;
        writeUB3(size);
        writeByte((byte) 1);
        writeUB4(ClientFlag.getClientFlags());
        writeUB4(Constants.MAX_PACKET_SIZE);

        writeByte(serverCharsetIndexByte);
        writeBytes(Constants.FILLER);

        writeBytes(username.getBytes());
        writeInt(0);

        if (pass != null) {
            writeWithLength(pass);
        } else {
            writeInt(0);
        }

        writeBytes(db.getBytes());
        writeInt(0);
        outputStream.flush();
    }

    public int getPassLength(byte[] src) {
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

    public void writeShowDatabases() throws Exception {
        byte[] arg = "show databases;".getBytes();
        // length : command + sql
        writeUB3(1 + arg.length);
        // packageId
        writeByte((byte) 1);
        // query command
        writeByte((byte) 3);
        // sql
        writeBytes(arg);

        outputStream.flush();
    }

    // TODO handle mysql query command resultset
    public void retrieveShowDatabases() {
        byte res = body[0];
        if (res == 0x00) {
            System.out.println("show databases success");
        } else if (res == 0xff) {
            System.out.println("show databases fail");
        } else if (res == 0xfe) {
            System.out.println("show databases EOF");
        } else {

        }
    }

    public void firstPackageToStr() {
        System.out.println("packageLength " + packageLength);
        System.out.println("packageId " + packageIdByte);
        System.out.println("protocolVersion " + protocolVersion);
        System.out.println("serverVersion length " + serverVersion.length);
        System.out.println("threadId " + threadId);
        System.out.println("seed length " + seedBytes.length);
        System.out.println("serverCapabilities " + serverCapabilities);
        System.out.println("serverCharsetIndex " + serverCharsetIndexByte);
        System.out.println("serverStatus " + serverStatus);
        System.out.println("restOfScrambleBuff length " + restOfScrambleBuffBytes.length);
    }

    public byte[] readBytesWithNull() {
        final byte[] b = body;
        if (position >= packageLength) {
            return Constants.EMPTY_BYTES;
        }
        int offset = -1;
        for (int i = position; i < packageLength; i++) {
            if (b[i] == 0) {
                offset = i;
                break;
            }
        }
        switch (offset) {
            case -1:
                byte[] ab1 = new byte[packageLength - position];
                System.arraycopy(b, position, ab1, 0, ab1.length);
                position = packageLength;
                return ab1;
            case 0:
                position++;
                return Constants.EMPTY_BYTES;
            default:
                byte[] ab2 = new byte[offset - position];
                System.arraycopy(b, position, ab2, 0, ab2.length);
                position = offset + 1;
                return ab2;
        }
    }

    public long readUB4() {
        int i = body[position++] & 0xff;
        i |= body[position++] & 0xff << 8;
        i |= body[position++] & 0xff << 16;
        i |= body[position++] & 0xff << 24;
        return i;
    }

    public Integer readUB3() {
        int i = body[position++] & 0xff;
        i |= body[position++] & 0xff << 8;
        i |= body[position++] & 0xff << 16;
        return i;
    }

    public Integer readUB2() {
        int i = body[position++] & 0xff;
        i |= body[position++] & 0xff << 8;
        return i;
    }

    public byte readByte() {
        return body[position++];
    }

    public void move(Integer i) {
        position += i;
    }

    public void writeByte(byte b) throws Exception {
        outputStream.write(b);
    }

    public void writeInt(int b) throws Exception {
        outputStream.write(b);
    }

    public void writeBytes(byte[] b) throws Exception {
        outputStream.write(b);
    }

    public void writeUB2(long l) throws Exception {
        byte[] bytes = {(byte) (l & 0xff), (byte) (l >>> 8)};
        outputStream.write(bytes);
    }

    public void writeUB3(long l) throws Exception {
        byte[] bytes = {(byte) (l & 0xff), (byte) (l >>> 8), (byte) (l >>> 16)};
        outputStream.write(bytes);
    }

    public void writeUB4(long l) throws Exception {
        byte[] bytes = {(byte) (l & 0xff), (byte) (l >>> 8), (byte) (l >>> 16), (byte) (l >>> 24)};
        outputStream.write(bytes);
    }

    public void writeLong(long l) throws Exception {
        outputStream.write((byte) (l & 0xff));
        outputStream.write((byte) (l >>> 8));
        outputStream.write((byte) (l >>> 16));
        outputStream.write((byte) (l >>> 24));
        outputStream.write((byte) (l >>> 32));
        outputStream.write((byte) (l >>> 40));
        outputStream.write((byte) (l >>> 48));
        outputStream.write((byte) (l >>> 56));
    }

    public void writeWithLength(byte[] src) throws Exception {
        int length = src.length;
        if (length < 251) {
            outputStream.write((byte) length);
        } else if (length < 0x10000L) {
            outputStream.write((byte) 252);
            writeUB2(length);
        } else if (length < 0x1000000L) {
            outputStream.write((byte) 253);
            writeUB3(length);
        } else {
            outputStream.write((byte) 254);
            writeLong(length);
        }
        outputStream.write(src);
    }

    public boolean checkAuth() {
        int res = readByte();

        position = 0;

        if (res == 0x00) {
            System.out.println("[AUTH RESULT] Success");
            return true;
        } else if (res == 0xff) {
            System.out.println("[AUTH RESULT] Fail");
            return false;
        } else {
            System.out.println("[AUTH RESULT] Unknown Error");
            return false;
        }
    }
}
