package com.eventseat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final EventManager eventManager;

    private PrintWriter writer;

    // All currently connected clients
    private static final List<ClientHandler> clients =
            new CopyOnWriteArrayList<>();

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

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()
                    )
            );

            writer = new PrintWriter(
                    clientSocket.getOutputStream(),
                    true
            );

            // Add this client to connected client list
            clients.add(this);

            System.out.println(
                    "Client connected. Total clients: "
                            + clients.size()
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

                    // Send booking result to this client
                    writer.println(result);

                    // If booking successful,
                    // notify ALL connected clients
                    if (result.startsWith("BOOKING_SUCCESS")) {

                        broadcast(
                                "SEAT_UPDATE:"
                                        + seatNumber
                                        + ":BOOKED"
                        );
                    }

                } else {

                    writer.println("UNKNOWN_COMMAND");
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Client connection closed."
            );

        } finally {

            clients.remove(this);

            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(
                    "Client disconnected. Total clients: "
                            + clients.size()
            );
        }
    }

    private static void broadcast(String message) {

        for (ClientHandler client : clients) {

            if (client.writer != null) {
                client.writer.println(message);
            }
        }
    }
}