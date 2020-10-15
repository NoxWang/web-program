package server;

import java.io.*;
import java.net.Socket;

public class ChatHandler implements Runnable {

    /** 服务器类 */
    private ChatServer chatServer;

    /** 当前客户端 Socket */
    private Socket socket;

    public ChatHandler(ChatServer chatServer, Socket socket) {
        this.chatServer = chatServer;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            chatServer.addClient(socket);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            String msg = null;
            while ((msg = reader.readLine()) != null) {
                if (chatServer.readyToQuit(msg)) {
                    break;
                }
                System.out.println("客户端[" + socket.getPort() + "]：" + msg);
                chatServer.forwardMessage(socket, msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                chatServer.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
