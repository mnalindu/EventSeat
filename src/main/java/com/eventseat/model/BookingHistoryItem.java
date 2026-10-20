
package com.eventseat.model;

public class BookingHistoryItem {

    private final int bookingId;
    private final String eventId;
    private final String seatNumber;
    private final String customerName;
    private final String bookingTime;

    public BookingHistoryItem(
            int bookingId,
            String eventId,
            String seatNumber,
            String customerName,
            String bookingTime
    ) {

        this.bookingId = bookingId;
        this.eventId = eventId;
        this.seatNumber = seatNumber;
        this.customerName = customerName;
        this.bookingTime = bookingTime;
    }

    public int getBookingId() {
        return bookingId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getBookingTime() {
        return bookingTime;
    }
}
