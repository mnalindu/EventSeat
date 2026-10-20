
package com.eventseat.repository;

import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingCancellationRepository {

    public String cancelBooking(
            int bookingId,
            String customerName,
            String allowedEventId
    ) throws SQLException {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // Find the booking
                String findSql =
                        "SELECT event_id, seat_number " +
                                "FROM bookings " +
                                "WHERE booking_id = ? " +
                                "AND customer_name = ?";

                String eventId;
                String seatNumber;

                try (PreparedStatement statement =
                             connection.prepareStatement(findSql)) {

                    statement.setInt(1, bookingId);
                    statement.setString(2, customerName);

                    try (ResultSet result =
                                 statement.executeQuery()) {

                        if (!result.next()) {
                            connection.rollback();
                            return "BOOKING_NOT_FOUND";
                        }

                        eventId = result.getString("event_id");
                        seatNumber = result.getString("seat_number");
                    }
                }

                // The current server supports event E001
                if (!allowedEventId.equals(eventId)) {
                    connection.rollback();
                    return "BOOKING_NOT_FOUND";
                }

                // Lock the seat row first.
                // This follows the same resource order
                // as the normal booking transaction.
                String lockSeatSql =
                        "SELECT status FROM seats " +
                                "WHERE event_id = ? " +
                                "AND seat_number = ? FOR UPDATE";

                String seatStatus;

                try (PreparedStatement statement =
                             connection.prepareStatement(lockSeatSql)) {

                    statement.setString(1, eventId);
                    statement.setString(2, seatNumber);

                    try (ResultSet result =
                                 statement.executeQuery()) {

                        if (!result.next()) {
                            connection.rollback();
                            return "CANCEL_FAILED";
                        }

                        seatStatus = result.getString("status");
                    }
                }

                // Lock the booking row and check its status
                String lockBookingSql =
                        "SELECT status FROM bookings " +
                                "WHERE booking_id = ? " +
                                "AND customer_name = ? FOR UPDATE";

                String bookingStatus;

                try (PreparedStatement statement =
                             connection.prepareStatement(lockBookingSql)) {

                    statement.setInt(1, bookingId);
                    statement.setString(2, customerName);

                    try (ResultSet result =
                                 statement.executeQuery()) {

                        if (!result.next()) {
                            connection.rollback();
                            return "BOOKING_NOT_FOUND";
                        }

                        bookingStatus = result.getString("status");
                    }
                }

                if ("CANCELLED".equalsIgnoreCase(bookingStatus)) {
                    connection.rollback();
                    return "ALREADY_CANCELLED:" + bookingId;
                }

                if (!"ACTIVE".equalsIgnoreCase(bookingStatus)
                        || !"BOOKED".equalsIgnoreCase(seatStatus)) {

                    connection.rollback();
                    return "CANCEL_FAILED";
                }

                // Mark booking as CANCELLED
                String cancelSql =
                        "UPDATE bookings SET status = 'CANCELLED' " +
                                "WHERE booking_id = ? AND status = 'ACTIVE'";

                int cancelledRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(cancelSql)) {

                    statement.setInt(1, bookingId);
                    cancelledRows = statement.executeUpdate();
                }

                if (cancelledRows != 1) {
                    connection.rollback();
                    return "CANCEL_FAILED";
                }

                // Release the seat
                String releaseSql =
                        "UPDATE seats SET status = 'AVAILABLE' " +
                                "WHERE event_id = ? " +
                                "AND seat_number = ? " +
                                "AND status = 'BOOKED'";

                int releasedRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(releaseSql)) {

                    statement.setString(1, eventId);
                    statement.setString(2, seatNumber);

                    releasedRows = statement.executeUpdate();
                }

                if (releasedRows != 1) {
                    connection.rollback();
                    return "CANCEL_FAILED";
                }

                // Both updates must succeed together
                connection.commit();

                return "CANCEL_SUCCESS:"
                        + bookingId + ":" + seatNumber;

            } catch (SQLException e) {

                connection.rollback();
                throw e;
            }
        }
    }
}
