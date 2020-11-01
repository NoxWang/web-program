import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * 实现简单的回音壁功能：收到客户端的消息后，再将该消息返回给客户端
 */
public class Server {

    final String LOCALHOST = "localhost";
    /** 服务器监听端口 */
    final int DEFAULT_PORT = 8888;

    /** 异步的服务器 Channel */
    AsynchronousServerSocketChannel serverChannel;

    private void close (Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                System.out.println("关闭" + closeable);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 服务器主逻辑
     */
    public void start() {
        // 绑定监听端口
        try {
            // 创建异步的服务器Channel
            serverChannel = AsynchronousServerSocketChannel.open();
            // 绑定监听端口
            serverChannel.bind(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));
            System.out.println("启动服务器，监听端口: " + DEFAULT_PORT + "...");

            while (true) {
                /*
                 * 第一个参数可有可无，根据业务需求而定（相当于附件）
                 * 第二个参数为一个CompletionHandler
                 * 该调用为异步调用，没有结果的话会直接返回
                 * 直到有客户端连接，AcceptHandler中定义的回调函数才会被系统调用
                 */
                serverChannel.accept(null, new AcceptHandler());

                // 避免主线程返回 & 避免过于频繁地调用accept()
                System.in.read();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close(serverChannel);
        }
    }

    // CompletionHandler的泛型：第一个为异步调用返回的结果的类型，第二个为attachment的类型
    // 处理accept()异步调用
    private class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, Object> {
        /**
         * 异步调用正常返回后会调用用的回调函数
         * 即有客户端连接进来后我们需要进行的操作
         * @param result accept()函数返回的结果，即客户端的 AsynchronousSocketChannel
         * @param attachment 传入的attachment
         */
        @Override
        public void completed(AsynchronousSocketChannel result, Object attachment) {
            // 等待下一个客户端发起连接请求
            if (serverChannel.isOpen()) {
                serverChannel.accept(null, this);
            }

            AsynchronousSocketChannel clientChannel = result;
            if (clientChannel != null && clientChannel.isOpen()) {
                // 处理客户端通道的读写
                ClientHandler handler = new ClientHandler(clientChannel);

                ByteBuffer buffer = ByteBuffer.allocate(1024);

                // 定义一个 Map 作为 attachment
                Map<String, Object> info = new HashMap<>();
                info.put("type", "read");
                info.put("buffer", buffer);

                // 异步调用
                clientChannel.read(buffer, info, handler);
            }
        }

        // 异步调用操作出现错误时会调用的回调函数
        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

    // 处理客户端channel的read、write异步调用
    private class ClientHandler implements CompletionHandler<Integer, Object> {

        private AsynchronousSocketChannel clientChannel;

        public ClientHandler(AsynchronousSocketChannel channel) {
            this.clientChannel = channel;
        }

        @Override
        public void completed(Integer result, Object attachment) {
            Map<String, Object> info = (Map<String, Object>) attachment;
            String type = (String) info.get("type");

            if ("read".equals(type)) {
                ByteBuffer buffer = (ByteBuffer) info.get("buffer");
                buffer.flip();
                info.put("type", "write");

                // 异步调用write
                clientChannel.write(buffer, info, this);
                buffer.clear();

            } else if ("write".equals(type)) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                info = new HashMap<>();
                info.put("type", "read");
                info.put("buffer", buffer);
                clientChannel.read(buffer, info, this);
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {

        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
}
