package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserInputHandler implements Runnable {
    ChatClient client;

    public UserInputHandler(ChatClient client) {
        this.client = client;
    }
    @Override
    public void run() {
        BufferedReader consoleReader = new BufferedReader(
                new InputStreamReader(System.in)
        );
        while (true) {
            try {
                String input = consoleReader.readLine();
                client.send(input);

                if (client.readyToQuit(input)) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
