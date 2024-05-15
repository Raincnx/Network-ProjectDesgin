package server.utils;

import server.ClientHandler;

import java.io.*;
import java.net.Socket;

public class AudioHandler extends Handler {
    private Socket socket;

    public AudioHandler(PrintWriter out, Socket socket, ClientHandler clientHandler) {
        super(out, clientHandler);
        this.socket = socket;
    }

    @Override
    public void handle(String message) {
        String[] parts = message.split(":", 4);
        if (parts.length >= 4) {
            String targetUsername = parts[1];
            String audioname = parts[2];
            long fileSize = Long.parseLong(parts[3]);
            System.out.println("Audio from " + clientHandler.getUsername() + " to " + targetUsername + ": " + audioname + " (" + fileSize + " bytes)");
            saveAudio(targetUsername, audioname, fileSize);
        } else {
            System.err.println("Invalid audio message format: " + message);
        }
    }

    private void saveAudio(String targetUsername, String audioname, long fileSize) {
        try {
            ByteArrayOutputStream audioStream = new ByteArrayOutputStream();
            InputStream inputStream = socket.getInputStream();

            byte[] buffer = new byte[1460];
            int bytesRead;
            long remaining = fileSize;

            while (remaining > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                audioStream.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }

            System.out.println("Audio received: " + audioname + " (" + audioStream.size() + " bytes)");

            sendAudio(targetUsername, audioname, audioStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendAudio(String targetUsername, String audioname, ByteArrayOutputStream audioStream) {
        byte[] audioData = audioStream.toByteArray();
        ClientHandler targetClient = clientHandler.getClients().get(targetUsername);
        if (targetClient != null) {
            targetClient.sendAudio(audioname, audioData);
        }
    }
}
