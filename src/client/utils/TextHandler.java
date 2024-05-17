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
        System.out.println("Text:" + friend + ":" + message);
        out.println("Text:" + friend + ":" + message);
    }

    public void receiveMessage(String message, ChatFrame chatFrame) {
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            String sender = parts[1];
            String textMessage = parts[2];
            chatFrame.appendMessage(sender + ": " + textMessage + "\n\n");
        } else {
            System.err.println("Invalid text message format: " + message);
        }
    }
}
