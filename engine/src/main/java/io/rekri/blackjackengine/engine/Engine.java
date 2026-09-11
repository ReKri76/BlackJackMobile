// SPDX-License-Identifier: MPL-2.0
package io.rekri.blackjackengine.engine;

import io.rekri.blackjackengine.card.Card;
import io.rekri.blackjackengine.card.Value;
import io.rekri.blackjackengine.engine.config.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Engine {
    private Deck deck;

    @NotNull private List<@NotNull Card> dealerHand = new ArrayList<>();
    @Nullable private Card hideCard;
    @NotNull private final List<@NotNull Card> playerHand = new ArrayList<>();
    @NotNull private final Config config;
    private boolean isSplitWas = false;
    private boolean splitWasSkip = false;
    @Nullable private List<Card> deterministicDeck = null;

    public record State(
            @NotNull List<Card> dealer,
            @NotNull List<Card> player,
            @NotNull Status status
    ) {}

    public Engine(@NotNull Config config) {
        this.config = config;
    }

    @NotNull
    public State shuffle() {
        if (deterministicDeck !=null)
            deck = new Deck(deterministicDeck);
        else
            deck = new Deck(config.countOfDecks());
        return turn();
    }

    @NotNull
    public List<Card> getPlayerHand() { return playerHand; }

    @NotNull
    public State turn() {

        splitWasSkip = false;

        playerHand.clear();
        dealerHand.clear();

        playerHand.add(deck.draw());
        playerHand.add(deck.draw());

        dealerHand.add(deck.draw());
        hideCard = config.hideCardRules().equals(HideCard.AMERICAN) ? deck.draw() : null;

        return status(false);
    }

    @NotNull
    public State end() {
        if (isDealerBlackJack())
            return new State(List.copyOf(dealerHand), List.copyOf(playerHand), Status.DEALER_BLACKJACK);

        revealHideCard();

        while (config.dealerStand().equals(DealerStand.SOFT_17) ?
                softCount(dealerHand) <= 16 : hardCount(dealerHand) <= 16)
            dealerHand.add(deck.draw());

        return status(true);
    }

    @NotNull
    public State draw() {
        playerHand.add(deck.draw());
        return status(false);
    }

    /**
     * Draw card or show hide card if it exists.
     */
    @NotNull
    public State dealerDraw() {
        if (config.hideCardRules().equals(HideCard.EUROPEAN))
            dealerHand.add(deck.draw());
        else
            revealHideCard();

        return status(false);
    }

    public boolean isSurrenderAvailable() {
        if (config.surrender().equals(Surrender.NO_SURRENDER) || isSplitWas)
            return false;

        return playerHand.size() == 2;
    }

    public boolean isSplitAvailable() {
        return !splitWasSkip && playerHand.size() == 2 &&
                playerHand.get(0).value() == playerHand.get(1).value() &&
                !isDealerBlackJack();
    }

    public boolean isDealerBlackJack() {
        if (hideCard != null)
            dealerHand.add(hideCard);

        var res = dealerHand.size() == 2 &&
                (dealerHand.get(0).value().equals(Value.ACE) && dealerHand.get(1).value().getValue() == 10 ||
                dealerHand.get(1).value().equals(Value.ACE) && dealerHand.get(0).value().getValue() == 10
                );

        dealerHand.remove(hideCard);

        return res;
    }

    public boolean isDoubleAvailable() {
        if (config.doubleRules().equals(DoubleRules.ANY))
            return true;

        if (config.isDaS() && isSplitWas)
            return false;

        final var firstCard = playerHand.get(0);
        final var secondCard = playerHand.get(1);
        final var sum = firstCard.value().getValue() + secondCard.value().getValue();

        return config.doubleRules().equals(DoubleRules.TEN_ELEVEN) && (sum == 10 || sum == 11) ||
                config.doubleRules().equals(DoubleRules.NINE_TEN_ELEVEN) && (sum == 10 || sum == 11 || sum == 9);
    }

    @NotNull
    public Engine split() {
        var res = new Engine(this.config);
        res.deck = this.deck;
        final var currentFirst = playerHand.get(0);
        res.playerHand.add(new Card(currentFirst.suit(), currentFirst.value(), currentFirst.uuid()));
        playerHand.remove(0);
        res.dealerHand = this.dealerHand;
        revealHideCard();
        res.hideCard = null;
        res.isSplitWas = this.isSplitWas;
        return res;
    }

    public State skipSplit(){
        splitWasSkip = true;
        return status(false);
    }

    public int getSizeOfDeck() {
        return deck != null ? deck.getSize() : 0;
    }

    public void setIsSplitWas(boolean status) {
        isSplitWas = status;
    }

    public boolean isSplitWas() {
        return isSplitWas;
    }

    /**
     * Showing hide card in American rules when game is over if enabled in settings.
     */
    public State showHideCard() {
        if (config.isDealerShowSecondCardInAmericanRule() && config.hideCardRules().equals(HideCard.AMERICAN))
            revealHideCard();
        return status(true);
    }

    public void addDeterministicHand(List<Card> deck){
        deterministicDeck = deck;
    }

    private void revealHideCard() {
        if (hideCard != null) {
            dealerHand.add(new Card(hideCard.suit(), hideCard.value(), hideCard.uuid()));
            hideCard = null;
        }
    }

    @NotNull
    private State status(boolean isOver) {
        var playerPoints = softCount(playerHand);

        var dealerPoints = config.dealerStand().equals(DealerStand.SOFT_17) ? softCount(dealerHand)
                : hardCount(dealerHand);

        final var dealerBlackJack = isDealerBlackJack();

        var status = Status.CONTINUE;

        if (playerPoints > 21)
            status = Status.PLAYER_IS_TOO_MUCH;
        else if (playerPoints == 21 && playerHand.size() == 2 && dealerBlackJack)
            status = Status.PUSH;
        else if (playerPoints == 21 && playerHand.size() == 2 && !isSplitWas)
            status = Status.PLAYER_BLACKJACK;
        else if (dealerBlackJack)
            status = Status.DEALER_BLACKJACK;
        else if (dealerPoints > 21)
            status = Status.DEALER_IS_TOO_MUCH;
        else if (isOver)
            if (dealerPoints > playerPoints)
                status = Status.LOSE;
            else if (dealerPoints < playerPoints)
                status = Status.WIN;
            else
                status = Status.PUSH;

        return new State(List.copyOf(dealerHand), List.copyOf(playerHand), status);
    }

    private int softCount(List<Card> hand) {
        var count = 0;
        var aces = 0;

        for (var card : hand) {
            count += card.value().getValue();
            if (card.value().equals(Value.ACE))
                aces++;
        }

        while (count > 21 && aces > 0) {
            count -= 10;
            aces--;
        }

        return count;
    }

    private int hardCount(List<Card> hand) {
        var count = 0;

        for (var card : hand)
            count += card.value().equals(Value.ACE) ? 1 : card.value().getValue();

        return count;
    }
}