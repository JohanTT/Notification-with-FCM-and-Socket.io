package com.example.notificationdemo.Socket;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketHandler {
    private Socket mSocket;
    public SocketHandler() {
        try {
        // "http://10.0.2.2:3000" là mạng của máu ảo android dùng để kết nối với mạng localhost của máy tính
        // Việc chúng ta dùng "http://localhost:300/" sẽ không hoạt động được
            mSocket = IO.socket("http://10.0.2.2:3000");
        } catch (URISyntaxException e) {
            System.out.println("Connecting to Socket.io Server failed");
        }
    }

    public Socket getSocket() {
        return this.mSocket;
    }
}
