import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Client {
    final String LOCALHOST = "localhost";
    final int DEFAULT_PORT = 8888;

    AsynchronousSocketChannel clientChannel;

    private void close(Closeable closeable) {
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
            // 创建channel
            clientChannel = AsynchronousSocketChannel.open();

            // 异步调用
            Future<Void> future = clientChannel.connect(new InetSocketAddress(LOCALHOST, DEFAULT_PORT));

            // 该方法为阻塞式，该方法返回则说明之前的异步调用已返回，客户端已连接
            future.get();

            // 等待用户输入
            BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in)
            );
            while (true) {
                String input = consoleReader.readLine();
                byte[] inputBytes = input.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(inputBytes);

                // 异步调用
                Future<Integer> writeResult = clientChannel.write(buffer);
                // get()方法返回则说明内容已写入channel
                writeResult.get();

                buffer.flip();
                // 异步调用
                Future<Integer> readResult = clientChannel.read(buffer);
                // get()方法返回则说明内容已读取并写入buffer
                readResult.get();

                String echo = new String(buffer.array());
                buffer.clear();

                System.out.println(echo);
            }

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            close(clientChannel);
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}
