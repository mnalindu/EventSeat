package com.eventseat.server;

import com.eventseat.model.Event;
import com.eventseat.model.Seat;
import com.eventseat.repository.EventRepository;

import java.sql.SQLException;

public class EventManager {

    private final Event event;

    public EventManager() {

        EventRepository repository =
                new EventRepository();

        try {

            event = repository.loadEvent("E001");

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

    public String getEventDetails() {
        return event.toString();
    }

    public synchronized String getSeatStatus() {

        StringBuilder result =
                new StringBuilder();

        for (Seat seat : event.getSeats()) {

            result.append(
                    seat.getSeatNumber()
            );

            result.append("=");

            result.append(
                    seat.isBooked()
                            ? "BOOKED"
                            : "AVAILABLE"
            );

            result.append(",");
        }

        return result.toString();
    }

    public synchronized String bookSeat(
            String seatNumber
    ) {

        Seat seat =
                event.findSeat(seatNumber);

        if (seat == null) {
            return "SEAT_NOT_FOUND";
        }

        if (seat.isBooked()) {
            return "ALREADY_BOOKED:"
                    + seatNumber;
        }

        seat.setBooked(true);

        return "BOOKING_SUCCESS:"
                + seatNumber;
    }
}