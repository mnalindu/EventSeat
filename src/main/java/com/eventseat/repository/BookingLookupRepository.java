
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
                        "b.customer_name, b.booking_time, b.status, " +
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

            try (ResultSet result =
                         statement.executeQuery()) {

                if (!result.next()) {
                    return "BOOKING_NOT_FOUND";
                }

                int id = result.getInt("booking_id");

                String seatNumber =
                        result.getString("seat_number");

                String name =
                        result.getString("customer_name");

                String eventName =
                        result.getString("event_name");

                String venue =
                        result.getString("venue");

                String eventDate =
                        result.getDate("event_date").toString();

                String bookingStatus =
                        result.getString("status");

                String bookingTime =
                        result.getTimestamp("booking_time").toString();

                return "BOOKING_DETAILS:"
                        + id + ":"
                        + seatNumber + ":"
                        + name + ":"
                        + eventName + ":"
                        + venue + ":"
                        + eventDate + ":"
                        + bookingStatus + ":"
                        + bookingTime;
            }
        }
    }
}
