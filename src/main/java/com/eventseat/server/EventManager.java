package com.eventseat.server;

import com.eventseat.model.Event;
import com.eventseat.model.Seat;
import com.eventseat.repository.BookingRepository;
import com.eventseat.repository.EventRepository;

import java.sql.SQLException;

public class EventManager {

    private final Event event;

    private final BookingRepository bookingRepository;

    public EventManager() {

        EventRepository eventRepository =
                new EventRepository();

        bookingRepository =
                new BookingRepository();

        try {

            // Load event and seats from MySQL
            event =
                    eventRepository.loadEvent("E001");

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


    // --------------------------------------------------
    // Get event details
    // --------------------------------------------------

    public String getEventDetails() {

        return event.toString();
    }


    // --------------------------------------------------
    // Get current seat status
    // --------------------------------------------------

    public synchronized String getSeatStatus() {

        StringBuilder result =
                new StringBuilder();

        for (Seat seat : event.getSeats()) {

            result.append(
                    seat.getSeatNumber()
            );

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


    // --------------------------------------------------
    // Book a seat
    // --------------------------------------------------

    public synchronized String bookSeat(
            String seatNumber,
            String customerName
    ) {

        if (customerName == null
                || customerName.isBlank()
                || customerName.length() > 100
                || customerName.contains(":")) {

            return "INVALID_CUSTOMER_NAME";
        }

        Seat seat = event.findSeat(seatNumber);

        if (seat == null) {
            return "SEAT_NOT_FOUND";
        }

        if (seat.isBooked()) {
            return "ALREADY_BOOKED:" + seat.getSeatNumber();
        }

        try {

            String result = bookingRepository.bookSeat(
                    event.getEventId(),
                    seat.getSeatNumber(),
                    customerName.trim()
            );

            if (result.startsWith("BOOKING_SUCCESS:")
                    || result.startsWith("ALREADY_BOOKED:")) {

                seat.setBooked(true);
            }

            return result;

        } catch (SQLException e) {

            e.printStackTrace();
            return "DATABASE_ERROR";
        }
    }
}