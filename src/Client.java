import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private JFrame frame;
    private JTextArea messageArea;
    private JTextField inputField;
    private JFrame loginFrame;
    private JFrame friendFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private JList<String> friendList;
    private DefaultListModel<String> friendListModel;
    private String selectedFriend;
    private int audiocount;
    private boolean isLoading;
    private boolean isBegin;
    private boolean isRecording;
    private JButton uploadButton;

    private Map<String, File> receivedFiles = new HashMap<>();

    public Client(String ip, int port) throws IOException {
        connectToServer(ip, port);
    }

    private void createLoginGUI() {
        loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 150);
        loginFrame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        JLabel usernameLabel = new JLabel("             Username:");
        usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("             Password:");
        passwordField = new JPasswordField();

        inputPanel.add(usernameLabel);
        inputPanel.add(usernameField);
        inputPanel.add(passwordLabel);
        inputPanel.add(passwordField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                out.println(username);
                out.println(password);

                try {
                    String response = in.readLine();
                    System.out.println("Server response: " + response);
                    if (response.equals("Authentication successful")) {
                        loginFrame.dispose();
                        createFriendGUI();
                    } else {
                        statusLabel.setText("用户名或密码错误!");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        buttonPanel.add(loginButton);

        statusLabel = new JLabel("", JLabel.CENTER);

        loginFrame.add(inputPanel, BorderLayout.CENTER);
        loginFrame.add(buttonPanel, BorderLayout.SOUTH);
        loginFrame.add(statusLabel, BorderLayout.NORTH);

        loginFrame.setLocationRelativeTo(null); // 将窗口居中显示
    }

    private void connectToServer(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        System.out.println("Connected to server: " + socket);

        createLoginGUI();
        loginFrame.setVisible(true);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // 开启一个单独的线程读取服务器消息并更新图形界面
        Thread readerThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Clients:")) {
                        updateFriendList(message.substring(8));
                    } else {
                        receiveMessage(message, null);  // 将 fileList 参数设置为 null，因为此时尚未创建 UI
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readerThread.start();
    }

    private void createFriendGUI() {
        friendFrame = new JFrame("User: " + usernameField.getText());
        friendFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        friendFrame.setSize(300, 400);
        friendFrame.setLayout(new BorderLayout());

        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedFriend = friendList.getSelectedValue();
            }
        });

        JScrollPane scrollPane = new JScrollPane(friendList);
        friendFrame.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton chatButton = new JButton("Chat");
        chatButton.addActionListener(e -> {
            if (selectedFriend != null) {
                friendFrame.dispose();
                createAndShowGUI();
            } else {
                JOptionPane.showMessageDialog(friendFrame, "Please select a friend to chat with.");
            }
        });
        buttonPanel.add(chatButton);

        friendFrame.add(buttonPanel, BorderLayout.SOUTH);

        friendFrame.setLocationRelativeTo(null); // 将窗口居中显示
        friendFrame.setVisible(true);
    }

    private void createAndShowGUI() {
        frame = new JFrame("Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400); // 增加窗口大小以容纳文件列表
        audiocount = 0;
        isLoading = false;
        isBegin = true;
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // 将消息显示区域放置在滚动窗格中
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        JPanel inputPanel = new JPanel(new BorderLayout());

        // 创建语音输入按钮
        JButton voiceButton = new JButton("Voice");
        voiceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!isRecording) {
                    // 开始录制语音
                    startRecording();
                    voiceButton.setText("Stop");
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append("You start a Voice Chat...\n\n");
                    });
                } else {
                    // 结束录制语音并发送
                    stopRecordingAndSend();
                    voiceButton.setText("Voice");
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append("Voice Chat is over...\n\n");
                    });
                }
            }
        });
        inputPanel.add(voiceButton, BorderLayout.WEST);

        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                sendMessage(message);
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("You: " + message + "\n\n");
                });
                inputField.setText("");
            }
        });
        inputPanel.add(inputField, BorderLayout.CENTER);

        // 创建上传文件按钮
        uploadButton = new JButton("Upload File");
        uploadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (isBegin) {
                    JFileChooser fileChooser = new JFileChooser();
                    int result = fileChooser.showOpenDialog(frame);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        // 处理上传文件的逻辑
                        uploadButton.setText("Stop");
                        isLoading = true;
                        isBegin = false;
                        new Thread(() -> {
                            sendFile(selectedFile);
                        }).start();
                    }
                } else {
                    if (isLoading) {
                        uploadButton.setText("Continue");
                        isLoading = false;
                    } else {
                        uploadButton.setText("Stop");
                        isLoading = true;
                    }
                }
            }
        });

        inputPanel.add(uploadButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // 创建文件列表
        JPanel filePanel = new JPanel(new BorderLayout());
        JLabel fileLabel = new JLabel("Received Files:");
        JList<String> fileList = new JList<>(new DefaultListModel<>());
        fileList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // 双击下载文件
                    String fileName = fileList.getSelectedValue();
                    if (fileName != null) {
                        downloadFile(fileName);
                    }
                }
            }
        });

        filePanel.add(fileLabel, BorderLayout.NORTH);
        filePanel.add(new JScrollPane(fileList), BorderLayout.CENTER);
        frame.add(filePanel, BorderLayout.EAST);

        frame.setVisible(true);

        // 开启一个单独的线程读取服务器消息并更新图形界面
        Thread readerThread = new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    receiveMessage(message, fileList);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        readerThread.start();
    }

    public void sendMessage(String message) {
        if (selectedFriend != null) {
            out.println("Text:" + selectedFriend + ":" + message);
        } else {
            JOptionPane.showMessageDialog(frame, "Please select a friend to chat with.");
        }
    }

    private void startRecording() {
        try {
            // 创建音频格式
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

            // 获取音频输入设备
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format);
            line.start();
            audiocount += 1;
            System.out.println("Audio:" + audiocount);
            // 创建输出流，用于发送音频数据
            OutputStream outputStream = socket.getOutputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            isRecording = true;
            // 创建线程用于录制音频并发送
            Thread recordingThread = new Thread(() -> {
                try {
                    System.out.println("Start recording...");
                    byte[] buffer = new byte[1460];
                    int bytesRead;
                    while (isRecording && (bytesRead = line.read(buffer, 0, buffer.length)) != -1) {
                        // 将音频数据发送到服务器
                        os.write(buffer, 0, bytesRead);
                    }
                    out.println("Audio:" + audiocount + ":" + os.size());
                    out.flush();

                    outputStream.write(os.toByteArray());
                    outputStream.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // 关闭音频输入设备和输出流
                    line.stop();
                    line.close();
                }
            });
            recordingThread.start();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingAndSend() {
        System.out.println("Stop recording and send...");
        isRecording = false;
    }

    private void receiveMessage(String message, JList<String> fileList) {
        try {
            System.out.println(message);
            if (message.startsWith("Text:")) {
                // 接收到文本消息
                String textMessage = message.substring(5);
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("Client: " + textMessage + "\n\n");
                });
            } else if (message.startsWith("File:")) {
                // 接收到文件消息
                String[] parts = message.split(":");
                String fileName = parts[1];
                long fileSize = Long.parseLong(parts[2]);
                receiveFile(fileName, fileSize, fileList);
            } else if (message.startsWith("Audio:")) {
                String[] parts = message.split(":");
                String audioname = parts[1];
                long fileSize = Long.parseLong(parts[2]);
                SwingUtilities.invokeLater(() -> {
                    messageArea.append("You received a Voice Chat...\n\n");
                });
                receiveAudio(audioname, fileSize);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveAudio(String audioname, long filesize) {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1460];
            int bytesRead;
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

            // 获取音频输出设备
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);

            // 打开音频输出设备并开始播放
            line.open(format);
            line.start();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                line.write(buffer, 0, bytesRead);
                filesize -= bytesRead;
                if (filesize <= 0) break;
            }
            line.drain();
            line.close();
            SwingUtilities.invokeLater(() -> {
                messageArea.append("Voice is played over...\n\n");
            });
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFile(File file) {
        try {
            // 读取文件内容并发送
            byte[] buffer = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(buffer, 0, buffer.length);

            if (selectedFriend != null) {
                // 发送文件名
                out.println("File:" + selectedFriend + ":" + file.getName() + ":" + file.length());
                out.flush();

                int p = 0;
                // 发送文件内容
                OutputStream os = socket.getOutputStream();
                int bytesSent = 0;
                while (bytesSent < buffer.length) {
                    if (isLoading) {
                        int remaining = buffer.length - bytesSent;
                        int bytesToSend = Math.min(remaining, 1460);
                        os.write(buffer, bytesSent, bytesToSend);
                        os.flush();
                        bytesSent += bytesToSend;

                        // 更新图形界面显示
                        final int progress = (int) ((double) bytesSent / buffer.length * 100);
                        if (progress == p) {
                            p += 5;
                            SwingUtilities.invokeLater(() -> {
                                messageArea.append("Sending: " + progress + "%\n");
                            });
                        }
                    } else {
                        // 如果暂停发送，等待一段时间再继续
                        Thread.sleep(1000);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a friend to send the file to.");
            }

            // 关闭流
            fis.close();
            bis.close();

            isBegin = true;
            isLoading = false;
            uploadButton.setText("Upload File");
            // 在图形界面中显示文件传输信息
            SwingUtilities.invokeLater(() -> {
                messageArea.append("File sent: " + file.getName() + "---" + file.length() + "\n\n");
            });
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void receiveFile(String fileName, long filesize, JList<String> fileList) {
        try {
            // 创建文件输出流
            File receivedFile = new File(fileName);
            FileOutputStream fos = new FileOutputStream(receivedFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            System.out.println("I want to receive File");

            // 从输入流中读取文件内容
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1460];
            int bytesRead;

            while (filesize > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, filesize))) != -1) {
                bos.write(buffer, 0, bytesRead);
                bos.flush();
                filesize -= bytesRead;
            }
            bos.flush();
            // 关闭流
            bos.close();
            fos.close();

            // 在图形界面中显示文件接收信息
            SwingUtilities.invokeLater(() -> {
                messageArea.append("File received: " + fileName + "\n");
                messageArea.append("File saved to: " + receivedFile.getAbsolutePath() + "\n\n");

                // 将文件添加到文件列表中
                DefaultListModel<String> model = (DefaultListModel<String>) fileList.getModel();
                model.addElement(fileName);
                receivedFiles.put(fileName, receivedFile);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadFile(String fileName) {
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

    private void updateFriendList(String clients) {
        SwingUtilities.invokeLater(() -> {
            friendListModel.clear();
            Arrays.stream(clients.split(",")).forEach(friendListModel::addElement);
        });
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        socket.close();
    }

}
