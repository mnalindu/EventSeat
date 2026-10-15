package com.eventseat.repository;

import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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

                    String bookingSql =
                            "INSERT INTO bookings " +
                                    "(event_id, seat_number, customer_name) " +
                                    "VALUES (?, ?, ?)";

                    try (PreparedStatement statement =
                                 connection.prepareStatement(bookingSql)) {

                        statement.setString(1, eventId);
                        statement.setString(2, seatNumber);
                        statement.setString(3, customerName);

                        statement.executeUpdate();
                    }

                    connection.commit();

                    return "BOOKING_SUCCESS:" + seatNumber;
                }

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

                        String status = result.getString("status");

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