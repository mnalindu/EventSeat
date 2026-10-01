package com.eventseat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {

    private final Socket clientSocket;

    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {

        try {
            System.out.println(
                    "Handling client: " + clientSocket.getInetAddress()
            );

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );

            PrintWriter writer = new PrintWriter(
                    clientSocket.getOutputStream(),
                    true
            );

            String message;

            while ((message = reader.readLine()) != null) {

                System.out.println(
                        "Client " +
                                clientSocket.getInetAddress() +
                                " says: " +
                                message
                );

                if (message.equalsIgnoreCase("EXIT")) {
                    writer.println("Goodbye!");
                    break;
                }

                writer.println("Server received: " + message);
            }

            reader.close();
            writer.close();
            clientSocket.close();

            System.out.println("Client disconnected.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}