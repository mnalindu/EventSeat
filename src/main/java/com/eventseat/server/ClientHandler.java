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

    // Store all connected clients
    private static final List<ClientHandler> clients =
            new CopyOnWriteArrayList<>();

    // Constructor
    public ClientHandler(
            Socket clientSocket,
            EventManager eventManager
    ) {
        this.clientSocket = clientSocket;
        this.eventManager = eventManager;
    }

    @Override
    public void run() {

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                clientSocket.getInputStream()
                        )
                );

                PrintWriter output = new PrintWriter(
                        clientSocket.getOutputStream(),
                        true
                )
        ) {

            writer = output;

            // Register this client
            clients.add(this);

            System.out.println(
                    "Client connected. Total clients: "
                            + clients.size()
            );

            String message;

            // Listen for commands continuously
            while ((message = reader.readLine()) != null) {

                System.out.println(
                        "Client says: " + message
                );

                // EXIT command
                if (message.equalsIgnoreCase("EXIT")) {

                    writer.println("Goodbye!");
                    break;

                }

                // Get event information
                else if (message.equalsIgnoreCase("GET_EVENT")) {

                    writer.println(
                            eventManager.getEventDetails()
                    );

                }

                // Get seat availability
                else if (message.equalsIgnoreCase("GET_SEATS")) {

                    writer.println(
                            eventManager.getSeatStatus()
                    );

                }

                // Book a seat with customer name
                else if (
                        message.toUpperCase().startsWith("BOOK:")
                ) {

                    // Expected: BOOK:A2:Kamal Perera
                    String[] parts = message.split(":", 3);

                    if (parts.length != 3) {

                        writer.println("INVALID_CUSTOMER_NAME");
                        continue;
                    }

                    String seatNumber =
                            parts[1].trim().toUpperCase();

                    String customerName =
                            parts[2].trim();

                    // Book seat through EventManager
                    String result = eventManager.bookSeat(
                            seatNumber,
                            customerName
                    );

                    // Send booking result
                    writer.println(result);

                    // Broadcast successful bookings
                    if (result.startsWith("BOOKING_SUCCESS:")) {

                        broadcast(
                                "SEAT_UPDATE:"
                                        + seatNumber
                                        + ":BOOKED"
                        );
                    }

                }

                // Unknown command
                else {

                    writer.println("UNKNOWN_COMMAND");

                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Client connection error: " + e.getMessage()
            );

        } finally {

            // Remove disconnected client
            clients.remove(this);

            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println(
                        "Error closing socket: " + e.getMessage()
                );
            }

            System.out.println(
                    "Client disconnected. Total clients: "
                            + clients.size()
            );
        }
    }

    // Send real-time updates to every connected client
    private static void broadcast(String message) {

        for (ClientHandler client : clients) {

            if (client.writer != null) {

                client.writer.println(message);
            }
        }
    }
}