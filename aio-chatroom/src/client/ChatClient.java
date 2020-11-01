package client;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ChatClient {

    private static final String LOCALHOST = "localhost";
    private String host;
    private static final int DEFAULT_PORT = 8888;
    private int port;

    private static final int BUFFER_SIZE = 1024;

    private static final String QUIT = "\\quit";
    private Charset charset = Charset.forName("UTF-8");

    private AsynchronousSocketChannel clientChannel;

    public ChatClient() {
        this(LOCALHOST, DEFAULT_PORT);
    }

    public ChatClient (String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean readyToQuit(String msg) {
        return QUIT.equals(msg);
    }

    public void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        try {
            // 创建客户端channel
            clientChannel = AsynchronousSocketChannel.open();
            // 连接服务端，异步调用
            Future<Void> future = clientChannel.connect(new InetSocketAddress(host, port));
            future.get();

            // 处理用户输入
            new Thread(new UserInputHandler(this)).start();

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while (true) {
                Future<Integer> readResult = clientChannel.read(buffer);
                int result = readResult.get();

                if (result <= 0) {
                    // 说明出现异常，无法再从服务器得到有效信息
                    System.out.println("服务器断开");
                    close(clientChannel);
                    System.exit(1);

                } else {
                    buffer.flip();
                    String msg = String.valueOf(charset.decode(buffer));
                    buffer.clear();
                    System.out.println(msg);
                }
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void send(String msg) {
        if (msg.isEmpty()) {
            return;
        }

        ByteBuffer buffer = charset.encode(msg);
        Future<Integer> writeResult = clientChannel.write(buffer);
        try {
            writeResult.get();
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("发送消息失败");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient();
        client.start();
    }
}
