package com.eventseat.util;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseTestMain {

    public static void main(String[] args) {

        try {

            Connection connection =
                    DatabaseConnection.getConnection();

            System.out.println(
                    "Database connected successfully!"
            );

            System.out.println(
                    "Database: "
                            + connection.getCatalog()
            );

            connection.close();

        } catch (SQLException e) {

            System.out.println(
                    "Database connection failed!"
            );

            e.printStackTrace();
        }
    }
}
