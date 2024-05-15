package client.entry;

import client.Client;

import java.io.*;

public class Client2 {
    private static Client client;

    public static void main(String[] args) throws IOException {
        //client = new Client.Client("121.43.55.5", 3389);
        client = new Client("localhost", 3389);
    }
}
