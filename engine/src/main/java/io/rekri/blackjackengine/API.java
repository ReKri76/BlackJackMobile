// SPDX-License-Identifier: MPL-2.0
package io.rekri.blackjackengine;

import io.rekri.blackjackengine.engine.Engine;
import io.rekri.blackjackengine.engine.Status;
import io.rekri.blackjackengine.engine.Engine.State;
import io.rekri.blackjackengine.card.Value;
import io.rekri.blackjackengine.engine.config.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class API {
    private final Engine engine;
    State currentState;
    private double currentBet;
    private double insuranceBet = 0.0;
    private final int minSizeOfDeck;
    private boolean insuranceIsOffered = false;
    private boolean isGameOver = false;
    private final Config config;
    private boolean splitHand = false;

    /**
     * Represents the outcome of a single game action.
     *
     * @param state the resulting game state (dealer/player hands and status)
     * @param insuranceIsOffered whether insurance is currently being offered to the player
     * @param win the profit or loss produced by this action, or {@code null} if the round is not yet resolved
     * @param deckSize the number of cards remaining in the deck
     */
    public record Response(
            @NotNull State state,
            @NotNull Boolean insuranceIsOffered,
            @Nullable Double win,
            @NotNull Integer deckSize
    ) {}

    API(Engine engine, Config config) {
        this.engine = engine;
        this.config = config;
        minSizeOfDeck = 52 * this.config.countOfDecks() / 3;
    }

    /**
     * Creates a new API instance with the given configuration.
     * A new game engine is created internally.
     *
     * @param config the game configuration to use
     */
    public API(@NotNull Config config) {
        this.config = config;
        engine = new Engine(this.config);
        minSizeOfDeck = 52 * this.config.countOfDecks() / 3;
    }

    /**
     * Creates a new API instance with a default rule set
     */
    public API() {
        this.config = new Config(
                1,
                DealerStand.HARD_17,
                Surrender.LATE_SURRENDER,
                true,
                HideCard.AMERICAN,
                DoubleRules.NINE_TEN_ELEVEN,
                BlackJackRules.THREE_TO_TWO,
                true,
                true
        );
        engine = new Engine(this.config);
        minSizeOfDeck = 52 * this.config.countOfDecks() / 3;
    }

    /**
     * Creates a copy of the given API instance.
     * The underlying engine and configuration are shared, not duplicated.
     *
     * @param api the API instance to copy state from
     */
    public API(@NotNull API api) {
        this.currentBet = api.currentBet;
        this.currentState = api.currentState;
        this.engine = api.engine;
        this.isGameOver = api.isGameOver;
        this.insuranceBet = api.insuranceBet;
        this.insuranceIsOffered = api.insuranceIsOffered;
        this.config = api.config;
        this.minSizeOfDeck = api.minSizeOfDeck;
        this.splitHand = api.splitHand;
    }

    /**
     * Starts a new round with the given bet.
     * Shuffles the deck if needed and deals the initial hands, automatically
     * resolving player/dealer blackjacks and offering insurance when applicable.
     *
     * @param bet the amount to bet; must be positive
     * @return the response describing the state after dealing
     * @throws IllegalArgumentException if {@code bet} is not positive
     */
    @NotNull
    public Response newGame(double bet) {
        if (bet <= 0)
            throw new IllegalArgumentException("Bet must be positive");

        this.currentBet = bet;
        this.insuranceBet = 0.0;
        this.isGameOver = false;
        this.insuranceIsOffered = false;
        this.splitHand =false;
        engine.setIsSplitWas(false);

        currentState = engine.getSizeOfDeck() < minSizeOfDeck || config.isNewDeckPerRound() ?
                engine.shuffle() : engine.turn();

        if (currentState.status().equals(Status.PLAYER_BLACKJACK)) {
            isGameOver = true;
            currentState = engine.dealerDraw();

            if (currentState.status().equals(Status.PUSH)){
                return new Response(currentState, false, 0.0,
                        engine.getSizeOfDeck());
            }

            return new Response(currentState, false,
                    currentBet * (config.blackJackRules().equals(BlackJackRules.THREE_TO_TWO) ? 1.5 : 1.2),
                    engine.getSizeOfDeck());
        }

        if (currentState.dealer().get(0).value().equals(Value.ACE)) {
            insuranceIsOffered = true;
            currentState = new State(currentState.dealer(), currentState.player(), Status.CONTINUE);
            return new Response(currentState, true, null, engine.getSizeOfDeck());
        }

        if (currentState.status().equals(Status.DEALER_BLACKJACK)) {
            if (config.surrender().equals(Surrender.EARLY_SURRENDER))
                currentState = new State(currentState.dealer(), currentState.player(), Status.CONTINUE);
            else {
                currentState = engine.dealerDraw();
                isGameOver = true;
                return new Response(currentState, false, -currentBet - insuranceBet,
                        engine.getSizeOfDeck());
            }
        }

        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    /**
     * Places an insurance bet after the dealer shows an ace.
     * If the dealer has blackjack, the round is resolved immediately.
     *
     * @return the resulting game response
     * @throws IllegalStateException if the game is already over, or if insurance is not currently offered
     */
    @NotNull
    public Response makeInsurance() {
        checkNotGameOver();

        if (!insuranceIsOffered)
            throw new IllegalStateException("Insurance is not offered now");

        insuranceIsOffered = false;
        insuranceBet = currentBet / 2.0;

        if (engine.isDealerBlackJack()) {
            currentState = engine.dealerDraw();
            isGameOver = true;
            return new Response(currentState, false,
                    -currentBet + insuranceBet * 2.0, engine.getSizeOfDeck());
        }

        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    /**
     * Declines the insurance offer.
     * If the dealer has blackjack and hole card rules is a European,
     * the round is resolved immediately.
     *
     * @return the resulting game response
     */
    @NotNull
    public Response skipInsurance() {
        if (engine.isDealerBlackJack() && !config.hideCardRules().equals(HideCard.EUROPEAN)) {
            currentState = engine.dealerDraw();
            isGameOver = true;
            return new Response(currentState, false,
                    -currentBet - insuranceBet, engine.getSizeOfDeck());
        } else
            return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    /**
     * Draws one more card for the player.
     * Ends the round with a loss if the player busts.
     *
     * @return the resulting game response
     * @throws IllegalStateException if the game is already over
     */
    @NotNull
    public Response hit() {
        checkNotGameOver();
        insuranceIsOffered = false;

        final var isDealerBlackJack = chekDealerBlackJack();
        if (isDealerBlackJack != null)
            return isDealerBlackJack;

        currentState = engine.draw();

        if (currentState.status().equals(Status.PLAYER_IS_TOO_MUCH)) {
            isGameOver = true;
            if (!splitHand)
                currentState = engine.showHideCard();
            return new Response(currentState, false, -currentBet - insuranceBet,
                    engine.getSizeOfDeck());
        }

        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    /**
     * Ends the player's turn, plays out the dealer's hand, and resolves the round.
     *
     * @return the resulting game response, including the total profit or loss
     * @throws IllegalStateException if the game is already over
     */
    @NotNull
    public Response stand() {
        checkNotGameOver();
        isGameOver = true;

        if (config.hideCardRules().equals(HideCard.EUROPEAN)){
            engine.dealerDraw();
            var res = chekDealerBlackJack();
            if (res!=null)
                return res;
        }

        currentState = engine.end();

        var insuranceProfit = insuranceBet > 0
                ? (currentState.status().equals(Status.DEALER_BLACKJACK) ? insuranceBet * 2.0 : -insuranceBet)
                : 0.0;

        var status = currentState.status();
        var mainBetProfit = 0.0;

        if (status.equals(Status.LOSE) || status.equals(Status.PLAYER_IS_TOO_MUCH))
            mainBetProfit = -currentBet;
        else if (status.equals(Status.PUSH))
            mainBetProfit = 0.0;
        else
            mainBetProfit = currentBet;

        return new Response(currentState, false, mainBetProfit + insuranceProfit,
                engine.getSizeOfDeck());
    }

    /**
     * Checks whether doubling the bet is currently allowed under the active rules.
     *
     * @return {@code true} if doubling is available
     */
    @NotNull
    public Boolean isDoubleAvailable() {
        return engine.isDoubleAvailable();
    }

    /**
     * Doubles the current bet, draws exactly one more card, and ends the turn.
     *
     * @return the resulting game response
     * @throws IllegalStateException if the game is already over, if double after split is
     *                                disallowed, or if doubling is otherwise unavailable
     */
    @NotNull
    public Response doubleBet() {
        checkNotGameOver();
        if (engine.isSplitWas() && !config.isDaS())
            throw new IllegalStateException("By current rules double after split is not available.");
        if (!engine.isDoubleAvailable())
            throw new IllegalStateException("By current rules double is not available");

        final var isDealerBlackJack = chekDealerBlackJack();
        if (isDealerBlackJack != null)
            return isDealerBlackJack;

        currentState = engine.draw();

        currentBet *= 2;

        if (currentState.status().equals(Status.PLAYER_IS_TOO_MUCH)) {
            isGameOver = true;
            currentState = engine.showHideCard();
            return new Response(currentState, false, -currentBet - insuranceBet,
                    engine.getSizeOfDeck());
        }

        return this.stand();
    }

    /**
     * Checks whether surrendering is currently allowed under the active rules.
     *
     * @return {@code true} if surrender is available
     */
    @NotNull
    public Boolean isSurrenderAvailable() {
        return engine.isSurrenderAvailable();
    }

    /**
     * Surrenders the current hand, forfeiting half of the bet.
     *
     * @return the resulting game response
     * @throws IllegalStateException if the game is already over, or if surrender is not available
     */
    @NotNull
    public Response surrender() {
        checkNotGameOver();

        if (!engine.isSurrenderAvailable())
            throw new IllegalStateException("Surrender is not available.");

        isGameOver = true;

        if (config.surrender().equals(Surrender.LATE_SURRENDER)
                && config.hideCardRules().equals(HideCard.EUROPEAN)){
            currentState = engine.dealerDraw();
            currentState = new State(currentState.dealer(), currentState.player(), Status.LOSE);

            final var res = chekDealerBlackJack();
            return Objects.requireNonNullElseGet(res, () ->
                    new Response(currentState, false, -currentBet / 2, engine.getSizeOfDeck()));
        }

        currentState = engine.showHideCard();

        return new Response(currentState, false, -currentBet / 2 , engine.getSizeOfDeck());
    }

    /**
     * Checks whether the current hand can be split.
     *
     * @return {@code true} if split is available (only on the initial two-card hand)
     */
    @NotNull
    public Boolean isSplitAvailable() {
        return engine.isSplitAvailable();
    }

    /**
     * Splits the current hand into two separate hands.
     * This instance continues to represent the first hand, while the returned
     * instance represents the newly created second hand.
     *
     * @return a new API instance managing the split-off hand
     * @throws IllegalStateException if the game is already over, or if split is not available
     */
    @NotNull
    public API split() {
        checkNotGameOver();

        if (!engine.isSplitAvailable())
            throw new IllegalStateException("Split is only available on the initial hand.");

        engine.setIsSplitWas(true);

        var newEngine = engine.split();

        this.currentState = new State(currentState.dealer(), engine.getPlayerHand(), currentState.status());

        var newAPI = new API(newEngine, this.config);
        newAPI.insuranceBet = this.insuranceBet;
        newAPI.currentBet = this.currentBet;
        newAPI.currentState = new State(currentState.dealer(), newEngine.getPlayerHand(), currentState.status());
        newAPI.splitHand =true;
        engine.draw();
        newAPI.hit();

        return newAPI;
    }

    /**
     * Skips resolving the split hand and advances the engine to the next state.
     *
     * @return the resulting game response
     */
    @NotNull
    public Response skipSplit(){
        currentState = engine.skipSplit();
        return new Response(currentState, false, null, engine.getSizeOfDeck());
    }

    /**
     * Returns the configuration this API instance is using.
     *
     * @return the current game configuration
     */
    @NotNull
    public Config getConfig() {
        return this.config;
    }

    /**
     * Returns the current game state without performing any action.
     *
     * @return the current game response
     */
    @NotNull
    public Response getCurrentResponse() {
        var state = new State(currentState.dealer(), engine.getPlayerHand(), currentState.status());
        return new Response(state, insuranceIsOffered, null, engine.getSizeOfDeck());
    }

    private void checkNotGameOver() {
        if (isGameOver)
            throw new IllegalStateException("Game is already over");
    }

    @Nullable
    private Response chekDealerBlackJack() {
        if (engine.isDealerBlackJack()) {
            var insuranceProfit = insuranceBet > 0 ? insuranceBet * 2.0 : 0.0;

            isGameOver = true;
            currentState = engine.showHideCard();
            return new Response(currentState, false, -currentBet + insuranceProfit,
                    engine.getSizeOfDeck());
        }
        else
            return null;
    }
}