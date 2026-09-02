## Interaction Contract

All game interactions are performed through the `API` class. Each player action updates the internal game engine and returns a `Response` record.

### Creating an API instance
- `new API()` — creates an instance with a default config (1 deck, dealer stands on hard 17, late surrender, double on 9/10/11, blackjack pays 3:2, new deck per round).
- `new API(Config config)` — creates an instance with a custom config, letting you set the rule variations yourself.

### Response Object
```java
public record Response(State state, boolean insuranceIsOffered, Double win, Integer deckSize) {}
```
Holds the current game state, whether insurance is offered, the round's profit/loss (`null` while the round is ongoing), and how many cards remain in the deck. See the code for field-level details.

### Core Actions

- `newGame(double bet)`: Starts a new round. Automatically shuffles the deck if cards are running low. Returns the initial state or instantly resolves if the player hits Blackjack.
- `hit()`: Draws a card for the player. Can result in a bust.
- `stand()`: Ends the player's turn, executes the dealer's turn, and calculates final winnings (including insurance).
- `doubleBet()`: Doubles the initial bet, draws exactly one card, and automatically stands (unless busted).
- `split()`: Splits the current hand. Returns a **new instance** of the `API` class specifically for the split hand. **Important:** the original (first) split hand must be played to completion last — play out the new hand returned by `split()` first, and only return to the original hand's instance afterward.
- `surrender()`: Gives up the current hand in exchange for half of the bet. Only available on the initial hand.
- `makeInsurance()`: Places an insurance bet (half of the original bet). Can only be called immediately after `newGame` if `insuranceIsOffered` is true.

### Rules & Exceptions
- The API enforces game rules: calling actions out of turn or when the game is over throws an `IllegalStateException`.
- Negative bets throw an `IllegalArgumentException`.