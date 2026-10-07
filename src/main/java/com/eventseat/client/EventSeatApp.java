package com.eventseat.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EventSeatApp extends Application {

    private Stage stage;
    private Label selectedSeatLabel;
    private String selectedSeat = null;

    @Override
    public void start(Stage stage) {

        this.stage = stage;

        showHomeScreen();

        stage.setTitle("EventSeat");
        stage.show();
    }

    private void showHomeScreen() {

        Label title = new Label("EventSeat");
        title.setStyle(
                "-fx-font-size: 28px;" +
                        "-fx-font-weight: bold;"
        );

        Label subtitle =
                new Label("Real-Time Event Seat Booking System");

        Button viewEventsButton =
                new Button("View Events");

        viewEventsButton.setOnAction(event -> {
            showEventScreen();
        });

        VBox layout = new VBox(
                20,
                title,
                subtitle,
                viewEventsButton
        );

        layout.setAlignment(Pos.CENTER);

        Scene scene =
                new Scene(layout, 700, 500);

        stage.setScene(scene);
    }

    private void showEventScreen() {

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
                        "-fx-padding: 10px;" +
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

        selectedSeatLabel =
                new Label("Selected Seat: None");

        Button bookButton =
                new Button("Book Selected Seat");

        bookButton.setOnAction(event -> {

            if (selectedSeat == null) {

                selectedSeatLabel.setText(
                        "Please select a seat first."
                );

            } else {

                selectedSeatLabel.setText(
                        "Selected Seat: "
                                + selectedSeat
                                + " (Ready to book)"
                );
            }
        });

        Button backButton =
                new Button("Back");

        backButton.setOnAction(event -> {
            showHomeScreen();
        });

        VBox layout = new VBox(
                18,
                title,
                venue,
                date,
                stageLabel,
                seatGrid,
                selectedSeatLabel,
                bookButton,
                backButton
        );

        layout.setAlignment(Pos.CENTER);
        layout.setPadding(
                new Insets(30)
        );

        Scene scene =
                new Scene(layout, 700, 550);

        stage.setScene(scene);
    }

    private Button createSeatButton(
            String seatNumber
    ) {

        Button button =
                new Button(seatNumber);

        button.setPrefSize(
                70,
                50
        );

        button.setStyle(
                "-fx-background-color: lightgreen;" +
                        "-fx-font-weight: bold;"
        );

        button.setOnAction(event -> {

            selectedSeat = seatNumber;

            selectedSeatLabel.setText(
                    "Selected Seat: "
                            + selectedSeat
            );
        });

        return button;
    }

    public static void main(String[] args) {
        launch(args);
    }
}