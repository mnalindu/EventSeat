package com.eventseat.server;


import com.eventseat.model.Event;
import com.eventseat.model.Seat;

public class EventManager {
    private final Event event;

    public EventManager() {

        event = new Event(
                "E001",
                "Music Night 2026",
                "Main Auditorium",
                "2026-11-20"
        );

        event.addSeat(new Seat("A1"));
        event.addSeat(new Seat("A2"));
        event.addSeat(new Seat("A3"));
        event.addSeat(new Seat("A4"));

        event.addSeat(new Seat("B1"));
        event.addSeat(new Seat("B2"));
        event.addSeat(new Seat("B3"));
        event.addSeat(new Seat("B4"));
    }

    public String getEventDetails() {
        return event.toString();
    }

    public String getSeatStatus() {

        StringBuilder result = new StringBuilder();

        for (Seat seat : event.getSeats()) {

            result.append(seat.getSeatNumber())
                    .append("=")
                    .append(seat.isBooked() ? "BOOKED" : "AVAILABLE")
                    .append(",");
        }

        return result.toString();
    }

    public String bookSeat(String seatNumber) {

        Seat seat = event.findSeat(seatNumber);

        if (seat == null) {
            return "SEAT_NOT_FOUND";
        }

        if (seat.isBooked()) {
            return "ALREADY_BOOKED:" + seatNumber;
        }

        seat.setBooked(true);

        return "BOOKING_SUCCESS:" + seatNumber;
    }
}
