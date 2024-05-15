package client.entry;

import client.Client;

import java.io.*;

public class Client1 {
    private static Client client;

    public static void main(String[] args) throws IOException {
        client = new Client("localhost", 3389);
        //client = new Client.Client("localhost", 3389);
    }
}
