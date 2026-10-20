
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

    private final Socket socket;
    private final EventManager eventManager;
    private final BookingLookupRepository lookupRepository =
            new BookingLookupRepository();

    private volatile PrintWriter writer;
    private volatile String subscribedEventId;

    private static final List<ClientHandler> clients =
            new CopyOnWriteArrayList<>();

    public ClientHandler(Socket socket, EventManager eventManager) {
        this.socket = socket;
        this.eventManager = eventManager;
    }

    @Override
    public void run() {

        try (Socket client = socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(client.getInputStream()));
             PrintWriter output = new PrintWriter(
                     client.getOutputStream(), true)) {

            writer = output;
            clients.add(this);

            System.out.println(
                    "Client connected. Total clients: " + clients.size()
            );

            String message;

            while ((message = reader.readLine()) != null) {

                if (message.equalsIgnoreCase("EXIT")) {
                    output.println("Goodbye!");
                    break;
                }

                if (message.equalsIgnoreCase("GET_EVENTS")) {

                    subscribedEventId = null;
                    output.println(eventManager.getEvents());

                } else if (message.equalsIgnoreCase("GET_EVENT")) {

                    output.println(
                            eventManager.getEventDetails("E001")
                    );

                } else if (message.equalsIgnoreCase("GET_SEATS") ||
                        message.toUpperCase(Locale.ROOT)
                                .startsWith("GET_SEATS:")) {

                    String eventId = message.equalsIgnoreCase("GET_SEATS")
                            ? "E001"
                            : message.substring(
                            "GET_SEATS:".length()
                    ).trim();

                    synchronized (eventManager) {

                        String seats =
                                eventManager.getSeatStatus(eventId);

                        subscribedEventId =
                                seats.startsWith("SEATS:")
                                        ? eventId : null;

                        output.println(seats);
                    }

                } else if (message.startsWith("GET_BOOKING:")) {

                    String[] parts = message.split(":", 3);

                    if (parts.length != 3 ||
                            parts[2].isBlank() ||
                            parts[2].length() > 100) {

                        output.println("INVALID_BOOKING_REQUEST");
                        continue;
                    }

                    try {

                        int bookingId =
                                Integer.parseInt(parts[1]);

                        if (bookingId <= 0) {
                            output.println("INVALID_BOOKING_REQUEST");
                            continue;
                        }

                        output.println(
                                lookupRepository.findBooking(
                                        bookingId,
                                        parts[2].trim()
                                )
                        );

                    } catch (NumberFormatException e) {

                        output.println("INVALID_BOOKING_REQUEST");

                    } catch (SQLException e) {

                        output.println("DATABASE_ERROR");
                    }

                } else if (message.startsWith("CANCEL_BOOKING:")) {

                    String[] parts = message.split(":", 3);

                    if (parts.length != 3 ||
                            parts[2].isBlank() ||
                            parts[2].length() > 100) {

                        output.println("INVALID_CANCEL_REQUEST");
                        continue;
                    }

                    try {

                        int bookingId =
                                Integer.parseInt(parts[1]);

                        if (bookingId <= 0) {
                            output.println("INVALID_CANCEL_REQUEST");
                            continue;
                        }

                        synchronized (eventManager) {

                            String reply =
                                    eventManager.cancelBooking(
                                            bookingId,
                                            parts[2].trim()
                                    );

                            output.println(reply);

                            if (reply.startsWith("CANCEL_SUCCESS:")) {

                                String[] result = reply.split(":", 4);

                                if (result.length == 4) {

                                    broadcastToEvent(
                                            result[2],
                                            "SEAT_UPDATE:"
                                                    + result[2] + ":"
                                                    + result[3]
                                                    + ":AVAILABLE"
                                    );
                                }
                            }
                        }

                    } catch (NumberFormatException e) {

                        output.println("INVALID_CANCEL_REQUEST");
                    }

                } else if (message.toUpperCase(Locale.ROOT)
                        .startsWith("BOOK:")) {

                    String[] parts = message.split(":", 4);

                    String eventId;
                    String seatNumber;
                    String customerName;

                    if (parts.length == 4) {

                        eventId = parts[1].trim();

                        seatNumber = parts[2].trim()
                                .toUpperCase(Locale.ROOT);

                        customerName = parts[3].trim();

                    } else if (parts.length == 3) {

                        // Support old single-event console command
                        eventId = "E001";

                        seatNumber = parts[1].trim()
                                .toUpperCase(Locale.ROOT);

                        customerName = parts[2].trim();

                    } else {

                        output.println("INVALID_BOOKING_REQUEST");
                        continue;
                    }

                    if (eventId.isBlank() ||
                            seatNumber.isBlank() ||
                            customerName.isBlank() ||
                            customerName.length() > 100 ||
                            customerName.contains(":")) {

                        output.println("INVALID_BOOKING_REQUEST");
                        continue;
                    }

                    synchronized (eventManager) {

                        String reply = eventManager.bookSeat(
                                eventId,
                                seatNumber,
                                customerName
                        );

                        if (reply.startsWith("BOOKING_SUCCESS:")) {

                            String[] result = reply.split(":", 3);

                            if (result.length == 3) {

                                output.println(
                                        "BOOKING_SUCCESS:"
                                                + eventId + ":"
                                                + seatNumber + ":"
                                                + result[2]
                                );

                                broadcastToEvent(
                                        eventId,
                                        "SEAT_UPDATE:"
                                                + eventId + ":"
                                                + seatNumber
                                                + ":BOOKED"
                                );

                            } else {

                                output.println("DATABASE_ERROR");
                            }

                        } else if (reply.startsWith("ALREADY_BOOKED:")) {

                            output.println(
                                    "ALREADY_BOOKED:"
                                            + eventId + ":"
                                            + seatNumber
                            );

                        } else {

                            output.println(reply);
                        }
                    }

                } else {

                    output.println("UNKNOWN_COMMAND");
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Client connection closed: " + e.getMessage()
            );

        } finally {

            clients.remove(this);
            writer = null;
            subscribedEventId = null;

            System.out.println(
                    "Client disconnected. Total clients: "
                            + clients.size()
            );
        }
    }

    private static void broadcastToEvent(
            String eventId,
            String message
    ) {

        for (ClientHandler client : clients) {

            PrintWriter clientWriter = client.writer;

            if (clientWriter != null &&
                    eventId.equals(client.subscribedEventId)) {

                clientWriter.println(message);
            }
        }
    }
}
