
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

    private static class EventInfo {
        final String id, name, venue, date;

        EventInfo(String id, String name,
                  String venue, String date) {
            this.id = id;
            this.name = name;
            this.venue = venue;
            this.date = date;
        }
    }

    private Stage stage;
    private ServerConnection serverConnection;
    private String currentScreen = "HOME";

    private VBox eventsContainer;
    private GridPane seatGrid;
    private EventInfo selectedEvent;
    private String selectedSeat;
    private Label selectedSeatLabel;
    private Label bookingStatusLabel;
    private Button bookButton;

    private final Map<String, Button> seatButtons =
            new HashMap<>();

    private boolean bookingPending = false;
    private String pendingCustomerName = "";
    private EventInfo pendingEvent;

    private TextArea bookingLookupResultArea;
    private Button cancelButton;

    private String requestedBookingId = "";
    private String requestedCustomerName = "";
    private String verifiedBookingId = "";
    private String verifiedCustomerName = "";

    // -----------------------------------------
    // APPLICATION START
    // -----------------------------------------

    @Override
    public void start(Stage primaryStage) {

        stage = primaryStage;

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
    // SERVER CONNECTION
    // -----------------------------------------

    private void connectToServer() {

        serverConnection = new ServerConnection(
                message -> Platform.runLater(
                        () -> handleServerMessage(message)
                )
        );

        try {

            serverConnection.connect("localhost", 5000);

            System.out.println(
                    "Connected to EventSeat server."
            );

        } catch (IOException e) {

            System.out.println(
                    "Could not connect to server: "
                            + e.getMessage()
            );
        }
    }

    private boolean isConnected() {

        return serverConnection != null &&
                serverConnection.isConnected();
    }

    // -----------------------------------------
    // HOME SCREEN
    // -----------------------------------------

    private void showHomeScreen() {

        currentScreen = "HOME";
        selectedEvent = null;
        bookingLookupResultArea = null;
        seatButtons.clear();

        Label title = new Label("EventSeat");

        title.setStyle(
                "-fx-font-size: 28px; -fx-font-weight: bold;"
        );

        Label subtitle = new Label(
                "Real-Time Event Seat Booking System"
        );

        Label connection = new Label(
                isConnected()
                        ? "Server: Connected"
                        : "Server: Not Connected"
        );

        connection.setStyle(
                isConnected()
                        ? "-fx-text-fill: green;"
                        : "-fx-text-fill: red;"
        );

        Button viewEvents = new Button("View Events");
        viewEvents.setPrefWidth(190);

        viewEvents.setOnAction(event ->
                showEventsListScreen()
        );

        Button findBooking = new Button("Find My Booking");
        findBooking.setPrefWidth(190);

        findBooking.setOnAction(event ->
                showBookingLookupScreen()
        );

        VBox layout = new VBox(
                20,
                title,
                subtitle,
                connection,
                viewEvents,
                findBooking
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(30));

        stage.setScene(new Scene(layout, 720, 540));
    }

    // -----------------------------------------
    // AVAILABLE EVENTS SCREEN
    // -----------------------------------------

    private void showEventsListScreen() {

        currentScreen = "EVENTS";
        selectedEvent = null;
        bookingLookupResultArea = null;
        seatButtons.clear();

        Label title = new Label("Available Events");

        title.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold;"
        );

        eventsContainer = new VBox(12);
        eventsContainer.setAlignment(Pos.CENTER);

        eventsContainer.getChildren().add(
                new Label("Loading events...")
        );

        Button back = new Button("Back to Home");

        back.setOnAction(event -> showHomeScreen());

        VBox layout = new VBox(
                25,
                title,
                eventsContainer,
                back
        );

        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(35));

        stage.setScene(new Scene(layout, 720, 570));

        if (isConnected()) {

            serverConnection.send("GET_EVENTS");

        } else {

            eventsContainer.getChildren().setAll(
                    new Label("Server is not connected.")
            );
        }
    }

    // -----------------------------------------
    // DISPLAY ALL EVENTS
    // -----------------------------------------

    private void displayEvents(String data) {

        if (!"EVENTS".equals(currentScreen)
                || eventsContainer == null) {
            return;
        }

        eventsContainer.getChildren().clear();

        int count = 0;

        for (String row : data.split(";")) {

            if (row.isBlank()) {
                continue;
            }

            String[] parts = row.split("\\|", 4);

            if (parts.length != 4) {
                continue;
            }

            EventInfo event = new EventInfo(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3]
            );

            Button button = new Button(
                    "View Seats - " + event.id
            );

            button.setOnAction(action ->
                    showEventScreen(event)
            );

            VBox card = new VBox(
                    6,
                    new Label(event.name),
                    new Label("Venue: " + event.venue),
                    new Label("Date: " + event.date),
                    button
            );

            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(12));

            card.setStyle(
                    "-fx-background-color: #f3f5f7;" +
                            "-fx-background-radius: 10;"
            );

            card.setMaxWidth(390);

            eventsContainer.getChildren().add(card);

            count++;
        }

        if (count == 0) {

            eventsContainer.getChildren().add(
                    new Label("No events found.")
            );
        }
    }

    // -----------------------------------------
    // SELECTED EVENT SCREEN
    // -----------------------------------------

    private void showEventScreen(EventInfo event) {

        currentScreen = "SEATS";
        selectedEvent = event;
        selectedSeat = null;

        seatButtons.clear();
        bookingLookupResultArea = null;

        Label title = new Label(event.name);

        title.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold;"
        );

        Label venue = new Label(
                "Venue: " + event.venue
        );

        Label date = new Label(
                "Date: " + event.date
        );

        Label stageLabel = new Label("STAGE");

        stageLabel.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 10px 60px;" +
                        "-fx-border-color: black;"
        );

        seatGrid = new GridPane();

        seatGrid.setHgap(15);
        seatGrid.setVgap(15);
        seatGrid.setAlignment(Pos.CENTER);

        seatGrid.add(
                new Label("Loading seats..."),
                0,
                0
        );

        selectedSeatLabel = new Label(
                "Selected Seat: None"
        );

        bookingStatusLabel = new Label("");

        TextField nameField = new TextField();

        nameField.setPromptText(
                "Enter your full name"
        );

        nameField.setMaxWidth(260);

        bookButton = new Button(
                "Book Selected Seat"
        );

        bookButton.setDisable(bookingPending);

        bookButton.setOnAction(action -> {

            if (bookingPending) {
                return;
            }

            if (selectedSeat == null) {

                bookingStatusLabel.setText(
                        "Please select a seat first."
                );

                return;
            }

            String customerName =
                    nameField.getText().trim();

            if (customerName.isEmpty()
                    || customerName.length() > 100
                    || customerName.contains(":")) {

                bookingStatusLabel.setText(
                        "Enter a valid customer name."
                );

                return;
            }

            if (!isConnected()) {

                bookingStatusLabel.setText(
                        "Server is not connected."
                );

                return;
            }

            bookingPending = true;
            bookButton.setDisable(true);

            pendingCustomerName = customerName;
            pendingEvent = selectedEvent;

            bookingStatusLabel.setText(
                    "Booking " + selectedSeat + "..."
            );

            serverConnection.send(
                    "BOOK:"
                            + selectedEvent.id + ":"
                            + selectedSeat + ":"
                            + customerName
            );
        });

        Button back = new Button("Back to Events");

        back.setOnAction(action ->
                showEventsListScreen()
        );

        VBox layout = new VBox(
                15,
                title,
                venue,
                date,
                stageLabel,
                seatGrid,
                new Label(
                        "Green = Available     Red = Booked"
                ),
                selectedSeatLabel,
                new Label("Customer Name:"),
                nameField,
                bookingStatusLabel,
                bookButton,
                back
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));

        stage.setScene(
                new Scene(layout, 720, 680)
        );

        if (isConnected()) {

            serverConnection.send(
                    "GET_SEATS:" + event.id
            );

        } else {

            bookingStatusLabel.setText(
                    "Server is not connected."
            );
        }
    }

    // -----------------------------------------
    // DISPLAY EVENT SEATS
    // -----------------------------------------

    private void displaySeats(
            String eventId,
            String data
    ) {

        if (!"SEATS".equals(currentScreen)
                || selectedEvent == null
                || !eventId.equals(selectedEvent.id)) {
            return;
        }

        seatGrid.getChildren().clear();
        seatButtons.clear();

        int index = 0;

        for (String row : data.split(",")) {

            if (row.isBlank()) {
                continue;
            }

            String[] parts = row.split("=", 2);

            if (parts.length != 2) {
                continue;
            }

            String seatNumber = parts[0];

            Button button =
                    createSeatButton(seatNumber);

            seatGrid.add(
                    button,
                    index % 4,
                    index / 4
            );

            updateSeat(
                    seatNumber,
                    "BOOKED".equalsIgnoreCase(parts[1])
            );

            index++;
        }

        if (index == 0) {

            seatGrid.add(
                    new Label(
                            "No seats available for this event."
                    ),
                    0,
                    0
            );
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

        seatButtons.put(seatNumber, button);

        setSeatAvailable(button);

        button.setOnAction(action -> {

            selectedSeat = seatNumber;

            selectedSeatLabel.setText(
                    "Selected Seat: " + seatNumber
            );

            bookingStatusLabel.setText("");
        });

        return button;
    }

    // -----------------------------------------
    // UPDATE SEAT AVAILABILITY
    // -----------------------------------------

    private void setSeatAvailable(Button button) {

        button.setDisable(false);

        button.setStyle(
                "-fx-background-color: lightgreen;" +
                        "-fx-font-weight: bold;"
        );
    }

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
    // FIND MY BOOKING SCREEN
    // -----------------------------------------

    private void showBookingLookupScreen() {

        currentScreen = "LOOKUP";

        selectedEvent = null;
        seatButtons.clear();

        verifiedBookingId = "";
        verifiedCustomerName = "";

        Label title = new Label(
                "Find My Booking"
        );

        title.setStyle(
                "-fx-font-size: 24px; -fx-font-weight: bold;"
        );

        TextField idField = new TextField();

        idField.setPromptText(
                "Enter Booking ID"
        );

        idField.setMaxWidth(250);

        TextField nameField = new TextField();

        nameField.setPromptText(
                "Enter Customer Name"
        );

        nameField.setMaxWidth(250);

        bookingLookupResultArea = new TextArea();

        bookingLookupResultArea.setEditable(false);
        bookingLookupResultArea.setWrapText(true);
        bookingLookupResultArea.setMaxWidth(440);
        bookingLookupResultArea.setPrefRowCount(9);

        cancelButton = new Button(
                "Cancel Booking"
        );

        cancelButton.setDisable(true);

        cancelButton.setStyle(
                "-fx-background-color: lightcoral;" +
                        "-fx-font-weight: bold;"
        );

        // ----------------------------------
        // FIND BOOKING
        // ----------------------------------

        Button findButton = new Button(
                "Find Booking"
        );

        findButton.setOnAction(action -> {

            String bookingId =
                    idField.getText().trim();

            String name =
                    nameField.getText().trim();

            if (!validBookingInput(
                    bookingId,
                    name
            )) {

                bookingLookupResultArea.setText(
                        "Please enter a valid Booking ID "
                                + "and Customer Name."
                );

                return;
            }

            if (!isConnected()) {

                bookingLookupResultArea.setText(
                        "Server is not connected."
                );

                return;
            }

            cancelButton.setDisable(true);
            verifiedBookingId = "";

            requestedBookingId = bookingId;
            requestedCustomerName = name;

            bookingLookupResultArea.setText(
                    "Searching for booking..."
            );

            serverConnection.send(
                    "GET_BOOKING:"
                            + bookingId + ":"
                            + name
            );
        });

        // ----------------------------------
        // CANCEL BOOKING
        // ----------------------------------

        cancelButton.setOnAction(action -> {

            String bookingId =
                    idField.getText().trim();

            String name =
                    nameField.getText().trim();

            if (!bookingId.equals(verifiedBookingId)
                    || !name.equals(verifiedCustomerName)) {

                bookingLookupResultArea.setText(
                        "Find this booking again before cancelling."
                );

                cancelButton.setDisable(true);
                return;
            }

            if (!isConnected()) {

                bookingLookupResultArea.setText(
                        "Server is not connected."
                );

                return;
            }

            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION
            );

            confirm.setTitle("Cancel Booking");

            confirm.setHeaderText(
                    "Are you sure you want to cancel booking #"
                            + bookingId + "?"
            );

            boolean accepted =
                    confirm.showAndWait()
                            .filter(ButtonType.OK::equals)
                            .isPresent();

            if (!accepted) {
                return;
            }

            cancelButton.setDisable(true);

            bookingLookupResultArea.setText(
                    "Cancelling booking..."
            );

            serverConnection.send(
                    "CANCEL_BOOKING:"
                            + bookingId + ":"
                            + name
            );
        });

        Button back = new Button(
                "Back to Home"
        );

        back.setOnAction(action ->
                showHomeScreen()
        );

        VBox layout = new VBox(
                14,
                title,
                new Label("Booking ID:"),
                idField,
                new Label("Customer Name:"),
                nameField,
                findButton,
                cancelButton,
                bookingLookupResultArea,
                back
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(25));

        stage.setScene(
                new Scene(layout, 720, 620)
        );
    }

    // -----------------------------------------
    // VALIDATE BOOKING INPUT
    // -----------------------------------------

    private boolean validBookingInput(
            String bookingId,
            String customerName
    ) {

        return bookingId.matches("[1-9][0-9]*")
                && !customerName.isBlank()
                && customerName.length() <= 100
                && !customerName.contains(":");
    }

    // -----------------------------------------
    // HANDLE SERVER MESSAGES
    // -----------------------------------------

    private void handleServerMessage(String message) {

        System.out.println("Server: " + message);

        // ----------------------------------
        // EVENT LIST
        // ----------------------------------

        if (message.startsWith("EVENTS:")) {

            displayEvents(
                    message.substring("EVENTS:".length())
            );

            // ----------------------------------
            // INITIAL SEAT DATA
            // ----------------------------------

        } else if (message.startsWith("SEATS:")) {

            String[] parts =
                    message.split(":", 3);

            if (parts.length == 3) {

                displaySeats(
                        parts[1],
                        parts[2]
                );
            }

            // ----------------------------------
            // REAL-TIME SEAT UPDATE
            // ----------------------------------

        } else if (message.startsWith("SEAT_UPDATE:")) {

            String[] parts =
                    message.split(":", 4);

            if (parts.length == 4
                    && "SEATS".equals(currentScreen)
                    && selectedEvent != null
                    && selectedEvent.id.equals(parts[1])) {

                updateSeat(
                        parts[2],
                        "BOOKED".equalsIgnoreCase(parts[3])
                );
            }

            // ----------------------------------
            // BOOKING SUCCESS
            // ----------------------------------

        } else if (message.startsWith("BOOKING_SUCCESS:")) {

            String[] parts =
                    message.split(":", 4);

            bookingPending = false;

            if (bookButton != null) {
                bookButton.setDisable(false);
            }

            if (parts.length == 4) {

                if ("SEATS".equals(currentScreen)
                        && selectedEvent != null
                        && selectedEvent.id.equals(parts[1])) {

                    updateSeat(parts[2], true);

                    bookingStatusLabel.setText(
                            "Booking successful: " + parts[2]
                    );
                }

                showBookingConfirmation(
                        parts[3],
                        parts[2],
                        pendingCustomerName,
                        pendingEvent
                );
            }

            // ----------------------------------
            // ALREADY BOOKED
            // ----------------------------------

        } else if (message.startsWith("ALREADY_BOOKED:")) {

            bookingPending = false;

            if (bookButton != null) {
                bookButton.setDisable(false);
            }

            String[] parts =
                    message.split(":", 3);

            if (parts.length == 3
                    && "SEATS".equals(currentScreen)
                    && selectedEvent != null
                    && selectedEvent.id.equals(parts[1])) {

                updateSeat(parts[2], true);

                bookingStatusLabel.setText(
                        "Seat " + parts[2] + " is already booked."
                );
            }

            // ----------------------------------
            // BOOKING FAILED
            // ----------------------------------

        } else if (message.startsWith("BOOKING_FAILED:")) {

            bookingPending = false;

            if (bookButton != null) {
                bookButton.setDisable(false);
            }

            if ("SEATS".equals(currentScreen)
                    && bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        "Booking failed. Please refresh the seat list."
                );
            }

            // ----------------------------------
            // CANCELLATION SUCCESS
            // ----------------------------------

        } else if (message.startsWith("CANCEL_SUCCESS:")) {

            String[] parts =
                    message.split(":", 4);

            if (parts.length == 4) {

                if ("LOOKUP".equals(currentScreen)
                        && bookingLookupResultArea != null) {

                    bookingLookupResultArea.setText(
                            "BOOKING CANCELLED SUCCESSFULLY!\n\n" +
                                    "Booking ID: #" + parts[1] + "\n" +
                                    "Event ID: " + parts[2] + "\n" +
                                    "Seat: " + parts[3] + "\n" +
                                    "Status: CANCELLED"
                    );

                    cancelButton.setDisable(true);
                    verifiedBookingId = "";
                }

                if ("SEATS".equals(currentScreen)
                        && selectedEvent != null
                        && selectedEvent.id.equals(parts[2])) {

                    updateSeat(parts[3], false);
                }
            }

            // ----------------------------------
            // ALREADY CANCELLED
            // ----------------------------------

        } else if (message.startsWith("ALREADY_CANCELLED:")) {

            if ("LOOKUP".equals(currentScreen)
                    && bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        "This booking is already cancelled."
                );

                cancelButton.setDisable(true);
            }

            // ----------------------------------
            // BOOKING DETAILS
            // ----------------------------------

        } else if (message.startsWith("BOOKING_DETAILS:")) {

            String[] parts =
                    message.split(":", 9);

            if (parts.length == 9
                    && "LOOKUP".equals(currentScreen)
                    && bookingLookupResultArea != null
                    && parts[1].equals(requestedBookingId)) {

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

                verifiedBookingId = parts[1];

                verifiedCustomerName =
                        requestedCustomerName;

                cancelButton.setDisable(
                        !"ACTIVE".equalsIgnoreCase(parts[7])
                );
            }

            // ----------------------------------
            // BOOKING LOOKUP ERRORS
            // ----------------------------------

        } else if (
                message.equals("BOOKING_NOT_FOUND")
                        || message.equals("INVALID_BOOKING_REQUEST")
                        || message.equals("INVALID_CANCEL_REQUEST")
                        || message.equals("CANCEL_FAILED")
        ) {

            if ("LOOKUP".equals(currentScreen)
                    && bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        message.replace('_', ' ')
                );

                cancelButton.setDisable(true);
            }

            // ----------------------------------
            // GENERAL ERRORS
            // ----------------------------------

        } else if (
                message.equals("DATABASE_ERROR")
                        || message.equals("SEAT_NOT_FOUND")
                        || message.equals("EVENT_NOT_FOUND")
                        || message.equals("INVALID_CUSTOMER_NAME")
                        || message.equals("INVALID_BOOKING_REQUEST")
                        || message.startsWith("CONNECTION_ERROR:")
        ) {

            bookingPending = false;

            if (bookButton != null) {
                bookButton.setDisable(false);
            }

            if ("LOOKUP".equals(currentScreen)
                    && bookingLookupResultArea != null) {

                bookingLookupResultArea.setText(
                        message.replace('_', ' ')
                );

            } else if ("SEATS".equals(currentScreen)
                    && bookingStatusLabel != null) {

                bookingStatusLabel.setText(
                        message.replace('_', ' ')
                );

            } else if ("EVENTS".equals(currentScreen)
                    && eventsContainer != null) {

                eventsContainer.getChildren().setAll(
                        new Label(
                                message.replace('_', ' ')
                        )
                );
            }
        }
    }

    // -----------------------------------------
    // BOOKING CONFIRMATION
    // -----------------------------------------

    private void showBookingConfirmation(
            String bookingId,
            String seatNumber,
            String customerName,
            EventInfo event
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

        alert.setContentText(
                "Booking ID: #" + bookingId + "\n\n" +
                        "Customer: " + customerName + "\n" +
                        "Event: " +
                        (event == null ? "Unknown" : event.name) + "\n" +
                        "Venue: " +
                        (event == null ? "Unknown" : event.venue) + "\n" +
                        "Date: " +
                        (event == null ? "Unknown" : event.date) + "\n" +
                        "Seat: " + seatNumber + "\n\n" +
                        "Thank you for booking with EventSeat!"
        );

        alert.showAndWait();
    }

    // -----------------------------------------
    // MAIN METHOD
    // -----------------------------------------

    public static void main(String[] args) {
        launch(args);
    }
}
