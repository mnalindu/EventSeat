package com.eventseat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class ServerConnection {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    private Thread listenerThread;

    private volatile boolean running = false;

    private final Consumer<String> messageHandler;

    public ServerConnection(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void connect(String host, int port) throws IOException {

        socket = new Socket(host, port);

        reader = new BufferedReader(
                new InputStreamReader(
                        socket.getInputStream()
                )
        );

        writer = new PrintWriter(
                socket.getOutputStream(),
                true
        );

        running = true;

        listenerThread = new Thread(() -> {

            try {

                String message;

                while (running &&
                        (message = reader.readLine()) != null) {

                    messageHandler.accept(message);
                }

            } catch (IOException e) {

                if (running) {
                    messageHandler.accept(
                            "CONNECTION_ERROR:" + e.getMessage()
                    );
                }
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    public void send(String message) {

        if (writer != null) {
            writer.println(message);
        }
    }

    public boolean isConnected() {

        return socket != null
                && socket.isConnected()
                && !socket.isClosed();
    }

    public void close() {

        running = false;

        try {

            if (writer != null) {
                writer.println("EXIT");
            }

            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
