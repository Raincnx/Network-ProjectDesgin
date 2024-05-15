package client.utils;

import client.ChatFrame;

import java.io.PrintWriter;

public class TextHandler {
    private PrintWriter out;
    private String friend;

    public TextHandler(PrintWriter out, String friend) {
        this.out = out;
        this.friend = friend;
    }

    public void sendMessage(String message) {
        out.println("Text:" + friend + ":" + message);
    }

    public void receiveMessage(String message, ChatFrame chatFrame) {
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            String textMessage = parts[2];
            chatFrame.appendMessage("Client: " + textMessage + "\n\n");
        } else {
            System.err.println("Invalid text message format: " + message);
        }
    }
}
