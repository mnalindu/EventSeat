package com.eventseat.client;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class EventSeatApp extends Application {

    @Override
    public void start(Stage stage) {

        Label title = new Label("EventSeat");

        Label subtitle =
                new Label("Real-Time Event Seat Booking System");

        Button button =
                new Button("View Events");

        VBox layout = new VBox(
                20,
                title,
                subtitle,
                button
        );

        layout.setAlignment(Pos.CENTER);

        Scene scene =
                new Scene(layout, 600, 400);

        stage.setTitle("EventSeat");
        stage.setScene(scene);

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
