package client;

import java.io.*;
import java.net.Socket;

public class UserInputHandler implements Runnable{
    private ChatClient chatClient;
    private Socket socket;

    public UserInputHandler(ChatClient chatClient, Socket socket) {
        this.chatClient = chatClient;
        this.socket = socket;
    }

    @Override
    public void run() {
        BufferedWriter writer = null;
        BufferedReader consoleReader = new BufferedReader(
                new InputStreamReader(System.in)
        );
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream())
            );
            String msg = null;
            while ((msg = consoleReader.readLine()) != null) {
                writer.write(msg + "\n");
                writer.flush();

                if (chatClient.readerToQuit(msg)) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
