
package com.eventseat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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

    private TextArea bookingLookupResultArea;

    private String selectedSeat = null;
    private String pendingCustomerName = "";

    private boolean showingBookingLookup = false;

    private final Map<String, Button> seatButtons =
            new HashMap<>();

    // -----------------------------------------
    // APPLICATION START
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
    // CONNECT TO TCP SERVER
    // -----------------------------------------

    private void connectToServer() {

        serverConnection =
                new ServerConnection(message -> {

                    Platform.runLater(() -> {
                        handleServerMessage(message);
                    });
                });

        try {

            serverConnection.connect("localhost", 5000);

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
    // HOME SCREEN
    // -----------------------------------------

    private void showHomeScreen() {

        showingBookingLookup = false;
        bookingLookupResultArea = null;

        Label title = new Label("EventSeat");

        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;"
        );

        Label subtitle = new Label(
                "Real-Time Event Seat Booking System"
        );

        Label connectionLabel = new Label();

        if (serverConnection != null &&
                serverConnection.isConnected()) {

            connectionLabel.setText("Server: Connected");

            connectionLabel.setStyle(
                    "-fx-text-fill: green;"
            );

        } else {

            connectionLabel.setText("Server: Not Connected");

            connectionLabel.setStyle(
                    "-fx-text-fill: red;"
            );
        }

        Button viewEventsButton =
                new Button("View Events");

        viewEventsButton.setPrefWidth(180);

        viewEventsButton.setOnAction(event -> {
            showEventScreen();
        });

        Button findBookingButton =
                new Button("Find My Booking");

        findBookingButton.setPrefWidth(180);

        findBookingButton.setOnAction(event -> {
            showBookingLookupScreen();
        });

        VBox layout = new VBox(
                20,
                title,
                subtitle,
                connectionLabel,
                viewEventsButton,
                findBookingButton
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        stage.setScene(
                new Scene(layout, 700, 500)
        );
    }

    // -----------------------------------------
    // EVENT SCREEN
    // -----------------------------------------

    private void showEventScreen() {

        showingBookingLookup = false;
        bookingLookupResultArea = null;

        selectedSeat = null;
        seatButtons.clear();

        Label title =
                new Label("Music Night 2026");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        Label venue =
                new Label("Main Auditorium");

        Label date =
                new Label("20 November 2026");

        Label stageLabel =
                new Label("STAGE");

        stageLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px 60px;" +
                        "-fx-border-color: black;"
        );

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

        Label legend = new Label(
                "Green = Available     Red = Booked"
        );

        selectedSeatLabel =
                new Label("Selected Seat: None");

        Label customerNameLabel =
                new Label("Customer Name:");

        TextField customerNameField =
                new TextField();

        customerNameField.setPromptText(
                "Enter your full name"
        );

        customerNameField.setMaxWidth(250);

        bookingStatusLabel = new Label("");

        Button bookButton =
                new Button("Book Selected Seat");

        bookButton.setOnAction(event -> {

            if (selectedSeat == null) {

                bookingStatusLabel.setText(
                        "Please select a seat first."
                );

                return;
            }

            String customerName =
                    customerNameField.getText().trim();

            if (customerName.isEmpty() ||
                    customerName.length() > 100 ||
                    customerName.contains(":")) {

                bookingStatusLabel.setText(
                        "Enter a valid customer name."
                );

                return;
            }

            if (serverConnection == null ||
                    !serverConnection.isConnected()) {

                bookingStatusLabel.setText(
                        "Server is not connected."
                );

                return;
            }

            bookingStatusLabel.setText(
                    "Booking " + selectedSeat + "..."
            );

            pendingCustomerName = customerName;

            serverConnection.send(
                    "BOOK:" + selectedSeat + ":" + customerName
            );
        });

        Button backButton = new Button("Back");

        backButton.setOnAction(event -> {
            showHomeScreen();
        });

        VBox layout = new VBox(
                15,
                title,
                venue,
                date,
                stageLabel,
                seatGrid,
                legend,
                selectedSeatLabel,
                customerNameLabel,
                customerNameField,
                bookingStatusLabel,
                bookButton,
                backButton
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));

        stage.setScene(
                new Scene(layout, 700, 650)
        );

        // Load latest seat status
        if (serverConnection != null &&
                serverConnection.isConnected()) {

            serverConnection.send("GET_SEATS");
        }
    }

    // -----------------------------------------
    // CREATE SEAT BUTTON
    // -----------------------------------------

    private Button createSeatButton(
            String seatNumber
    ) {

        Button button = new Button(seatNumber);

        button.setPrefSize(70, 50);

        setSeatAvailable(button);

        button.setOnAction(event -> {

            selectedSeat = seatNumber;

            selectedSeatLabel.setText(
                    "Selected Seat: " + selectedSeat
            );

            bookingStatusLabel.setText("");
        });

        seatButtons.put(seatNumber, button);

        return button;
    }

    // -----------------------------------------
    // FIND MY BOOKING SCREEN
    // -----------------------------------------

    private void showBookingLookupScreen() {

        showingBookingLookup = true;

        Label title = new Label("Find My Booking");

        title.setStyle(
                "-fx-font-size: 24px;" +
                        "-fx-font-weight: bold;"
        );

        Label bookingIdLabel =
                new Label("Booking ID:");

        TextField bookingIdField = new TextField();

        bookingIdField.setPromptText(
                "Enter Booking ID"
        );

        bookingIdField.setMaxWidth(250);

        Label customerNameLabel =
                new Label("Customer Name:");

        TextField customerNameField =
                new TextField();

        customerNameField.setPromptText(
                "Enter Customer Name"
        );

        customerNameField.setMaxWidth(250);

        bookingLookupResultArea = new TextArea();

        bookingLookupResultArea.setEditable(false);
        bookingLookupResultArea.setWrapText(true);
        bookingLookupResultArea.setMaxWidth(420);
        bookingLookupResultArea.setPrefRowCount(9);

        // ----------------------------------
        // FIND BOOKING BUTTON
        // ----------------------------------

        Button findButton =
                new Button("Find Booking");

        findButton.setOnAction(event -> {

            String bookingId =
                    bookingIdField.getText().trim();

            String customerName =
                    customerNameField.getText().trim();

            if (!isValidBookingInput(
                    bookingId, customerName)) {

                bookingLookupResultArea.setText(
                        "Please enter a valid Booking ID " +
                                "and Customer Name."
                );

                return;
            }

            if (serverConnection == null ||
                    !serverConnection.isConnected()) {

                bookingLookupResultArea.setText(
                        "Server is not connected."
                );

                return;
            }

            bookingLookupResultArea.setText(
                    "Searching for booking..."
            );

            serverConnection.send(
                    "GET_BOOKING:"
                            + bookingId
                            + ":"
                            + customerName
            );
        });

        // ----------------------------------
        // CANCEL BOOKING BUTTON
        // ----------------------------------

        Button cancelButton =
                new Button("Cancel Booking");

        cancelButton.setStyle(
                "-fx-background-color: lightcoral;" +
                        "-fx-font-weight: bold;"
        );

        cancelButton.setOnAction(event -> {

            String bookingId =
                    bookingIdField.getText().trim();

            String customerName =
                    customerNameField.getText().trim();

            if (!isValidBookingInput(
                    bookingId, customerName)) {

                bookingLookupResultArea.setText(
                        "Please enter a valid Booking ID " +
                                "and Customer Name."
                );

                return;
            }

            if (serverConnection == null ||
                    !serverConnection.isConnected()) {

                bookingLookupResultArea.setText(
                        "Server is not connected."
                );

                return;
            }

            Alert confirmation = new Alert(
                    Alert.AlertType.CONFIRMATION
            );

            confirmation.setTitle("Cancel Booking");

            confirmation.setHeaderText(
                    "Are you sure you want to cancel this booking?"
            );

            confirmation.setContentText(
                    "Booking ID: #" + bookingId
            );

            boolean confirmed =
                    confirmation.showAndWait()
                            .filter(ButtonType.OK::equals)
                            .isPresent();

            if (!confirmed) {
                return;
            }

            bookingLookupResultArea.setText(
                    "Cancelling booking..."
            );

            serverConnection.send(
                    "CANCEL_BOOKING:"
                            + bookingId
                            + ":"
                            + customerName
            );
        });

        // ----------------------------------
        // BACK BUTTON
        // ----------------------------------

        Button backButton = new Button("Back");

        backButton.setOnAction(event -> {
            showHomeScreen();
        });

        VBox layout = new VBox(
                15,
                title,
                bookingIdLabel,
                bookingIdField,
                customerNameLabel,
                customerNameField,
                findButton,
                cancelButton,
                bookingLookupResultArea,
                backButton
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));

        stage.setScene(
                new Scene(layout, 700, 600)
        );
    }

    // -----------------------------------------
    // VALIDATE BOOKING LOOKUP INPUT
    // -----------------------------------------

    private boolean isValidBookingInput(
            String bookingId,
            String customerName
    ) {

        return bookingId.matches("[1-9][0-9]*")
                && !customerName.isEmpty()
                && customerName.length() <= 100
                && !customerName.contains(":");
    }

    // -----------------------------------------
    // HANDLE SERVER MESSAGES
    // -----------------------------------------

    private void handleServerMessage(
            String message
    ) {

        System.out.println("Server: " + message);

        // ----------------------------------
        // BOOKING SUCCESS
        // ----------------------------------

        if (message.startsWith("BOOKING_SUCCESS:")) {

            String[] parts = message.split(":", 3);

            if (parts.length == 3) {

                String seatNumber = parts[1];
                String bookingId = parts[2];

                if (bookingStatusLabel != null) {

                    bookingStatusLabel.setText(
                            "Booking successful: "
                                    + seatNumber
                    );
                }

                updateSeat(seatNumber, true);

                showBookingConfirmation(
                        bookingId,
                        seatNumber,
                        pendingCustomerName
                );
            }
        }

        // ----------------------------------
        // SEAT ALREADY BOOKED
        // ----------------------------------

        else if (message.startsWith("ALREADY_BOOKED:")) {

            String seatNumber =
                    message.substring(
                            "ALREADY_BOOKED:".length()
                    );

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Seat " + seatNumber +
                                " is already booked."
                );
            }

            updateSeat(seatNumber, true);
        }

        // ----------------------------------
        // SEAT NOT FOUND
        // ----------------------------------

        else if (message.equals("SEAT_NOT_FOUND")) {

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Seat not found."
                );
            }
        }

        // ----------------------------------
        // INVALID CUSTOMER NAME
        // ----------------------------------

        else if (message.equals("INVALID_CUSTOMER_NAME")) {

            if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Invalid customer name."
                );
            }
        }

        // ----------------------------------
        // REAL-TIME SEAT UPDATE
        // ----------------------------------

        else if (message.startsWith("SEAT_UPDATE:")) {

            String[] parts = message.split(":");

            if (parts.length == 3) {

                String seatNumber = parts[1];
                String status = parts[2];

                if (status.equalsIgnoreCase("BOOKED")) {

                    updateSeat(seatNumber, true);

                } else if (
                        status.equalsIgnoreCase("AVAILABLE")
                ) {

                    updateSeat(seatNumber, false);
                }
            }
        }

        // ----------------------------------
        // CANCELLATION SUCCESS
        // ----------------------------------

        else if (message.startsWith("CANCEL_SUCCESS:")) {

            String[] parts = message.split(":", 3);

            if (parts.length == 3) {

                String bookingId = parts[1];
                String seatNumber = parts[2];

                if (bookingLookupResultArea != null &&
                        showingBookingLookup) {

                    bookingLookupResultArea.setText(
                            "BOOKING CANCELLED SUCCESSFULLY!\n\n" +
                                    "Booking ID: #" + bookingId + "\n" +
                                    "Seat: " + seatNumber + "\n" +
                                    "Status: CANCELLED\n\n" +
                                    "The seat is now available for booking."
                    );
                }

                updateSeat(seatNumber, false);
            }
        }

        // ----------------------------------
        // BOOKING ALREADY CANCELLED
        // ----------------------------------

        else if (message.startsWith("ALREADY_CANCELLED:")) {

            if (bookingLookupResultArea != null &&
                    showingBookingLookup) {

                bookingLookupResultArea.setText(
                        "This booking has already been cancelled."
                );
            }
        }

        // ----------------------------------
        // INVALID CANCELLATION REQUEST
        // ----------------------------------

        else if (message.equals("INVALID_CANCEL_REQUEST")) {

            if (bookingLookupResultArea != null &&
                    showingBookingLookup) {

                bookingLookupResultArea.setText(
                        "Invalid cancellation request."
                );
            }
        }

        // ----------------------------------
        // CANCELLATION FAILED
        // ----------------------------------

        else if (message.equals("CANCEL_FAILED")) {

            if (bookingLookupResultArea != null &&
                    showingBookingLookup) {

                bookingLookupResultArea.setText(
                        "Cancellation failed. Please try again."
                );
            }
        }

        // ----------------------------------
        // BOOKING DETAILS
        // ----------------------------------

        else if (message.startsWith("BOOKING_DETAILS:")) {

            String[] parts = message.split(":", 9);

            if (parts.length == 9 &&
                    showingBookingLookup &&
                    bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        "BOOKING FOUND\n\n" +
                                "Booking ID: #" + parts[1] + "\n" +
                                "Seat: " + parts[2] + "\n" +
                                "Customer: " + parts[3] + "\n" +
                                "Event: " + parts[4] + "\n" +
                                "Venue: " + parts[5] + "\n" +
                                "Event Date: " + parts[6] + "\n" +
                                "Status: " + parts[7] + "\n" +
                                "Booking Time: " + parts[8]
                );
            }
        }

        // ----------------------------------
        // BOOKING NOT FOUND
        // ----------------------------------

        else if (message.equals("BOOKING_NOT_FOUND")) {

            if (showingBookingLookup &&
                    bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        "Booking not found.\n" +
                                "Please check your Booking ID " +
                                "and Customer Name."
                );
            }
        }

        // ----------------------------------
        // INVALID BOOKING REQUEST
        // ----------------------------------

        else if (message.equals("INVALID_BOOKING_REQUEST")) {

            if (showingBookingLookup &&
                    bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        "Invalid booking request."
                );
            }
        }

        // ----------------------------------
        // DATABASE ERROR
        // ----------------------------------

        else if (message.equals("DATABASE_ERROR")) {

            if (showingBookingLookup &&
                    bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        "Database error. Please try again."
                );

            } else if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Database error. Please try again."
                );
            }
        }

        // ----------------------------------
        // CONNECTION ERROR
        // ----------------------------------

        else if (message.startsWith("CONNECTION_ERROR:")) {

            if (showingBookingLookup &&
                    bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        "Connection lost."
                );

            } else if (bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Connection lost."
                );
            }
        }

        // ----------------------------------
        // INITIAL SEAT STATUS
        // ----------------------------------

        else if (message.contains("=")) {

            updateAllSeats(message);
        }
    }

    // -----------------------------------------
    // UPDATE ALL SEATS
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

            String seatNumber = parts[0];
            String status = parts[1];

            boolean booked =
                    status.equalsIgnoreCase("BOOKED");

            updateSeat(seatNumber, booked);
        }
    }

    // -----------------------------------------
    // UPDATE ONE SEAT
    // -----------------------------------------

    private void updateSeat(
            String seatNumber,
            boolean booked
    ) {

        Button button = seatButtons.get(seatNumber);

        if (button == null) {
            return;
        }

        if (booked) {

            button.setDisable(true);

            button.setStyle(
                    "-fx-background-color: lightcoral;" +
                            "-fx-font-weight: bold;"
            );

            if (seatNumber.equals(selectedSeat)) {

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
    // SET SEAT AVAILABLE
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
    // BOOKING CONFIRMATION POPUP
    // -----------------------------------------

    private void showBookingConfirmation(
            String bookingId,
            String seatNumber,
            String customerName
    ) {

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle(
                "EventSeat - Booking Confirmation"
        );

        alert.setHeaderText(
                "Booking Confirmed Successfully!"
        );

        String ticketDetails =
                "Booking ID: #" + bookingId + "\n\n" +
                        "Customer: " + customerName + "\n" +
                        "Event: Music Night 2026\n" +
                        "Venue: Main Auditorium\n" +
                        "Date: 20 November 2026\n" +
                        "Seat: " + seatNumber + "\n\n" +
                        "Thank you for booking with EventSeat!";

        alert.setContentText(ticketDetails);

        alert.showAndWait();
    }

    // -----------------------------------------
    // MAIN METHOD
    // -----------------------------------------

    public static void main(String[] args) {

        launch(args);
    }
}
