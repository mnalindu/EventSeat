package com.eventseat.model;

import java.util.ArrayList;
import java.util.List;

public class Event {

    private final String eventId;
    private final String name;
    private final String venue;
    private final String date;

    private final List<Seat> seats;

    public Event(String eventId, String name, String venue, String date) {
        this.eventId = eventId;
        this.name = name;
        this.venue = venue;
        this.date = date;
        this.seats = new ArrayList<>();
    }

    public String getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getVenue() {
        return venue;
    }

    public String getDate() {
        return date;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public void addSeat(Seat seat) {
        seats.add(seat);
    }

    public Seat findSeat(String seatNumber) {

        for (Seat seat : seats) {

            if (seat.getSeatNumber().equalsIgnoreCase(seatNumber)) {
                return seat;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return eventId + " - " + name + " - " + venue + " - " + date;
    }
}
