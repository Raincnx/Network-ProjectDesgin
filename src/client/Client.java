package client;

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
    private JFrame friendFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JLabel statusLabel;
    private JList<String> friendList;
    private DefaultListModel<String> friendListModel;
    private String selectedFriend;
    private Map<String, ChatFrame> chatFrames = new HashMap<>();
    private Map<String, File> receivedFiles = new HashMap<>();

    public Client(String ip, int port) throws IOException {
        connectToServer(ip, port);
    }

    private void createLoginGUI() {
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(300, 150);
        loginFrame.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        JLabel usernameLabel = new JLabel("Username:");
        usernameField = new JTextField();
        JLabel passwordLabel = new JLabel("Password:");
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
                    if (response.equals("Authentication successful")) {
                        loginFrame.dispose();
                        createFriendGUI();
                    } else {
                        statusLabel.setText("Invalid username or password!");
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
        loginFrame.setVisible(true);
    }

    private void connectToServer(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        createLoginGUI();

        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("Clients:")) {
                        updateFriendList(message.substring(8));
                    } else {
                        receiveMessage(message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void createFriendGUI() {
        friendFrame = new JFrame("Friends");
        friendFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        friendFrame.setSize(300, 400);
        friendFrame.setLayout(new BorderLayout());

        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selectedFriend = friendList.getSelectedValue();
                    if (selectedFriend != null) {
                        createAndShowChatGUI(selectedFriend);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(friendList);
        friendFrame.add(scrollPane, BorderLayout.CENTER);
        friendFrame.setLocationRelativeTo(null); // 将窗口居中显示
        friendFrame.setVisible(true);
    }

    private void createAndShowChatGUI(String friend) {
        ChatFrame chatFrame = new ChatFrame(friend, out, socket, receivedFiles);
        chatFrames.put(friend, chatFrame);
    }

    private void receiveMessage(String message) {
        try {
            System.out.println(message);
            if (message.startsWith("Text:")) {
                String[] parts = message.split(":", 3);
                String sender = parts[1];
                if (chatFrames.containsKey(sender) && chatFrames.get(sender).getFrame().isVisible()) {
                    chatFrames.get(sender).appendMessage("Client: " + parts[2] + "\n\n");
                } else {
                    friendListModel.set(friendListModel.indexOf(sender), sender + " (new message)");

                }
            } else if (message.startsWith("File:")) {
                String[] parts = message.split(":");
                String sender = parts[1];
                if (chatFrames.containsKey(sender) && chatFrames.get(sender).getFrame().isVisible()) {
                    chatFrames.get(sender).appendMessage("File received: " + parts[2] + "\n\n");
                } else {
                    friendListModel.set(friendListModel.indexOf(sender), sender + " (new message)");
                }
            } else if (message.startsWith("Audio:")) {
                String[] parts = message.split(":");
                String sender = parts[1];
                if (chatFrames.containsKey(sender) && chatFrames.get(sender).getFrame().isVisible()) {
                    chatFrames.get(sender).appendMessage("You received a Voice Chat...\n\n");
                } else {
                    friendListModel.set(friendListModel.indexOf(sender), sender + " (new message)");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateFriendList(String clients) {
        SwingUtilities.invokeLater(() -> {
            friendListModel.clear();
            Arrays.stream(clients.split(",")).forEach(friendListModel::addElement);
        });
    }
}
