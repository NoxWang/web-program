package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInputHandler implements Runnable {

    private ChatClient chatClient;

    public UserInputHandler (ChatClient client) {
        this.chatClient = client;
    }

    @Override
    public void run() {
        try {
            BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in)
            );
            String msg = null;
            while ((msg = consoleReader.readLine()) != null) {
                chatClient.send(msg);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
