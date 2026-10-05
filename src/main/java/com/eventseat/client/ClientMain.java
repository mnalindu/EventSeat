package com.eventseat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) {

        String serverAddress = "localhost";
        int port = 5000;

        try {

            System.out.println(
                    "Connecting to EventSeat server..."
            );

            Socket socket =
                    new Socket(serverAddress, port);

            System.out.println(
                    "Connected to server successfully!"
            );

            System.out.println(
                    "Commands:"
            );

            System.out.println("GET_EVENT");
            System.out.println("GET_SEATS");
            System.out.println("BOOK:A1");
            System.out.println("EXIT");

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            PrintWriter writer =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            /*
             * Separate thread continuously
             * listens for server messages.
             */
            Thread listenerThread = new Thread(() -> {

                try {

                    String serverMessage;

                    while (
                            (serverMessage =
                                    reader.readLine()) != null
                    ) {

                        System.out.println(
                                "\nServer says: "
                                        + serverMessage
                        );
                        System.out.print(
                                "Enter command: "
                        );

                    }

                } catch (IOException e) {

                    System.out.println(
                            "Disconnected from server."
                    );
                }
            });

            listenerThread.start();

            Scanner scanner =
                    new Scanner(System.in);

            while (true) {

                System.out.print(
                        "Enter command: "
                );

                String message =
                        scanner.nextLine();

                writer.println(message);

                if (
                        message.equalsIgnoreCase("EXIT")
                ) {
                    break;
                }
            }

            scanner.close();

            // Give the listener a moment to receive Goodbye
            try {
                listenerThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            socket.close();

        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}