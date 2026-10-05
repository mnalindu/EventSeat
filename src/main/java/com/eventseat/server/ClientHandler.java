package com.eventseat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final EventManager eventManager;

    public ClientHandler(
            Socket clientSocket,
            EventManager eventManager
    ) {
        this.clientSocket = clientSocket;
        this.eventManager = eventManager;
    }

    @Override
    public void run() {

        try {

            System.out.println(
                    "Handling client: " +
                            clientSocket.getInetAddress()
            );

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()
                    )
            );

            PrintWriter writer = new PrintWriter(
                    clientSocket.getOutputStream(),
                    true
            );

            String message;

            while ((message = reader.readLine()) != null) {

                System.out.println(
                        "Client says: " + message
                );

                if (message.equalsIgnoreCase("EXIT")) {

                    writer.println("Goodbye!");
                    break;

                } else if (
                        message.equalsIgnoreCase("GET_EVENT")
                ) {

                    writer.println(
                            eventManager.getEventDetails()
                    );

                } else if (
                        message.equalsIgnoreCase("GET_SEATS")
                ) {

                    writer.println(
                            eventManager.getSeatStatus()
                    );

                } else if (
                        message.toUpperCase().startsWith("BOOK:")
                ) {

                    String seatNumber =
                            message.substring(5).trim();

                    String result =
                            eventManager.bookSeat(seatNumber);

                    writer.println(result);

                } else {

                    writer.println("UNKNOWN_COMMAND");
                }
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