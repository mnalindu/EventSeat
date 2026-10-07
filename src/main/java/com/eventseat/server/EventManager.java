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
            String seatNumber
    ) {

        // Find the requested seat
        Seat seat =
                event.findSeat(seatNumber);


        // Seat does not exist
        if (seat == null) {

            return "SEAT_NOT_FOUND";
        }


        // Seat is already booked in server memory
        if (seat.isBooked()) {

            return "ALREADY_BOOKED:"
                    + seatNumber;
        }


        try {

            // Try to book the seat in MySQL
            String result =
                    bookingRepository.bookSeat(
                            event.getEventId(),
                            seatNumber
                    );


            // Booking successful
            if (result.startsWith(
                    "BOOKING_SUCCESS:"
            )) {

                // Update server memory
                // to keep it synchronized
                // with the database

                seat.setBooked(true);
            }


            // Another client may have booked
            // the seat before this request
            else if (result.startsWith(
                    "ALREADY_BOOKED:"
            )) {

                // Update server memory
                // because database says BOOKED

                seat.setBooked(true);
            }


            return result;


        } catch (SQLException e) {

            e.printStackTrace();

            return "DATABASE_ERROR";
        }
    }
}