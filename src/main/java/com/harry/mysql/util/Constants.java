package com.harry.mysql.util;

/**
 * Created by chenyehui on 16/11/25.
 */
public interface Constants {

    public static final String HOST = "localhost";

    public static final Integer PORT = 3306;

    public static final String USERNAME = "root";

    public static final String PASSWORD = "";

    public static final String DB = "JobHunter";

    public static final long MAX_PACKET_SIZE = 1024 * 1024 * 16;

    public static final byte[] FILLER = new byte[23];

    public static final byte[] EMPTY_BYTES = new byte[0];
}
