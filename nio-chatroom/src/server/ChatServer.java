package server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ChatServer {

    private static final int DEFAULT_PORT = 8888;
    /** 用户自定义的监听端口 */
    private int port;

    /** 处理服务器端 IO 的通道 */
    private ServerSocketChannel server;
    /** 监听 channel 上发生的事件和 channel 状态的变化 */
    private Selector selector;

    private static final int BUFFER_SIZE = 1024;
    /** 用于从通道读取数据的 Buffer */
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    /** 用于向通道写数据的 Buffer */
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private static final String QUIT = "\\quit";
    /** 指定编解码方式 */
    private Charset charset = StandardCharsets.UTF_8;

    public ChatServer() {
        this(DEFAULT_PORT);
    }

    public ChatServer(int port) {
        this.port = port;
    }

    private void start() {
        try {
            // 创建一个新的通道，并设置为非阻塞式调用（open()方法产生的通道默认为阻塞式调用）
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(port));

            selector = Selector.open();
            // 在selector上注册serverChannel的accept事件
            // 如果serverChannel触发了accept事件，selector会返回该事件相关的properties，封装在SelectionKey中
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("启动服务器，监听端口：" + port + "...");

            while (true) {
                // select()方法为阻塞式调用，如果当前没有selector监听事件出现，则该方法阻塞（返回值为出现事件的数量）
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    // 处理被触发的事件
                    handles(key);
                }
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭selector：解除注册，同时关闭对应的通道
            close(selector);
        }
    }

    /**
     * 需要处理两个事件：ACCEPT & READ
     */
    private void handles(SelectionKey key) throws IOException {
        // ACCEPT事件 --- 和客户端建立了连接
        if (key.isAcceptable()) {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverChannel.accept();
            // 转换为非阻塞式调用
            clientChannel.configureBlocking(false);

            // 注册该客户端channel的READ事件
            clientChannel.register(selector, SelectionKey.OP_READ);
            System.out.println(getClientName(clientChannel) + "已连接");
        }

        // READ事件 --- 客户端发送了消息
        else if (key.isReadable()) {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            String fwdMsg = receive(clientChannel);
            if (fwdMsg.isEmpty() || readyToQuit(fwdMsg)) { // 客户端异常 or 客户端准备退出
                // 取消注册该通道上的该事件
                key.cancel();
                // 更改状态后，强制返回selector，令其重新检测
                selector.wakeup();
                System.out.println(getClientName(clientChannel) + "已断开");
            } else {
                System.out.println(getClientName(clientChannel) + ":" + fwdMsg);
                forwardMessage(clientChannel, fwdMsg);
            }
        }
    }

    private void forwardMessage(SocketChannel clientChannel, String fwdMsg) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel connectedClient = (SocketChannel) key.channel();
                if (!connectedClient.equals(clientChannel)) {
                    wBuffer.clear();
                    wBuffer.put(charset.encode(getClientName(clientChannel) + ":" + fwdMsg));
                    // 将wBuffer从写入模式转换为读取模式
                    wBuffer.flip();
                    while (wBuffer.hasRemaining()) {
                        connectedClient.write(wBuffer);
                    }
                }
            }
        }
    }

    private String receive(SocketChannel clientChannel) throws IOException {
        rBuffer.clear();
        while ((clientChannel.read(rBuffer)) > 0);
        // 将rBuffer从写模式转换为读模式
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    private String getClientName(SocketChannel client) {
        return "客户端[" + client.socket().getPort() + "]";
    }

    private boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    private void close (Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
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