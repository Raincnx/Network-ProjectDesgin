package server.utils;

import server.ClientHandler;

import java.io.PrintWriter;

public class TextHandler extends Handler {

    public TextHandler(PrintWriter out, ClientHandler clientHandler) {
        super(out, clientHandler);
    }

    @Override
    public void handle(String message) {
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            String targetUsername = parts[1];
            String textMessage = parts[2];
            System.out.println("Message from " + clientHandler.getUsername() + " to " + targetUsername + ": " + textMessage);
            sendMessage(targetUsername, textMessage);
        } else {
            System.err.println("Invalid text message format: " + message);
        }
    }

    private void sendMessage(String targetUsername, String message) {
        ClientHandler targetClient = clientHandler.getClients().get(targetUsername);
        if (targetClient != null) {
            targetClient.sendMessage("Text:" + clientHandler.getUsername() + ":" + message);
        } else {
            System.out.println("client " + targetUsername + " not found.");
        }
    }
}
