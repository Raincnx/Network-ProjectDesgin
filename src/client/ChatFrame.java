package client;

import client.utils.AudioHandler;
import client.utils.FileHandler;
import client.utils.TextHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ChatFrame {
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton uploadButton;
    private JButton voiceButton;
    private TextHandler textHandler;
    private AudioHandler audioHandler;
    private FileHandler fileHandler;

    public ChatFrame(String friend, PrintWriter out, Socket socket, Map<String, File> receivedFiles) {
        textHandler = new TextHandler(out, friend);
        audioHandler = new AudioHandler(out, socket);
        fileHandler = new FileHandler(out, socket, receivedFiles);

        frame = new JFrame("Chat with " + friend);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(600, 400);
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());

        voiceButton = new JButton("Voice");
        voiceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!audioHandler.isRecording()) {
                    audioHandler.startRecording();
                    voiceButton.setText("Stop");
                    appendMessage("You start a Voice Chat...\n\n");
                } else {
                    audioHandler.stopRecording();
                    voiceButton.setText("Voice");
                    appendMessage("Voice Chat is over...\n\n");
                }
            }
        });
        inputPanel.add(voiceButton, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                textHandler.sendMessage(message);
                appendMessage("You: " + message + "\n\n");
                inputField.setText("");
            }
        });
        inputPanel.add(inputField, BorderLayout.CENTER);

        uploadButton = new JButton("Upload File");
        uploadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (fileHandler.isBegin()) {
                    JFileChooser fileChooser = new JFileChooser();
                    int result = fileChooser.showOpenDialog(frame);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        uploadButton.setText("Stop");
                        fileHandler.setLoading(true);
                        fileHandler.setBegin(false);
                        new Thread(() -> {
                            fileHandler.sendFile(selectedFile, friend);
                        }).start();
                    }
                } else {
                    if (fileHandler.isLoading()) {
                        uploadButton.setText("Continue");
                        fileHandler.setLoading(false);
                    } else {
                        uploadButton.setText("Stop");
                        fileHandler.setLoading(true);
                    }
                }
            }
        });
        inputPanel.add(uploadButton, BorderLayout.EAST);

        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.setVisible(true);

        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Text:")) {
                        textHandler.receiveMessage(message, this);
                    } else if (message.startsWith("File:")) {
                        fileHandler.receiveFile(message, this);
                    } else if (message.startsWith("Audio:")) {
                        audioHandler.receiveAudio(message, this);
                    } else {
                        System.err.println("Unknown message format: " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public JFrame getFrame() {
        return frame;
    }

    public void appendMessage(String message) {
        SwingUtilities.invokeLater(() -> messageArea.append(message));
    }
}
