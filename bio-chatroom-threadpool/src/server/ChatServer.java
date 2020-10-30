package server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    /** 服务器监听端口 */
    private final int SERVER_PORT = 8888;

    /** 客户端退出命令 */
    private final String QUIT = "\\quit";

    /** 服务端 Socket */
    private ServerSocket serverSocket;

    /** 线程池 */
    private ExecutorService executorService;

    /**
     * 记录当前在线客户端
     * key：客户端端口号
     * value：客户端对应的 Writer
     */
    private HashMap<Integer, Writer> connectedClient;

    public ChatServer(int threadNum) {
        // 创建线程池
        executorService = Executors.newFixedThreadPool(threadNum);
        connectedClient = new HashMap<>();
    }

    /**
     * 添加新在线客户端
     * @param socket 新增客户端的socket
     * @throws IOException
     */
    public synchronized void addClient(Socket socket) throws IOException {
        if (socket != null) {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            connectedClient.put(socket.getPort(), writer);
            System.out.println("客户端[" + socket.getPort() + "]已连接");
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
            if (connectedClient.containsKey(port)) {
                connectedClient.get(port).close();
                System.out.println("客户端[" + port + "]已断开");
            }
            connectedClient.remove(port);
        }
    }

    /**
     * 转发信息给其他所有在线客户端
     * @param socket 发送信息的客户端
     * @param fwdMsg 该客户端发送的信息
     * @throws IOException
     */
    public synchronized void forwardMessage(Socket socket, String fwdMsg) throws IOException {
        if (socket != null) {
            int currentPort = socket.getPort();
            String msg = "客户端[" + currentPort + "]：" + fwdMsg + "\n";
            for (int port : connectedClient.keySet()) {
                if (port != currentPort) {
                    Writer writer = connectedClient.get(port);
                    writer.write(msg);
                    writer.flush();
                }
            }
        }
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    /**
     * 服务端主要逻辑
     */
    public void start() {
        try {
            // 绑定监听端口
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("服务器启动，监听端口" + SERVER_PORT + "...");

            while (true) {
                // 等待客户端连接
                Socket socket = serverSocket.accept();

                // 向线程池提交任务
                executorService.execute(new ChatHandler(this, socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    /**
     * 关闭服务器
     */
    public synchronized void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 创建服务器类时传入允许的最大在线用户数
        ChatServer chatServer = new ChatServer(3);
        chatServer.start();
    }
}
