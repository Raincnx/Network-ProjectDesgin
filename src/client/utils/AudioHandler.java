package client.utils;

import client.ChatFrame;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;

public class AudioHandler {
    private PrintWriter out;
    private Socket socket;
    private int audiocount = 0;
    private boolean isRecording = false;

    public AudioHandler(PrintWriter out, Socket socket) {
        this.out = out;
        this.socket = socket;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void startRecording() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format);
            line.start();
            audiocount++;
            OutputStream outputStream = socket.getOutputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            isRecording = true;

            Thread recordingThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[1460];
                    int bytesRead;
                    while (isRecording && (bytesRead = line.read(buffer, 0, buffer.length)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    out.println("Audio:" + audiocount + ":" + os.size());
                    out.flush();
                    outputStream.write(os.toByteArray());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    line.stop();
                    line.close();
                }
            });
            recordingThread.start();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void receiveAudio(String message, ChatFrame chatFrame) {
        String[] parts = message.split(":", 3);
        if (parts.length >= 3) {
            String audioname = parts[1];
            long fileSize = Long.parseLong(parts[2]);
            chatFrame.appendMessage("You received a Voice Chat...\n\n");
            playAudio(fileSize);
        } else {
            System.err.println("Invalid audio message format: " + message);
        }
    }

    private void playAudio(long fileSize) {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1460];
            int bytesRead;
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

            line.open(format);
            line.start();
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                line.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
                if (fileSize <= 0) break;
            }
            line.drain();
            line.close();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }
}
