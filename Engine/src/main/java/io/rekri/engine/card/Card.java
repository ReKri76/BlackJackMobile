package io.rekri.engine.card;

public record Card (
    Suit suit,
    Value value,
    String uuid
) {}
