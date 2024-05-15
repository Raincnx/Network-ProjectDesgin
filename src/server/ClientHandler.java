package server;

import server.utils.AudioHandler;
import server.utils.FileHandler;
import server.utils.Handler;
import server.utils.TextHandler;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private Map<String, Handler> handlers;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.handlers = new HashMap<>();
    }

    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            // 初始化各个处理类
            handlers.put("Text", new TextHandler(out, this));
            handlers.put("File", new FileHandler(out, clientSocket, this));
            handlers.put("Audio", new AudioHandler(out, clientSocket, this));

            username = in.readLine();
            String password = in.readLine();

            boolean authenticated = authenticateUser(username, password);

            if (authenticated) {
                synchronized (Server.clients) {
                    Server.clients.put(username, this);
                    sendClientList();
                }
                out.println("Authentication successful");
                System.out.println("Authentication successful for client: " + clientSocket);
                sendClientListToClient(this);
            } else {
                out.println("Authentication failed");
                System.out.println("Authentication failed for client: " + clientSocket);
            }

            receiveMessage();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stopConnection();
                System.out.println("Client disconnected: " + clientSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveMessage() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String[] parts = message.split(":", 2);
                if (parts.length >= 2) {
                    String messageType = parts[0];
                    String content = parts[1];
                    Handler handler = handlers.get(messageType);
                    if (handler != null) {
                        handler.handle(message);
                    } else {
                        System.err.println("Unknown message type: " + messageType);
                    }
                } else {
                    System.err.println("Invalid message format: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String fileName, byte[] fileData) {
        try {
            out.println("File:" + fileName + ":" + fileData.length);
            out.flush();

            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(fileData, 0, fileData.length);
            outputStream.flush();

            System.out.println("Sent file: " + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAudio(String audioname, byte[] audioData) {
        try {
            out.println("Audio:" + audioname + ":" + audioData.length);
            out.flush();

            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(audioData, 0, audioData.length);
            outputStream.flush();

            System.out.println("Sent audio: " + audioname);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        synchronized (Server.clients) {
            Server.clients.remove(username);
            sendClientList();
        }
    }

    public boolean authenticateUser(String username, String password) {
        if (Objects.equals(username, "user1") && Objects.equals(password, "123456")) {
            return true;
        }
        if (Objects.equals(username, "user2") && Objects.equals(password, "123")) {
            return true;
        }
        if (Objects.equals(username, "user3") && Objects.equals(password, "123")) {
            return true;
        }
        return false;
    }

    public void sendClientList() {
        synchronized (Server.clients) {
            for (ClientHandler client : Server.clients.values()) {
                String clientList = "Clients:" + Server.clients.keySet().stream()
                        .filter(username -> !username.equals(client.username))
                        .collect(Collectors.joining(","));
                client.sendMessage(clientList);
            }
        }
    }

    public void sendClientListToClient(ClientHandler client) {
        String clientList = "Clients:" + Server.clients.keySet().stream()
                .filter(username -> !username.equals(client.username))
                .collect(Collectors.joining(","));
        client.sendMessage(clientList);
    }

    public String getUsername() {
        return username;
    }

    public Map<String, ClientHandler> getClients() {
        return Server.clients;
    }
}
