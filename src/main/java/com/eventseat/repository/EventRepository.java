package com.eventseat.repository;

import com.eventseat.model.Event;
import com.eventseat.model.Seat;
import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class EventRepository {

    public Map<String, Event> loadAllEvents() throws SQLException {
        Map<String, Event> events = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection()) {
            String eventSql = "SELECT event_id, event_name, venue, event_date " +
                    "FROM events ORDER BY event_date, event_id";

            try (PreparedStatement statement = connection.prepareStatement(eventSql);
                 ResultSet result = statement.executeQuery()) {

                while (result.next()) {
                    Event event = new Event(
                            result.getString("event_id"),
                            result.getString("event_name"),
                            result.getString("venue"),
                            result.getDate("event_date").toString()
                    );
                    events.put(event.getEventId(), event);
                }
            }

            String seatSql = "SELECT seat_number, status FROM seats " +
                    "WHERE event_id = ? ORDER BY seat_id";

            try (PreparedStatement statement = connection.prepareStatement(seatSql)) {
                for (Event event : events.values()) {
                    statement.setString(1, event.getEventId());

                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            Seat seat = new Seat(result.getString("seat_number"));
                            seat.setBooked("BOOKED".equalsIgnoreCase(result.getString("status")));
                            event.addSeat(seat);
                        }
                    }
                }
            }
        }

        return events;
    }
}
