package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final String LOCALHOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    private int port;

    private static final int BUFFER_SIZE = 1024;
    private static final int THREADPOOL_SIZE = 8;

    private static final String QUIT = "\\quit";
    private Charset charset = Charset.forName("UTF-8");

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverChannel;

    private List<ClientHandler> connectedClients;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
        this.connectedClients = new ArrayList<>();
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 服务端主逻辑
     */
    private void start() {
        try {
            // 创建线程池
            ExecutorService executorService = Executors.newFixedThreadPool(THREADPOOL_SIZE);
            // 创建自定义线程池的ChannelGroup
            channelGroup = AsynchronousChannelGroup.withThreadPool(executorService);
            // 创建自定义ChannelGroup的异步服务端Channel
            serverChannel = AsynchronousServerSocketChannel.open(channelGroup);

            serverChannel.bind(new InetSocketAddress(LOCALHOST, port));
            System.out.println("启动服务器，监听端口: " + port + "...");

            while (true) {
                serverChannel.accept(null, new AcceptHandler());
                System.in.read();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }

    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            // 服务端持续监听客户端请求
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);
            }

            if (clientChannel != null && clientChannel.isOpen()) {
                ClientHandler handler = new ClientHandler(clientChannel);

                // 添加用户至在线列表
                addClient(handler);

                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                clientChannel.read(buffer, buffer, handler);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("连接失败: " + exc);
        }
    }

    private synchronized void addClient(ClientHandler handler) {
        connectedClients.add(handler);
        System.out.println(getClientName(handler.clientChannel) + "已连接");
    }

    private synchronized void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println(getClientName(handler.clientChannel) + "已断开");
        close(handler.clientChannel);
    }

    private class ClientHandler implements CompletionHandler<Integer, Object> {

        AsynchronousSocketChannel clientChannel;

        public ClientHandler (AsynchronousSocketChannel channel) {
            this.clientChannel = channel;
        }
        @Override
        public void completed(Integer result, Object attachment) {
            ByteBuffer buffer = (ByteBuffer) attachment;
            if (buffer != null) {   // 说明此时需要处理的是读
                if (result <= 0) {
                    // 客户端异常，移出在线列表
                    removeClient(this);
                } else {
                    // 获取并打印客户端发送来的信息
                    buffer.flip();
                    String fwdMsg = receive(buffer);

                    // 用户准备退出
                    if (readyToQuit(fwdMsg)) {
                        removeClient(this);
                        return;
                    }

                    System.out.println(getClientName(clientChannel) + ": " + fwdMsg);

                    // 给其他客户端发送消息
                    forwardMessage(clientChannel, fwdMsg);
                    buffer.clear();

                    // 持续监听该客户端channel的输入
                    clientChannel.read(buffer, buffer, this);
                }
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("读写失败：" + exc);
        }
    }

    private synchronized void forwardMessage(AsynchronousSocketChannel clientChannel, String fwdMsg) {
        for (ClientHandler handler : connectedClients) {
            if (!clientChannel.equals(handler.clientChannel)) {
                try {
                    ByteBuffer buffer = charset.encode(getClientName(clientChannel) + ": " + fwdMsg);
                    handler.clientChannel.write(buffer, null, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getClientName(AsynchronousSocketChannel clientChannel) {
        int clientPort = -1;
        try {
            InetSocketAddress address = (InetSocketAddress) clientChannel.getRemoteAddress();
            clientPort = address.getPort();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "客户端[" + clientPort + "]";
    }

    private String receive(ByteBuffer buffer) {
        CharBuffer charBuffer = charset.decode(buffer);
        return String.valueOf(charBuffer);
    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start();
    }
}