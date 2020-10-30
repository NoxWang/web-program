package client;

import java.io.*;
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

    /** 从服务端读取信息的 Reader */
    private BufferedReader reader;

    /** 向服务端发送消息的 Writer */
    private BufferedWriter writer;

    /**
     * 发送消息给服务器
     * @param msg
     * @throws IOException
     */
    public void send(String msg) throws IOException {
        // 确定输出流没有被关闭
        if (!socket.isOutputShutdown()) {
            writer.write(msg + "\n");
            writer.flush();
        }
    }

    /**
     * 检查用户是否准备退出
     */
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 关闭服务器
     */
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

    public void start() {
        try {
            // 创建socket
            socket = new Socket(SERVER_HOST, SERVER_PORT);

            // 创建IO流
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

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
}
