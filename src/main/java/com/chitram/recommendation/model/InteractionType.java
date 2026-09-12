package com.chitram.recommendation.model;

public enum InteractionType {
    VIEW(1.0),
    LONG_VIEW(2.0),
    CLICK(3.0),
    LIKE(5.0),
    SAVE(8.0),
    SHARE(10.0),
    HIDE(-10.0),
    REPORT(-20.0);

    private final double weight;

    InteractionType(double weight) {
        this.weight = weight;
    }

    public double weight() {
        return weight;
    }
}
