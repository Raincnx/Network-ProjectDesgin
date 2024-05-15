package server.utils;

import server.ClientHandler;

import java.io.*;
import java.net.Socket;

public class FileHandler extends Handler {
    private Socket socket;

    public FileHandler(PrintWriter out, Socket socket, ClientHandler clientHandler) {
        super(out, clientHandler);
        this.socket = socket;
    }

    @Override
    public void handle(String message) {
        String[] parts = message.split(":", 4);
        if (parts.length >= 4) {
            String targetUsername = parts[1];
            String fileName = parts[2];
            long fileSize = Long.parseLong(parts[3]);
            System.out.println("File from " + clientHandler.getUsername() + " to " + targetUsername + ": " + fileName + " (" + fileSize + " bytes)");
            saveFile(targetUsername, fileName, fileSize);
        } else {
            System.err.println("Invalid file message format: " + message);
        }
    }

    private void saveFile(String targetUsername, String fileName, long fileSize) {
        try {
            File receivedFile = new File(fileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            InputStream inputStream = socket.getInputStream();

            byte[] buffer = new byte[1460];
            int bytesRead;
            long remaining = fileSize;

            while (remaining > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                bos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }

            bos.close();
            fos.close();

            System.out.println("File received: " + fileName + " (" + receivedFile.length() + " bytes)");

            sendFile(targetUsername, fileName, receivedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(String targetUsername, String fileName, File file) {
        try {
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(buffer, 0, buffer.length);

            ClientHandler targetClient = clientHandler.getClients().get(targetUsername);
            if (targetClient != null) {
                targetClient.sendFile(fileName, buffer);
            }

            bis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
