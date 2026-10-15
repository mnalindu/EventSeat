
package com.eventseat.repository;

import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

public class BookingRepository {

    public String bookSeat(
            String eventId,
            String seatNumber,
            String customerName
    ) throws SQLException {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                // Step 1: Update seat status
                String updateSql =
                        "UPDATE seats " +
                                "SET status = 'BOOKED' " +
                                "WHERE event_id = ? " +
                                "AND seat_number = ? " +
                                "AND status = 'AVAILABLE'";

                int updatedRows;

                try (PreparedStatement statement =
                             connection.prepareStatement(updateSql)) {

                    statement.setString(1, eventId);
                    statement.setString(2, seatNumber);

                    updatedRows = statement.executeUpdate();
                }

                if (updatedRows == 1) {

                    // Step 2: Insert booking
                    String bookingSql =
                            "INSERT INTO bookings " +
                                    "(event_id, seat_number, customer_name) " +
                                    "VALUES (?, ?, ?)";

                    int bookingId;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(
                                         bookingSql,
                                         Statement.RETURN_GENERATED_KEYS
                                 )) {

                        statement.setString(1, eventId);
                        statement.setString(2, seatNumber);
                        statement.setString(3, customerName);

                        statement.executeUpdate();

                        // Step 3: Get generated booking ID
                        try (ResultSet keys =
                                     statement.getGeneratedKeys()) {

                            if (!keys.next()) {
                                throw new SQLException(
                                        "Booking ID was not generated."
                                );
                            }

                            bookingId = keys.getInt(1);
                        }
                    }

                    // Step 4: Get booking time from MySQL
                    String timeSql =
                            "SELECT booking_time " +
                                    "FROM bookings WHERE booking_id = ?";

                    String bookingTime;

                    try (PreparedStatement statement =
                                 connection.prepareStatement(timeSql)) {

                        statement.setInt(1, bookingId);

                        try (ResultSet result =
                                     statement.executeQuery()) {

                            if (!result.next()) {
                                throw new SQLException(
                                        "Booking time not found."
                                );
                            }

                            Timestamp timestamp =
                                    result.getTimestamp("booking_time");

                            bookingTime = timestamp
                                    .toLocalDateTime()
                                    .format(
                                            DateTimeFormatter.ofPattern(
                                                    "yyyy-MM-dd HH:mm:ss"
                                            )
                                    );
                        }
                    }

                    // Step 5: Save transaction
                    connection.commit();

                    // Return booking confirmation data
                    return "BOOKING_SUCCESS:"
                            + seatNumber
                            + ":" + bookingId
                            + ":" + bookingTime;
                }

                // Seat was not updated
                connection.rollback();

                String checkSql =
                        "SELECT status FROM seats " +
                                "WHERE event_id = ? " +
                                "AND seat_number = ?";

                try (PreparedStatement statement =
                             connection.prepareStatement(checkSql)) {

                    statement.setString(1, eventId);
                    statement.setString(2, seatNumber);

                    try (ResultSet result =
                                 statement.executeQuery()) {

                        if (!result.next()) {
                            return "SEAT_NOT_FOUND";
                        }

                        String status =
                                result.getString("status");

                        if ("BOOKED".equalsIgnoreCase(status)) {
                            return "ALREADY_BOOKED:" + seatNumber;
                        }
                    }
                }

                return "BOOKING_FAILED:" + seatNumber;

            } catch (SQLException e) {

                connection.rollback();
                throw e;
            }
        }
    }
}
