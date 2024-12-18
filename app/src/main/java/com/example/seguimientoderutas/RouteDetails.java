package com.example.seguimientoderutas;

public class RouteDetails {
    private String distance;
    private String duration;

    public RouteDetails(String distance, String duration) {
        this.distance = distance;
        this.duration = duration;
    }

    // Getters y setters
    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
