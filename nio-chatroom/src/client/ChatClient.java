package client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class ChatClient {

    /** 服务器地址 */
    private String host;
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";

    /** 服务器端口 */
    private int port;
    private static final int DEFAULT_SERVER_PORT = 8888;

    /** 客户端 Channel */
    private SocketChannel client;
    /** 监听Channel的Selector */
    private Selector selector;

    /** 缓冲区大小 */
    private static final int BUFFER_SIZE = 1024;
    /** 用于从通道读取数据的 Buffer */
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    /** 用于向通道写数据的 Buffer */
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    /** 客户端退出命令 */
    private static final String QUIT = "\\quit";
    /** 指定编解码方式 */
    private Charset charset = StandardCharsets.UTF_8;

    public ChatClient() {
        this(DEFAULT_SERVER_HOST, DEFAULT_SERVER_PORT);
    }

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean readyToQuit(String msg) {
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
     * 客户端主要逻辑
     */
    private void start() {
        try {
            // 创建Channel，并设置为非阻塞式调用
            client = SocketChannel.open();
            client.configureBlocking(false);

            // 创建Selector
            selector = Selector.open();
            // 注册 连接就绪CONNECT 事件
            client.register(selector, SelectionKey.OP_CONNECT);
            // 向服务器发送连接请求
            client.connect(new InetSocketAddress(host, port));

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                for (SelectionKey key : selectionKeys) {
                    handles(key);
                }
                selectionKeys.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClosedSelectorException e) {
            // 用户正常退出
        } finally {
            close(selector);
        }
    }

    /**
     * 处理 CONNECT (连接就绪)和 READ （服务器转发消息）事件
     */
    private void handles(SelectionKey key) throws IOException {
        if (key.isConnectable()) {  // 处理 CONNECT
            SocketChannel clientChannel = (SocketChannel) key.channel();
            if (clientChannel.isConnectionPending()) {  // 返回true：连接已就绪
                // 结束连接状态，完成连接
                clientChannel.finishConnect();
                new Thread(new UserInputHandler(this)).start();
            }
            // 注册READ事件，以接收服务端转发的消息
            clientChannel.register(selector, SelectionKey.OP_READ);

        } else if (key.isReadable()) {  // 处理READ
            SocketChannel clientChannel = (SocketChannel) key.channel();
            String msg = receive(clientChannel);
            if (msg.isEmpty()) {
                // 服务器异常
                close(selector);
            } else {
                System.out.println(msg);
            }
        }
    }

    /**
     * 向服务端发送信息
     * @param msg 用户输入的信息
     * @throws IOException
     */
    public void send(String msg) throws IOException {
        if (msg.isEmpty()) {
            return;
        }

        wBuffer.clear();
        wBuffer.put(charset.encode(msg));
        wBuffer.flip();
        while (wBuffer.hasRemaining()) {
            client.write(wBuffer);
        }

        if (readyToQuit(msg)) {
            close(selector);
        }
    }

    /**
     * 读取服务端转发来的消息
     * @param clientChannel 客户端channel
     * @return 收到的消息
     * @throws IOException
     */
    private String receive(SocketChannel clientChannel) throws IOException {
        rBuffer.clear();
        while (clientChannel.read(rBuffer) > 0);
        rBuffer.flip();
        return String.valueOf(charset.decode(rBuffer));
    }

    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
        chatClient.start();
    }
}
