
package com.eventseat.server;

import com.eventseat.model.Event;
import com.eventseat.model.Seat;
import com.eventseat.repository.BookingRepository;
import com.eventseat.repository.EventRepository;
import com.eventseat.repository.BookingCancellationRepository;

import java.sql.SQLException;

public class EventManager {

    private final Event event;

    private final BookingRepository bookingRepository;

    private final BookingCancellationRepository cancellationRepository;

    public EventManager() {

        EventRepository eventRepository =
                new EventRepository();

        bookingRepository = new BookingRepository();

        cancellationRepository =
                new BookingCancellationRepository();

        try {

            // Load event and seats from MySQL
            event = eventRepository.loadEvent("E001");

            if (event == null) {

                throw new IllegalStateException(
                        "Event E001 was not found in database."
                );
            }

            System.out.println(
                    "Event loaded from database: "
                            + event.getName()
            );

            System.out.println(
                    "Seats loaded: "
                            + event.getSeats().size()
            );

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Failed to load event from database.",
                    e
            );
        }
    }

    // -----------------------------------------
    // GET EVENT DETAILS
    // -----------------------------------------

    public String getEventDetails() {

        return event.toString();
    }

    // -----------------------------------------
    // GET SEAT STATUS
    // -----------------------------------------

    public synchronized String getSeatStatus() {

        StringBuilder result = new StringBuilder();

        for (Seat seat : event.getSeats()) {

            result.append(seat.getSeatNumber());

            result.append("=");

            if (seat.isBooked()) {

                result.append("BOOKED");

            } else {

                result.append("AVAILABLE");
            }

            result.append(",");
        }

        return result.toString();
    }

    // -----------------------------------------
    // BOOK SEAT WITHOUT CUSTOMER NAME
    // -----------------------------------------

    public synchronized String bookSeat(String seatNumber) {

        return bookSeat(seatNumber, "Guest");
    }

    // -----------------------------------------
    // BOOK SEAT WITH CUSTOMER NAME
    // -----------------------------------------

    public synchronized String bookSeat(
            String seatNumber,
            String customerName
    ) {

        Seat seat = event.findSeat(seatNumber);

        if (seat == null) {

            return "SEAT_NOT_FOUND";
        }

        if (customerName == null ||
                customerName.isBlank() ||
                customerName.length() > 100) {

            return "INVALID_CUSTOMER_NAME";
        }

        if (seat.isBooked()) {

            return "ALREADY_BOOKED:" + seatNumber;
        }

        try {

            // Save booking in MySQL
            String result =
                    bookingRepository.bookSeat(
                            event.getEventId(),
                            seatNumber,
                            customerName.trim()
                    );

            // Update memory when MySQL confirms booking
            if (result.startsWith("BOOKING_SUCCESS:")) {

                seat.setBooked(true);
            }

            // Database may already have a booking
            else if (result.startsWith("ALREADY_BOOKED:")) {

                seat.setBooked(true);
            }

            return result;

        } catch (SQLException e) {

            System.err.println(
                    "Database error while booking seat."
            );

            e.printStackTrace();

            return "DATABASE_ERROR";
        }
    }

    // -----------------------------------------
    // CANCEL BOOKING
    // -----------------------------------------

    public synchronized String cancelBooking(
            int bookingId,
            String customerName
    ) {

        try {

            String result =
                    cancellationRepository.cancelBooking(
                            bookingId,
                            customerName,
                            event.getEventId()
                    );

            if (result.startsWith("CANCEL_SUCCESS:")) {

                String[] parts = result.split(":", 3);

                if (parts.length == 3) {

                    String seatNumber = parts[2];

                    Seat seat = event.findSeat(seatNumber);

                    if (seat != null) {

                        // Make the seat available again
                        seat.setBooked(false);
                    }

                    System.out.println(
                            "Booking cancelled: #" + bookingId
                    );

                    System.out.println(
                            "Seat " + seatNumber +
                                    " is now AVAILABLE."
                    );
                }
            }

            return result;

        } catch (SQLException e) {

            System.err.println(
                    "Database error while cancelling booking."
            );

            e.printStackTrace();

            return "DATABASE_ERROR";
        }
    }
}
