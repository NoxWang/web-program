package client;

import java.io.*;
import java.net.Socket;

public class ChatClient {
    private final String SERVER_HOST = "127.0.0.1";
    private final int SERVER_PORT = 8888;
    private final String QUIT = "\\quit";

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    // 发送消息给服务器
    public void send(String msg) throws IOException {
        if (!socket.isOutputShutdown()) {
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    // 从服务器接收消息
    public String receive() throws IOException {
        String msg = null;
        if (!socket.isInputShutdown()) {
            msg = reader.readLine();
        }
        return msg;
    }

    // 检查用户是否准备退出
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void start() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);

            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );

            // 处理用户输入
            new Thread(new UserInputHandler(this)).start();

            // 读取服务器转发的信息
            String msg = null;
            while ((msg = reader.readLine()) != null) {
                System.out.println(msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public void close() {
        if (writer != null) {
            try {
                writer.close();
                System.out.println("关闭客户端");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
}
