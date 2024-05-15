import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

public class Server {
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new HashMap<>();

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Server started on port " + port);

        while (true) {
            System.out.println("Waiting for client...");
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket);

            // 创建客户端处理器
            ClientHandler clientHandler = new ClientHandler(clientSocket);
            clientHandler.start();
        }
    }

    private class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // 读取用户名和密码
                username = in.readLine();
                String password = in.readLine();

                // 验证用户信息
                boolean authenticated = authenticateUser(username, password);

                // 发送验证结果给客户端
                if (authenticated) {
                    synchronized (clients) {
                        clients.put(username, this);
                        sendClientList();
                    }
                    out.println("Authentication successful");
                    System.out.println("Authentication successful for client: " + clientSocket);
                    // 发送当前用户列表给新登录用户
                    sendClientListToClient(this);
                } else {
                    out.println("Authentication failed");
                    System.out.println("Authentication failed for client: " + clientSocket);
                }

                // 等待客户端通信
                receiveMessage();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // 关闭连接
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
                    if (message.startsWith("Text:")) {
                        // 接收到文本消息
                        String[] parts = message.split(":", 3);
                        String targetUsername = parts[1];
                        String textMessage = parts[2];
                        System.out.println("Message from " + username + " to " + targetUsername + ": " + textMessage);
                        sendMessageToClient(targetUsername, textMessage);
                    } else if (message.startsWith("File:")) {
                        // 接收到文件消息
                        String[] parts = message.split(":", 4);
                        String targetUsername = parts[1];
                        String fileName = parts[2];
                        long fileSize = Long.parseLong(parts[3]);
                        System.out.println("File from " + username + " to " + targetUsername + ": " + fileName + " (" + fileSize + " bytes)");
                        receiveFile(targetUsername, fileName, fileSize);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void receiveFile(String targetUsername, String fileName, long fileSize) {
            try {
                // 创建文件输出流
                File receivedFile = new File(fileName);
                FileOutputStream fos = new FileOutputStream(receivedFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);

                // 从输入流中读取文件内容
                InputStream inputStream = clientSocket.getInputStream();

                byte[] buffer = new byte[1460];
                int bytesRead;
                long remaining = fileSize;

                while (remaining > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    bos.write(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }

                // 关闭流
                bos.close();
                fos.close();

                // 在服务端显示文件接收信息
                System.out.println("File received: " + fileName + " (" + receivedFile.length() + " bytes)");

                // 发送文件给目标客户端
                sendFileToClient(targetUsername, fileName, receivedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendFileToClient(String targetUsername, String fileName, File file) {
            try {
                byte[] buffer = new byte[(int) file.length()];
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(buffer, 0, buffer.length);

                ClientHandler targetClient = clients.get(targetUsername);
                if (targetClient != null) {
                    targetClient.sendFile(fileName, buffer);
                }

                bis.close();
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMessageToClient(String targetUsername, String message) {
            ClientHandler targetClient = clients.get(targetUsername);
            if (targetClient != null) {
                targetClient.sendMessage("Text:" + message);
            } else {
                System.out.println("Client " + targetUsername + " not found.");
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

        public void sendMessage(String message) {
            out.println(message);
        }

        public void stopConnection() throws IOException {
            in.close();
            out.close();
            clientSocket.close();
            synchronized (clients) {
                clients.remove(username);
                sendClientList();
            }
        }

        private boolean authenticateUser(String username, String password) {
            // 在实际情况下，你需要在这里编写验证用户信息的逻辑
            // 这里只是一个简单的示例，始终返回true
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

        private void sendClientList() {
            synchronized (clients) {
                for (ClientHandler client : clients.values()) {
                    String clientList = "Clients:" + clients.keySet().stream()
                            .filter(username -> !username.equals(client.username))
                            .collect(Collectors.joining(","));
                    client.sendMessage(clientList);
                }
            }
        }

        private void sendClientListToClient(ClientHandler client) {
            String clientList = "Clients:" + clients.keySet().stream()
                    .filter(username -> !username.equals(client.username))
                    .collect(Collectors.joining(","));
            client.sendMessage(clientList);
        }

    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start(3389);
    }
}
