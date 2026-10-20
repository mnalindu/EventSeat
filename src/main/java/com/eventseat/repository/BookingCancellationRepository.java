package com.eventseat.repository;

import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingCancellationRepository {

    public String cancelBooking(int bookingId, String customerName) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String eventId;
                String seatNumber;

                String findSql = "SELECT event_id, seat_number FROM bookings " +
                        "WHERE booking_id = ? AND customer_name = ?";

                try (PreparedStatement stmt = connection.prepareStatement(findSql)) {
                    stmt.setInt(1, bookingId);
                    stmt.setString(2, customerName);

                    try (ResultSet result = stmt.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return "BOOKING_NOT_FOUND";
                        }
                        eventId = result.getString("event_id");
                        seatNumber = result.getString("seat_number");
                    }
                }

                // Lock the seat before the booking, consistent with bookSeat updates.
                String lockSeatSql = "SELECT status FROM seats " +
                        "WHERE event_id = ? AND seat_number = ? FOR UPDATE";
                String seatStatus;
                try (PreparedStatement stmt = connection.prepareStatement(lockSeatSql)) {
                    stmt.setString(1, eventId);
                    stmt.setString(2, seatNumber);
                    try (ResultSet result = stmt.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return "CANCEL_FAILED";
                        }
                        seatStatus = result.getString("status");
                    }
                }

                String lockBookingSql = "SELECT event_id, seat_number, status FROM bookings " +
                        "WHERE booking_id = ? AND customer_name = ? FOR UPDATE";
                String bookingStatus;
                try (PreparedStatement stmt = connection.prepareStatement(lockBookingSql)) {
                    stmt.setInt(1, bookingId);
                    stmt.setString(2, customerName);
                    try (ResultSet result = stmt.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return "BOOKING_NOT_FOUND";
                        }
                        if (!eventId.equals(result.getString("event_id")) ||
                                !seatNumber.equals(result.getString("seat_number"))) {
                            connection.rollback();
                            return "CANCEL_FAILED";
                        }
                        bookingStatus = result.getString("status");
                    }
                }

                if ("CANCELLED".equalsIgnoreCase(bookingStatus)) {
                    connection.rollback();
                    return "ALREADY_CANCELLED:" + bookingId;
                }
                if (!"ACTIVE".equalsIgnoreCase(bookingStatus) ||
                        !"BOOKED".equalsIgnoreCase(seatStatus)) {
                    connection.rollback();
                    return "CANCEL_FAILED";
                }

                try (PreparedStatement stmt = connection.prepareStatement(
                        "UPDATE bookings SET status = 'CANCELLED' " +
                                "WHERE booking_id = ? AND status = 'ACTIVE'")) {
                    stmt.setInt(1, bookingId);
                    if (stmt.executeUpdate() != 1) {
                        connection.rollback();
                        return "CANCEL_FAILED";
                    }
                }

                try (PreparedStatement stmt = connection.prepareStatement(
                        "UPDATE seats SET status = 'AVAILABLE' " +
                                "WHERE event_id = ? AND seat_number = ? AND status = 'BOOKED'")) {
                    stmt.setString(1, eventId);
                    stmt.setString(2, seatNumber);
                    if (stmt.executeUpdate() != 1) {
                        connection.rollback();
                        return "CANCEL_FAILED";
                    }
                }

                connection.commit();
                return "CANCEL_SUCCESS:" + bookingId + ":" + eventId + ":" + seatNumber;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
