package com.eventseat.server;

import com.eventseat.model.Event;
import com.eventseat.model.Seat;
import com.eventseat.repository.BookingCancellationRepository;
import com.eventseat.repository.BookingRepository;
import com.eventseat.repository.EventRepository;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventManager {

    private final Map<String, Event> events;
    private final BookingRepository bookingRepository = new BookingRepository();
    private final BookingCancellationRepository cancellationRepository =
            new BookingCancellationRepository();

    public EventManager() {
        try {
            events = new LinkedHashMap<>(new EventRepository().loadAllEvents());
            if (events.isEmpty()) {
                throw new IllegalStateException("No events found in eventseat_db.");
            }
            System.out.println("Loaded " + events.size() + " events from MySQL.");
            for (Event event : events.values()) {
                System.out.println(event.getEventId() + " - " + event.getName() +
                        " (" + event.getSeats().size() + " seats)");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load events from MySQL.", e);
        }
    }

    public synchronized boolean hasEvent(String eventId) {
        return events.containsKey(eventId);
    }

    public synchronized String getEvents() {
        // Demo protocol: names/venues must not contain '|' or ';'.
        StringBuilder reply = new StringBuilder("EVENTS:");
        for (Event event : events.values()) {
            reply.append(event.getEventId()).append("|")
                    .append(event.getName()).append("|")
                    .append(event.getVenue()).append("|")
                    .append(event.getDate()).append(";");
        }
        return reply.toString();
    }

    public synchronized String getSeatStatus(String eventId) {
        Event event = events.get(eventId);
        if (event == null) return "EVENT_NOT_FOUND";

        StringBuilder reply = new StringBuilder("SEATS:").append(eventId).append(":");
        for (Seat seat : event.getSeats()) {
            reply.append(seat.getSeatNumber()).append("=")
                    .append(seat.isBooked() ? "BOOKED" : "AVAILABLE").append(",");
        }
        return reply.toString();
    }

    public synchronized String getEventDetails(String eventId) {
        Event event = events.get(eventId);
        return event == null ? "EVENT_NOT_FOUND" : event.toString();
    }

    public synchronized String bookSeat(
            String eventId, String seatNumber, String customerName) {

        Event event = events.get(eventId);
        if (event == null) return "EVENT_NOT_FOUND";

        Seat seat = event.findSeat(seatNumber);
        if (seat == null) return "SEAT_NOT_FOUND";
        if (customerName == null || customerName.isBlank() ||
                customerName.length() > 100 || customerName.contains(":")) {
            return "INVALID_CUSTOMER_NAME";
        }
        if (seat.isBooked()) return "ALREADY_BOOKED:" + seatNumber;

        try {
            String reply = bookingRepository.bookSeat(
                    eventId, seatNumber, customerName.trim());

            if (reply.startsWith("BOOKING_SUCCESS:") ||
                    reply.startsWith("ALREADY_BOOKED:")) {
                seat.setBooked(true);
            }
            return reply;
        } catch (SQLException e) {
            System.err.println("Database error while booking seat.");
            return "DATABASE_ERROR";
        }
    }

    public synchronized String cancelBooking(int bookingId, String customerName) {
        try {
            String reply = cancellationRepository.cancelBooking(bookingId, customerName);
            if (reply.startsWith("CANCEL_SUCCESS:")) {
                String[] parts = reply.split(":", 4);
                if (parts.length == 4) {
                    Event event = events.get(parts[2]);
                    if (event != null) {
                        Seat seat = event.findSeat(parts[3]);
                        if (seat != null) seat.setBooked(false);
                    }
                }
            }
            return reply;
        } catch (SQLException e) {
            System.err.println("Database error while cancelling booking.");
            return "DATABASE_ERROR";
        }
    }
}
