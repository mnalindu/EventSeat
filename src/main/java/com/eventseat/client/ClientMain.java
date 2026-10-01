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
            System.out.println("Connecting to EventSeat server...");

            Socket socket = new Socket(serverAddress, port);

            System.out.println("Connected to server successfully!");
            System.out.println("Type EXIT to disconnect.");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(),
                    true
            );

            Scanner scanner = new Scanner(System.in);

            while (true) {

                System.out.print("Enter message: ");
                String message = scanner.nextLine();

                writer.println(message);

                String reply = reader.readLine();

                System.out.println("Server says: " + reply);

                if (message.equalsIgnoreCase("EXIT")) {
                    break;
                }
            }

            scanner.close();
            reader.close();
            writer.close();
            socket.close();

            System.out.println("Disconnected from server.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
