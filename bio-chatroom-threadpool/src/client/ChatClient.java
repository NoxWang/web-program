package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ChatClient {

    /** 服务器 IP */
    private final String SERVER_HOST = "127.0.0.1";

    /** 服务器监听端口号 */
    private final int SERVER_PORT = 8888;

    /** 客户端退出命令 */
    private final String QUIT = "\\quit";

    /** 客户端 Socket */
    private Socket socket;

    public boolean readerToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void start() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);

            // 另起一个线程处理用户输入
            new Thread(new UserInputHandler(this, socket)).start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                System.out.println(msg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
