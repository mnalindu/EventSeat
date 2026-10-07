package com.eventseat.repository;

import com.eventseat.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BookingRepository {

    public String bookSeat(
            String eventId,
            String seatNumber
    ) throws SQLException {

        Connection connection =
                DatabaseConnection.getConnection();

        try {

            // We control commit/rollback manually
            connection.setAutoCommit(false);

            String updateSeatSql =
                    "UPDATE seats " +
                            "SET status = 'BOOKED' " +
                            "WHERE event_id = ? " +
                            "AND seat_number = ? " +
                            "AND status = 'AVAILABLE'";

            try (PreparedStatement updateStatement =
                         connection.prepareStatement(updateSeatSql)) {

                updateStatement.setString(1, eventId);
                updateStatement.setString(2, seatNumber);

                int updatedRows =
                        updateStatement.executeUpdate();

                /*
                 * updatedRows == 1 means:
                 * seat existed AND it was AVAILABLE
                 */
                if (updatedRows == 1) {

                    String insertBookingSql =
                            "INSERT INTO bookings " +
                                    "(event_id, seat_number) " +
                                    "VALUES (?, ?)";

                    try (PreparedStatement bookingStatement =
                                 connection.prepareStatement(
                                         insertBookingSql
                                 )) {

                        bookingStatement.setString(
                                1,
                                eventId
                        );

                        bookingStatement.setString(
                                2,
                                seatNumber
                        );

                        bookingStatement.executeUpdate();
                    }

                    connection.commit();

                    return "BOOKING_SUCCESS:"
                            + seatNumber;
                }

                /*
                 * No row was updated.
                 * Check whether the seat exists.
                 */
                String checkSeatSql =
                        "SELECT status " +
                                "FROM seats " +
                                "WHERE event_id = ? " +
                                "AND seat_number = ?";

                try (PreparedStatement checkStatement =
                             connection.prepareStatement(
                                     checkSeatSql
                             )) {

                    checkStatement.setString(
                            1,
                            eventId
                    );

                    checkStatement.setString(
                            2,
                            seatNumber
                    );

                    ResultSet resultSet =
                            checkStatement.executeQuery();

                    if (!resultSet.next()) {

                        connection.rollback();

                        return "SEAT_NOT_FOUND";
                    }

                    String status =
                            resultSet.getString(
                                    "status"
                            );

                    connection.rollback();

                    if ("BOOKED".equalsIgnoreCase(status)) {

                        return "ALREADY_BOOKED:"
                                + seatNumber;
                    }

                    return "BOOKING_FAILED:"
                            + seatNumber;
                }
            }

        } catch (SQLException e) {

            connection.rollback();
            throw e;

        } finally {

            connection.setAutoCommit(true);
            connection.close();
        }
    }
}