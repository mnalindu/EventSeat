package com.eventseat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EventSeatApp extends Application {

    private Stage stage;

    private ServerConnection serverConnection;

    private Label selectedSeatLabel;
    private Label bookingStatusLabel;

    // Customer name input field
    private TextField customerNameField;

    private String selectedSeat = null;

    private final Map<String, Button> seatButtons =
            new HashMap<>();

    // -----------------------------------------
    // Application Start
    // -----------------------------------------

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        connectToServer();

        showHomeScreen();

        stage.setTitle("EventSeat");

        stage.setOnCloseRequest(event -> {

            if (serverConnection != null) {
                serverConnection.close();
            }

        });

        stage.show();
    }

    // -----------------------------------------
    // Connect to Server
    // -----------------------------------------

    private void connectToServer() {

        serverConnection =
                new ServerConnection(message -> {

                    Platform.runLater(() -> {
                        handleServerMessage(message);
                    });

                });

        try {

            serverConnection.connect(
                    "localhost",
                    5000
            );

            System.out.println(
                    "Connected to EventSeat server."
            );

        } catch (IOException e) {

            System.out.println(
                    "Could not connect to server."
            );

        }
    }

    // -----------------------------------------
    // Home Screen
    // -----------------------------------------

    private void showHomeScreen() {

        Label title = new Label("EventSeat");

        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;"
        );

        Label subtitle = new Label(
                "Real-Time Event Seat Booking System"
        );

        Label connectionLabel = new Label();

        if (serverConnection != null
                && serverConnection.isConnected()) {

            connectionLabel.setText(
                    "Server: Connected"
            );

            connectionLabel.setStyle(
                    "-fx-text-fill: green;"
            );

        } else {

            connectionLabel.setText(
                    "Server: Not Connected"
            );

            connectionLabel.setStyle(
                    "-fx-text-fill: red;"
            );
        }

        Button viewEventsButton =
                new Button("View Events");

        viewEventsButton.setOnAction(event -> {
            showEventScreen();
        });

        VBox layout = new VBox(
                20,
                title,
                subtitle,
                connectionLabel,
                viewEventsButton
        );

        layout.setAlignment(Pos.CENTER);

        Scene scene = new Scene(
                layout,
                700,
                500
        );

        stage.setScene(scene);
    }

    // -----------------------------------------
    // Event Screen
    // -----------------------------------------

    private void showEventScreen() {

        selectedSeat = null;

        seatButtons.clear();

        Label title = new Label(
                "Music Night 2026"
        );

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        Label venue = new Label(
                "Main Auditorium"
        );

        Label date = new Label(
                "20 November 2026"
        );

        Label stageLabel = new Label(
                "STAGE"
        );

        stageLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px;" +
                        "-fx-border-color: black;"
        );

        // -------------------------------------
        // Seat Grid
        // -------------------------------------

        GridPane seatGrid = new GridPane();

        seatGrid.setHgap(15);
        seatGrid.setVgap(15);

        seatGrid.setAlignment(Pos.CENTER);

        String[][] seatNumbers = {

                {"A1", "A2", "A3", "A4"},
                {"B1", "B2", "B3", "B4"}

        };

        for (int row = 0;
             row < seatNumbers.length;
             row++) {

            for (int column = 0;
                 column < seatNumbers[row].length;
                 column++) {

                String seatNumber =
                        seatNumbers[row][column];

                Button seatButton =
                        createSeatButton(seatNumber);

                seatGrid.add(
                        seatButton,
                        column,
                        row
                );
            }
        }

        // -------------------------------------
        // Selected Seat Label
        // -------------------------------------

        selectedSeatLabel = new Label(
                "Selected Seat: None"
        );

        // -------------------------------------
        // Customer Name Input - NEW
        // -------------------------------------

        Label customerNameLabel = new Label(
                "Customer Name:"
        );

        customerNameField = new TextField();

        customerNameField.setPromptText(
                "Enter your full name"
        );

        customerNameField.setMaxWidth(250);

        // -------------------------------------
        // Booking Status Label
        // -------------------------------------

        bookingStatusLabel = new Label("");

        // -------------------------------------
        // Book Button
        // -------------------------------------

        Button bookButton = new Button(
                "Book Selected Seat"
        );

        bookButton.setOnAction(event -> {

            // Check seat selection
            if (selectedSeat == null) {

                bookingStatusLabel.setText(
                        "Please select a seat first."
                );

                return;
            }

            // Get customer name
            String customerName =
                    customerNameField.getText().trim();

            // Validate customer name
            if (customerName.isEmpty()
                    || customerName.length() > 100
                    || customerName.contains(":")
                    || customerName.contains("\n")
                    || customerName.contains("\r")) {

                bookingStatusLabel.setText(
                        "Please enter a valid customer name."
                );

                return;
            }

            // Check server connection
            if (serverConnection == null
                    || !serverConnection.isConnected()) {

                bookingStatusLabel.setText(
                        "Server is not connected."
                );

                return;
            }

            bookingStatusLabel.setText(
                    "Booking " + selectedSeat + "..."
            );

            // Send seat and customer name to server
            serverConnection.send(
                    "BOOK:"
                            + selectedSeat
                            + ":"
                            + customerName
            );

        });

        // -------------------------------------
        // Back Button
        // -------------------------------------

        Button backButton = new Button("Back");

        backButton.setOnAction(event -> {
            showHomeScreen();
        });

        // -------------------------------------
        // Layout
        // -------------------------------------

        VBox layout = new VBox(
                18,
                title,
                venue,
                date,
                stageLabel,
                seatGrid,
                selectedSeatLabel,
                customerNameLabel,
                customerNameField,
                bookingStatusLabel,
                bookButton,
                backButton
        );

        layout.setAlignment(Pos.CENTER);

        layout.setPadding(
                new Insets(30)
        );

        Scene scene = new Scene(
                layout,
                700,
                650
        );

        stage.setScene(scene);

        // Request latest seat availability
        if (serverConnection != null
                && serverConnection.isConnected()) {

            serverConnection.send(
                    "GET_SEATS"
            );
        }
    }

    // -----------------------------------------
    // Create Seat Buttons
    // -----------------------------------------

    private Button createSeatButton(
            String seatNumber
    ) {

        Button button = new Button(
                seatNumber
        );

        button.setPrefSize(
                70,
                50
        );

        setSeatAvailable(button);

        button.setOnAction(event -> {

            selectedSeat = seatNumber;

            selectedSeatLabel.setText(
                    "Selected Seat: "
                            + selectedSeat
            );

            bookingStatusLabel.setText("");

        });

        seatButtons.put(
                seatNumber,
                button
        );

        return button;
    }

    // -----------------------------------------
    // Handle Messages From Server
    // -----------------------------------------

    private void handleServerMessage(
            String message
    ) {

        System.out.println(
                "Server: " + message
        );

        // Booking successful
        if (message.startsWith(
                "BOOKING_SUCCESS:"
        )) {

            String seat = message.substring(
                    "BOOKING_SUCCESS:".length()
            );

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Booking successful: " + seat
                );
            }

        }

        // Already booked
        else if (message.startsWith(
                "ALREADY_BOOKED:"
        )) {

            String seat = message.substring(
                    "ALREADY_BOOKED:".length()
            );

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Seat " + seat
                                + " is already booked."
                );
            }

            updateSeat(
                    seat,
                    true
            );

        }

        // Invalid customer name
        else if (message.equals(
                "INVALID_CUSTOMER_NAME"
        )) {

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Please enter a valid customer name."
                );
            }

        }

        // Seat not found
        else if (message.equals(
                "SEAT_NOT_FOUND"
        )) {

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Seat not found."
                );
            }

        }

        // Database error
        else if (message.equals(
                "DATABASE_ERROR"
        )) {

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Booking failed due to a database error."
                );
            }

        }

        // General booking failure
        else if (message.startsWith(
                "BOOKING_FAILED:"
        )) {

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Booking failed. Please try again."
                );
            }

        }

        // Real-time seat updates
        else if (message.startsWith(
                "SEAT_UPDATE:"
        )) {

            String[] parts = message.split(":");

            if (parts.length == 3) {

                String seatNumber = parts[1];

                String status = parts[2];

                if (status.equalsIgnoreCase(
                        "BOOKED"
                )) {

                    updateSeat(
                            seatNumber,
                            true
                    );
                }
            }

        }

        // Connection error
        else if (message.startsWith(
                "CONNECTION_ERROR:"
        )) {

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Connection lost."
                );
            }

        }

        // Seat status list
        else if (message.contains("=")) {

            updateAllSeats(message);

        }
    }

    // -----------------------------------------
    // Update All Seats
    // -----------------------------------------

    private void updateAllSeats(
            String seatData
    ) {

        String[] seats = seatData.split(",");

        for (String seatInfo : seats) {

            if (seatInfo.isBlank()) {
                continue;
            }

            String[] parts = seatInfo.split("=");

            if (parts.length != 2) {
                continue;
            }

            String seatNumber = parts[0].trim();

            String status = parts[1].trim();

            boolean booked =
                    status.equalsIgnoreCase(
                            "BOOKED"
                    );

            updateSeat(
                    seatNumber,
                    booked
            );
        }
    }

    // -----------------------------------------
    // Update Single Seat
    // -----------------------------------------

    private void updateSeat(
            String seatNumber,
            boolean booked
    ) {

        Button button = seatButtons.get(
                seatNumber
        );

        if (button == null) {
            return;
        }

        if (booked) {

            button.setDisable(true);

            button.setStyle(
                    "-fx-background-color: lightcoral;" +
                            "-fx-font-weight: bold;"
            );

            if (seatNumber.equals(
                    selectedSeat
            )) {

                selectedSeat = null;

                if (selectedSeatLabel != null) {

                    selectedSeatLabel.setText(
                            "Selected Seat: None"
                    );
                }
            }

        } else {

            setSeatAvailable(button);

        }
    }

    // -----------------------------------------
    // Set Available Seat Style
    // -----------------------------------------

    private void setSeatAvailable(
            Button button
    ) {

        button.setDisable(false);

        button.setStyle(
                "-fx-background-color: lightgreen;" +
                        "-fx-font-weight: bold;"
        );
    }

    // -----------------------------------------
    // Main Method
    // -----------------------------------------

    public static void main(String[] args) {

        launch(args);

    }
}