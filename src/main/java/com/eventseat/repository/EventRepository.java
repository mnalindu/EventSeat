package com.eventseat.repository;

import com.eventseat.model.Event;
import com.eventseat.model.Seat;
import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class EventRepository {

    public Event loadEvent(String eventId) throws SQLException {

        String eventSql =
                "SELECT event_id, event_name, venue, event_date " +
                        "FROM events WHERE event_id = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement eventStatement =
                     connection.prepareStatement(eventSql)) {

            eventStatement.setString(1, eventId);

            ResultSet eventResult =
                    eventStatement.executeQuery();

            if (!eventResult.next()) {
                return null;
            }

            Event event = new Event(
                    eventResult.getString("event_id"),
                    eventResult.getString("event_name"),
                    eventResult.getString("venue"),
                    eventResult.getDate("event_date").toString()
            );

            loadSeats(connection, event);

            return event;
        }
    }

    private void loadSeats(
            Connection connection,
            Event event
    ) throws SQLException {

        String seatSql =
                "SELECT seat_number, status " +
                        "FROM seats " +
                        "WHERE event_id = ? " +
                        "ORDER BY seat_id";

        try (PreparedStatement seatStatement =
                     connection.prepareStatement(seatSql)) {

            seatStatement.setString(
                    1,
                    event.getEventId()
            );

            ResultSet seatResult =
                    seatStatement.executeQuery();

            while (seatResult.next()) {

                String seatNumber =
                        seatResult.getString(
                                "seat_number"
                        );

                String status =
                        seatResult.getString(
                                "status"
                        );

                Seat seat =
                        new Seat(seatNumber);

                if ("BOOKED".equalsIgnoreCase(status)) {
                    seat.setBooked(true);
                }

                event.addSeat(seat);
            }
        }
    }
}