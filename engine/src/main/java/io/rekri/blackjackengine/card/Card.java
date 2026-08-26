// SPDX-License-Identifier: MPL-2.0
package io.rekri.blackjackengine.card;

public record Card (
    Suit suit,
    Value value,
    String uuid
) {}
