import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 定义接口
 */
interface FileCopyRunner {
    void copyFile(File source, File target);
}

public class FileCopyDemo {

    private static final int ROUNDS = 5;

    private static void benchmark(FileCopyRunner test, File source, File target) {
        long elapsed = 0L;
        for (int i = 0; i < ROUNDS; i++) {
            long startTime = System.currentTimeMillis();
            test.copyFile(source, target);
            elapsed += System.currentTimeMillis() - startTime;
            target.delete();
        }
        System.out.println(test + ": " + elapsed / ROUNDS + "ms");
    }

    private static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        /**
         * 使用Stream，不使用缓冲区
         */
        FileCopyRunner noBufferStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    fin = new FileInputStream(source);
                    fout = new FileOutputStream(target);

                    int result;
                    // 读到结尾时，会返回-1
                    while ((result = fin.read()) != -1) {
                        fout.write(result);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "noBufferStreamCopy";
            }
        };

        /**
         * 使用Stream，使用缓冲区
         */
        FileCopyRunner bufferedStreamCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                InputStream fin = null;
                OutputStream fout = null;
                try {
                    // 在FileInputStream上包裹一个BufferedInputStream
                    fin = new BufferedInputStream(new FileInputStream(source));
                    fout = new BufferedOutputStream(new FileOutputStream(target));

                    // 缓冲区
                    byte[] buffer = new byte[1024];

                    // 没读到结尾时，result为这一次读到的数量；读到结尾时，result为-1
                    int result;
                    while ((result = fin.read(buffer)) != -1) {
                        fout.write(buffer, 0, result);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "bufferedStreamCopy";
            }
        };

        /**
         * 使用 Channel，使用 Buffer 进行读写
         */
        FileCopyRunner nioBufferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                // 声明文件通道
                FileChannel fin = null;
                FileChannel fout = null;

                try {
                    // 通过文件输入输出流得到文件通道
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();

                    // 创建ByteBuffer类型的缓冲区（按字节读取）
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    // 将数据从文件通道中读取出来，写进Buffer
                    while (fin.read(buffer) != -1) {
                        // 将Buffer从写模式转换为读模式
                        buffer.flip();
                        while (buffer.hasRemaining()) { // 确保Buffer中的内容被读完
                            // 将Buffer中的数据写入文件通道
                            fout.write(buffer);
                        }
                        // 将Buffer从读模式转换为写模式
                        buffer.clear();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "nioBufferCopy";
            }
        };

        /**
         * 使用 Channel，在两个 Channel 间直接传输数据
         */
        FileCopyRunner nioTransferCopy = new FileCopyRunner() {
            @Override
            public void copyFile(File source, File target) {
                // 声明文件通道
                FileChannel fin = null;
                FileChannel fout = null;

                try {
                    // 通过输入输出流创建通道
                    fin = new FileInputStream(source).getChannel();
                    fout = new FileOutputStream(target).getChannel();

                    long transferred = 0L;
                    long size = fin.size();
                    while (transferred != size) {
                        // 从位置0开始，拷贝fin通道中size长度的数据，至fout通道，返回的是已拷贝长度
                        // transferTo不能保证拷贝通道中的所有数据，因此使用while循环
                        transferred += fin.transferTo(0, size, fout);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    close(fin);
                    close(fout);
                }
            }

            @Override
            public String toString() {
                return "nioTransferCopy";
            }
        };

        File smallFile = new File("E:/JavaProject/web/nio-file-copy/tmp/smallFile.jpg");
        File smallFileCopy = new File("E:/JavaProject/web/nio-file-copy/tmp/smallFile-copy.jpg");

        System.out.println("--- Copying small file ---");
        benchmark(noBufferStreamCopy, smallFile, smallFileCopy);
        benchmark(bufferedStreamCopy, smallFile, smallFileCopy);
        benchmark(nioBufferCopy, smallFile, smallFileCopy);
        benchmark(nioTransferCopy, smallFile, smallFileCopy);

        File bigFile = new File("E:/JavaProject/web/nio-file-copy/tmp/bigFile.pptx");
        File bigFileCopy = new File("E:/JavaProject/web/nio-file-copy/tmp/bigFile-copy.pptx");

        System.out.println("--- Copying big file ---");
//        benchmark(noBufferStreamCopy, bigFile, bigFileCopy);
        benchmark(bufferedStreamCopy, bigFile, bigFileCopy);
        benchmark(nioBufferCopy, bigFile, bigFileCopy);
        benchmark(nioTransferCopy, bigFile, bigFileCopy);

        File hugeFile = new File("E:/JavaProject/web/nio-file-copy/tmp/hugeFile.mp4");
        File hugeFileCopy = new File("E:/JavaProject/web/nio-file-copy/tmp/hugeFile-copy.mp4");

        System.out.println("--- Copying huge file ---");
//        benchmark(noBufferStreamCopy, hugeFile, hugeFileCopy);
        benchmark(bufferedStreamCopy, hugeFile, hugeFileCopy);
        benchmark(nioBufferCopy, hugeFile, hugeFileCopy);
        benchmark(nioTransferCopy, hugeFile, hugeFileCopy);
    }
}
