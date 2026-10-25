package com.smartcollege.demo.dto;

public class BookingTrendDto {

    private String date;
    private int totalBookings;
    private int approved;
    private int pending;
    private int rejected;
    private int cancelled;
    private int completed;
    private double bookedHours;

    public BookingTrendDto() {
    }

    public BookingTrendDto(String date, int totalBookings, int approved, int pending, int rejected, int cancelled, int completed, double bookedHours) {
        this.date = date;
        this.totalBookings = totalBookings;
        this.approved = approved;
        this.pending = pending;
        this.rejected = rejected;
        this.cancelled = cancelled;
        this.completed = completed;
        this.bookedHours = bookedHours;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(int totalBookings) {
        this.totalBookings = totalBookings;
    }

    public int getApproved() {
        return approved;
    }

    public void setApproved(int approved) {
        this.approved = approved;
    }

    public int getPending() {
        return pending;
    }

    public void setPending(int pending) {
        this.pending = pending;
    }

    public int getRejected() {
        return rejected;
    }

    public void setRejected(int rejected) {
        this.rejected = rejected;
    }

    public int getCancelled() {
        return cancelled;
    }

    public void setCancelled(int cancelled) {
        this.cancelled = cancelled;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public double getBookedHours() {
        return bookedHours;
    }

    public void setBookedHours(double bookedHours) {
        this.bookedHours = bookedHours;
    }
}
