package com.eventseat.model;

public class ModelTestMain {
    public static void main(String[] args) {

        Event event = new Event(
                "E001",
                "Music Night 2026",
                "Main Auditorium",
                "2026-11-20"
        );

        event.addSeat(new Seat("A1"));
        event.addSeat(new Seat("A2"));
        event.addSeat(new Seat("A3"));
        event.addSeat(new Seat("A4"));

        System.out.println("Event:");
        System.out.println(event);

        System.out.println("\nSeats:");

        for (Seat seat : event.getSeats()) {
            System.out.println(seat);
        }

        Seat selectedSeat = event.findSeat("A2");

        if (selectedSeat != null) {
            selectedSeat.setBooked(true);
        }

        System.out.println("\nAfter booking A2:");

        for (Seat seat : event.getSeats()) {
            System.out.println(seat);
        }
    }

}
