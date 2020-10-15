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

    private String host;
    private static final String DEFAULT_SERVER_HOST = "127.0.0.1";

    private int port;
    private static final int DEFAULT_SERVER_PORT = 8888;

    private SocketChannel client;
    private Selector selector;

    private static final int BUFFER_SIZE = 1024;
    private ByteBuffer rBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private ByteBuffer wBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private Charset charset = StandardCharsets.UTF_8;
    private static final String QUIT = "\\quit";

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

    private void start() {
        try {
            client = SocketChannel.open();
            client.configureBlocking(false);

            selector = Selector.open();
            // 注册 连接就绪 事件
            client.register(selector, SelectionKey.OP_CONNECT);
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
            if (clientChannel.isConnectionPending()) {
                clientChannel.finishConnect();
                new Thread(new UserInputHandler(this)).start();
            }
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
