package com.eventseat.server;

public class ConcurrencyTestMain {

    public static void main(String[] args) {

        EventManager eventManager = new EventManager();

        Runnable bookingTask = () -> {

            String result = eventManager.bookSeat("A1");

            System.out.println(
                    Thread.currentThread().getName()
                            + " -> "
                            + result
            );
        };

        Thread client1 = new Thread(bookingTask, "Client-1");
        Thread client2 = new Thread(bookingTask, "Client-2");
        Thread client3 = new Thread(bookingTask, "Client-3");

        client1.start();
        client2.start();
        client3.start();
    }
}