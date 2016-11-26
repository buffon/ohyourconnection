package com.harry.mysql.v2;

import com.harry.mysql.util.StreamBuffer;
import com.harry.mysql.util.Constants;

import java.net.Socket;

/**
 * Created by chenyehui on 16/11/25.
 */
public class MySqlConnection {

    public static void main(String[] args) throws Exception {

        // handshake
        Socket socket = new Socket(Constants.HOST, Constants.PORT);
        StreamBuffer inputStreamBuffer = new StreamBuffer(socket.getInputStream(), socket.getOutputStream());
        inputStreamBuffer.analyzeHeader();
        inputStreamBuffer.analyzeFirstPackage();

        // auth
        inputStreamBuffer.auth(Constants.USERNAME, Constants.PASSWORD, Constants.DB);

        // check auth res
        inputStreamBuffer.analyzeHeader();
        inputStreamBuffer.checkAuth();
    }
}
