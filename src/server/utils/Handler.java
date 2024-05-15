package server.utils;

import server.ClientHandler;

import java.io.PrintWriter;

public abstract class Handler {
    protected PrintWriter out;
    protected ClientHandler clientHandler;

    public Handler(PrintWriter out, ClientHandler clientHandler) {
        this.out = out;
        this.clientHandler = clientHandler;
    }

    public abstract void handle(String message);
}
