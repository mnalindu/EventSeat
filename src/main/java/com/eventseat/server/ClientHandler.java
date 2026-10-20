
package com.eventseat.server;

import com.eventseat.repository.BookingLookupRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final EventManager eventManager;

    private final BookingLookupRepository bookingLookupRepository =
            new BookingLookupRepository();

    private volatile PrintWriter writer;

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

        try (
                Socket socket = clientSocket;

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()
                        )
                );

                PrintWriter output = new PrintWriter(
                        socket.getOutputStream(),
                        true
                )
        ) {

            writer = output;
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

                // ----------------------------------
                // EXIT COMMAND
                // ----------------------------------

                if (message.equalsIgnoreCase("EXIT")) {

                    writer.println("Goodbye!");
                    break;
                }

                // ----------------------------------
                // GET EVENT DETAILS
                // ----------------------------------

                else if (message.equalsIgnoreCase("GET_EVENT")) {

                    writer.println(
                            eventManager.getEventDetails()
                    );
                }

                // ----------------------------------
                // GET CURRENT SEATS
                // ----------------------------------

                else if (message.equalsIgnoreCase("GET_SEATS")) {

                    writer.println(
                            eventManager.getSeatStatus()
                    );
                }

                // ----------------------------------
                // FIND BOOKING
                // ----------------------------------

                else if (message.startsWith("GET_BOOKING:")) {

                    String[] parts = message.split(":", 3);

                    if (parts.length != 3 ||
                            parts[2].isBlank() ||
                            parts[2].length() > 100 ||
                            parts[2].contains(":")) {

                        writer.println("INVALID_BOOKING_REQUEST");
                        continue;
                    }

                    try {

                        int bookingId =
                                Integer.parseInt(parts[1]);

                        if (bookingId <= 0) {

                            writer.println("INVALID_BOOKING_REQUEST");
                            continue;
                        }

                        String customerName =
                                parts[2].trim();

                        String result =
                                bookingLookupRepository.findBooking(
                                        bookingId,
                                        customerName
                                );

                        // Reply only to requesting client
                        writer.println(result);

                    } catch (NumberFormatException e) {

                        writer.println("INVALID_BOOKING_REQUEST");

                    } catch (SQLException e) {

                        System.err.println(
                                "Database error during booking lookup."
                        );

                        writer.println("DATABASE_ERROR");
                    }
                }

                // ----------------------------------
                // CANCEL BOOKING
                // ----------------------------------

                else if (message.startsWith("CANCEL_BOOKING:")) {

                    String[] parts = message.split(":", 3);

                    if (parts.length != 3 ||
                            parts[2].isBlank() ||
                            parts[2].length() > 100 ||
                            parts[2].contains(":")) {

                        writer.println("INVALID_CANCEL_REQUEST");
                        continue;
                    }

                    try {

                        int bookingId =
                                Integer.parseInt(parts[1]);

                        if (bookingId <= 0) {

                            writer.println("INVALID_CANCEL_REQUEST");
                            continue;
                        }

                        String customerName =
                                parts[2].trim();

                        // Keep change and broadcast ordered
                        synchronized (eventManager) {

                            String result =
                                    eventManager.cancelBooking(
                                            bookingId,
                                            customerName
                                    );

                            writer.println(result);

                            // Notify every client after success
                            if (result.startsWith("CANCEL_SUCCESS:")) {

                                String[] responseParts =
                                        result.split(":", 3);

                                if (responseParts.length == 3) {

                                    String seatNumber =
                                            responseParts[2];

                                    broadcast(
                                            "SEAT_UPDATE:"
                                                    + seatNumber
                                                    + ":AVAILABLE"
                                    );
                                }
                            }
                        }

                    } catch (NumberFormatException e) {

                        writer.println("INVALID_CANCEL_REQUEST");
                    }
                }

                // ----------------------------------
                // BOOK A SEAT
                // ----------------------------------

                else if (
                        message.toUpperCase(Locale.ROOT)
                                .startsWith("BOOK:")
                ) {

                    String[] parts = message.split(":", 3);

                    if (parts.length < 2 ||
                            parts[1].isBlank()) {

                        writer.println("SEAT_NOT_FOUND");
                        continue;
                    }

                    String seatNumber =
                            parts[1].trim()
                                    .toUpperCase(Locale.ROOT);

                    String customerName =
                            parts.length == 3
                                    ? parts[2].trim()
                                    : "Guest";

                    if (customerName.isBlank() ||
                            customerName.length() > 100 ||
                            customerName.contains(":")) {

                        writer.println("INVALID_CUSTOMER_NAME");
                        continue;
                    }

                    synchronized (eventManager) {

                        String result =
                                eventManager.bookSeat(
                                        seatNumber,
                                        customerName
                                );

                        // Reply to booking client
                        writer.println(result);

                        // Notify all clients after success
                        if (result.startsWith("BOOKING_SUCCESS:")) {

                            broadcast(
                                    "SEAT_UPDATE:"
                                            + seatNumber
                                            + ":BOOKED"
                            );
                        }
                    }
                }

                // ----------------------------------
                // UNKNOWN COMMAND
                // ----------------------------------

                else {

                    writer.println("UNKNOWN_COMMAND");
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Client connection closed: "
                            + e.getMessage()
            );

        } finally {

            clients.remove(this);
            writer = null;

            System.out.println(
                    "Client disconnected. Total clients: "
                            + clients.size()
            );
        }
    }

    // -----------------------------------------
    // BROADCAST UPDATE TO ALL CLIENTS
    // -----------------------------------------

    private static void broadcast(String message) {

        for (ClientHandler client : clients) {

            PrintWriter clientWriter = client.writer;

            if (clientWriter != null) {

                clientWriter.println(message);
            }
        }
    }
}
