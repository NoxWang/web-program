package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {

    /** 服务器监听端口 */
    private int SERVER_PORT = 8888;

    /** 客户端退出标志（客户端发送\quit表示退出聊天室 */
    private final String QUIT = "\\quit";

    /** 服务端 Socket */
    private ServerSocket serverSocket;

    /**
     * 存储已连接的客户端
     * key：客户端的端口号
     * value：向该端口发信息所使用的 Writer
     */
    private Map<Integer, Writer> connectedClients;

    public ChatServer() {
        connectedClients = new HashMap<>();
    }

    /**
     * 添加新在线客户端
     * @param socket 新增客户端的socket
     * @throws IOException
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            connectedClients.put(port, writer);
            System.out.println("客户端[" + port + "]已连接到服务器");
        }
    }

    /**
     * 移除已下线客户端
     * @param socket 已下线的客户端socket
     * @throws IOException
     */
    public synchronized void removeClient(Socket socket) throws IOException {
        if (socket != null) {
            int port = socket.getPort();
            if (connectedClients.containsKey(port)) {
                connectedClients.get(port).close();
            }
            connectedClients.remove(port);
            System.out.println("客户端[" + port + "]已断开连接");
        }
    }

    /**
     * 转发信息给其他所有在线客户端
     * @param socket 发送信息的客户端
     * @param fwdMsg 该客户端发送的信息
     * @throws IOException
     */
    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        for (Integer id : connectedClients.keySet()) {
            if (!id.equals(socket.getPort())) {
                Writer writer = connectedClients.get(id);
                writer.write(fwdMsg);
                writer.flush();
            }
        }
    }

    /**
     * 服务端主要逻辑
     */
    public void start() {
        try {
            // 为服务端绑定端口
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("服务器启动，监听端口：" + SERVER_PORT + "...");

            while (true) {
                // accept()方法是阻塞式的
                Socket socket = serverSocket.accept();

                // 有客户端连接后，为它创建一个ChatHandler线程
                new Thread(new ChatHandler(this, socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    /**
     * 判断客户端是否准备退出
     * @param msg 客户端发送的消息
     * @return true：准备退出
     */
    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 关闭服务器
     */
    public synchronized void close() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                System.out.println("服务器关闭");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}
