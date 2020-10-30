package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ChatHandler implements Runnable{

    /** 服务器类 */
    private ChatServer server;

    /** 当前客户端 Socket */
    private Socket socket;

    public ChatHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 存储新上线用户
            server.addClient(socket);

            // 读取用户发送的消息
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            String msg = null;
            while ((msg = reader.readLine()) != null) {
                // 检查用户是否退出
                if (server.readyToQuit(msg)) {
                    break;
                }

                String fwdMsg = "客户端[" + socket.getPort() + "]：" + msg + "\n";
                System.out.print(fwdMsg);

                // 转发消息至其他在线用户
                server.forwardMessage(socket, fwdMsg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.removeClient(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
