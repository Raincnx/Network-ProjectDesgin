package client.utils;

import client.ChatFrame;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.Map;

public class FileHandler {
    private PrintWriter out;
    private Socket socket;
    private Map<String, File> receivedFiles;
    private boolean isLoading = false;
    private boolean isBegin = true;

    public FileHandler(PrintWriter out, Socket socket, Map<String, File> receivedFiles) {
        this.out = out;
        this.socket = socket;
        this.receivedFiles = receivedFiles;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean isLoading) {
        this.isLoading = isLoading;
    }

    public boolean isBegin() {
        return isBegin;
    }

    public void setBegin(boolean isBegin) {
        this.isBegin = isBegin;
    }

    public void sendFile(File file) {
        try {
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(buffer, 0, buffer.length);
            out.println("File:" + file.getName() + ":" + file.length());
            out.flush();
            OutputStream os = socket.getOutputStream();
            int bytesSent = 0;
            while (bytesSent < buffer.length) {
                if (isLoading) {
                    int remaining = buffer.length - bytesSent;
                    int bytesToSend = Math.min(remaining, 1460);
                    os.write(buffer, bytesSent, bytesToSend);
                    os.flush();
                    bytesSent += bytesToSend;
                } else {
                    Thread.sleep(1000);
                }
            }
            fis.close();
            bis.close();
            isBegin = true;
            isLoading = false;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void receiveFile(String message, ChatFrame chatFrame) {
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            String fileName = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            try {
                File receivedFile = new File(fileName);
                FileOutputStream fos = new FileOutputStream(receivedFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1460];
                int bytesRead;
                while (fileSize > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    bos.flush();
                    fileSize -= bytesRead;
                }
                bos.flush();
                bos.close();
                fos.close();
                receivedFiles.put(fileName, receivedFile);
                chatFrame.appendMessage("File received: " + fileName + "\n\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Invalid file message format: " + message);
        }
    }

    public void downloadFile(String fileName, JFrame frame) {
        File file = receivedFiles.get(fileName);
        if (file != null) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(file.getName()));
            int result = fileChooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File saveFile = fileChooser.getSelectedFile();
                try (InputStream in = new FileInputStream(file);
                     OutputStream out = new FileOutputStream(saveFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    JOptionPane.showMessageDialog(frame, "File downloaded to: " + saveFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Error downloading file: " + e.getMessage());
                }
            }
        }
    }
}
