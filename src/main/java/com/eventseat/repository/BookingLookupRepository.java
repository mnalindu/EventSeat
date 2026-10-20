
package com.eventseat.repository;

import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingLookupRepository {

    public String findBooking(
            int bookingId,
            String customerName
    ) throws SQLException {

        String sql =
                "SELECT b.booking_id, b.seat_number, " +
                        "b.customer_name, b.booking_time, " +
                        "e.event_name, e.venue, e.event_date " +
                        "FROM bookings b " +
                        "JOIN events e ON b.event_id = e.event_id " +
                        "WHERE b.booking_id = ? " +
                        "AND b.customer_name = ?";

        try (Connection connection =
                     DatabaseConnection.getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, bookingId);
            statement.setString(2, customerName);

            try (ResultSet result = statement.executeQuery()) {

                if (!result.next()) {
                    return "BOOKING_NOT_FOUND";
                }

                return "BOOKING_DETAILS:"
                        + result.getInt("booking_id") + ":"
                        + result.getString("seat_number") + ":"
                        + result.getString("customer_name") + ":"
                        + result.getString("event_name") + ":"
                        + result.getString("venue") + ":"
                        + result.getDate("event_date") + ":"
                        + result.getTimestamp("booking_time");
            }
        }
    }
}
