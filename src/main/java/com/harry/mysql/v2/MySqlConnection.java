package com.harry.mysql.v2;

import com.harry.mysql.util.Buffer;
import com.harry.mysql.util.Constants;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by chenyehui on 16/11/25.
 */
public class MySqlConnection {

    public static void main(String[] args) throws Exception {

        // handshake
        Socket socket = new Socket(Constants.HOST, Constants.PORT);
        Buffer inputBuffer = new Buffer(socket.getInputStream(), socket.getOutputStream());
        inputBuffer.analyzeHeader();
        inputBuffer.analyzeFirstPackage();

        // auth
        inputBuffer.auth(Constants.USERNAME, Constants.PASSWORD, Constants.DB);

        // check auth res
        inputBuffer.analyzeHeader();
        inputBuffer.checkAuth();
    }
}
