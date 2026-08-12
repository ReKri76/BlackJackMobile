package io.rekri.blackjackmobile.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import api.Status
import card.Card
import card.Suit
import card.Value
import io.rekri.blackjackmobile.R
import io.rekri.blackjackmobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainWidget(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    when (state.status) {
        Status.START -> StartGameDialog(
            onConfirm = { initialStack -> viewModel.addStack(initialStack) }
        )
        Status.STOP -> StartRoundDialog(
            maxBet = state.stack,
            onConfirm = { bet -> viewModel.startGame(bet) }
        )
        Status.ERROR -> EndOfRoundDialog(text = "ERROR" to null) { viewModel.stopGame() }
        Status.LOSE, Status.PLAYER_IS_TOO_MUCH -> EndOfRoundDialog(text = "Lose" to ResultLoss) { viewModel.stopGame() }
        Status.DEALER_IS_TOO_MUCH, Status.WIN -> EndOfRoundDialog(text = "Win" to ResultWin) { viewModel.stopGame() }
        Status.PLAYER_BLACKJACK -> EndOfRoundDialog(text = "Blackjack!" to ResultBlackjack) { viewModel.stopGame() }
        Status.PUSH -> EndOfRoundDialog(text = "push" to ResultPush) { viewModel.stopGame() }
        Status.CONTINUE, Status.WAITING -> {}
    }

    if (state.isInsuranceOffered) {
        InsuranceOffered(
            onConfirm = { viewModel.insurance() },
            onDismiss = { viewModel.skipInsurance() }
        )
    }

    Table(
        stack = state.stack,
        bet = state.currentBet,
        dealerHand = state.dealerHand,
        sizeOfDeck = state.sizeOfDeck ?: 0,
        playerHand = state.playerHand,
        onHit = ButtonState(
            isEnabled = state.status == Status.CONTINUE,
            onClick = { viewModel.hit() }
        ),
        onStand = ButtonState(
            isEnabled = state.status == Status.CONTINUE,
            onClick = { viewModel.stand() }
        ),
        onSurrender = ButtonState(
            isEnabled = state.playerHand.size == 2 && state.status == Status.CONTINUE,
            onClick = { viewModel.surrender() }
        ),
        onDouble = ButtonState(
            isEnabled = state.status == Status.CONTINUE && state.stack >= state.currentBet,
            onClick = { viewModel.double() }
        ),
        modifier = modifier
    )
}

@Composable
fun InsuranceOffered(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Insurance?", fontWeight = FontWeight.Bold) },
        text = { Text("Dealer has an Ace. Would you like to take insurance for half your bet?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Conifurm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
        modifier = modifier
    )
}

@Composable
fun EndOfRoundDialog(
    text: Pair<String, Color?>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isVisible by remember(text) { mutableStateOf(false) }

    LaunchedEffect(text) {
        delay(1000.milliseconds)
        isVisible = true
    }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Round Finished", fontWeight = FontWeight.Bold, color = text.second?: Color.White) },
            text = { Text(text = text.first, fontSize = 18.sp, color = text.second?: Color.White) },
            confirmButton = {
                Button(onClick = onClick) {
                    Text("Next Round")
                }
            },
            modifier = modifier
        )
    }
}

@Composable
fun StartRoundDialog(
    maxBet: Double,
    onConfirm: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val minBet = 1.0
    var betAmount by remember { mutableStateOf(minBet.coerceAtMost(maxBet)) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Place your bet", fontWeight = FontWeight.Bold) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                IconButton(
                    onClick = { if (betAmount - 1 >= minBet) betAmount -= 1 },
                    enabled = betAmount - 10 >= minBet
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }

                Text(
                    text = "$${betAmount.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = { if (betAmount + 1 <= maxBet) betAmount += 1 },
                    enabled = betAmount + 10 <= maxBet
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(betAmount) },
                enabled = maxBet >= minBet
            ) {
                Text("Deal")
            }
        },
        modifier = modifier
    )
}

@Composable
fun StartGameDialog(
    onConfirm: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputValue by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Welcome to Blackjack", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { if (it.all { char -> char.isDigit() }) inputValue = it },
                label = { Text("Starting chips") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val data = inputValue.toDoubleOrNull() ?: 0.0
                    if (data > 0) onConfirm(data)
                },
                enabled = (inputValue.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Start Game")
            }
        },
        modifier = modifier
    )
}

@Composable
fun Table(
    stack: Double,
    bet: Double,
    dealerHand: List<Card>?,
    sizeOfDeck: Int,
    playerHand: List<Card>?,
    onHit: ButtonState,
    onStand: ButtonState,
    onSurrender: ButtonState,
    onDouble: ButtonState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = TableGreen,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopPart(stack, bet, dealerHand, sizeOfDeck)

            Spacer(modifier = Modifier.weight(1f))

            BottomPart(playerHand, onHit, onStand, onSurrender, onDouble)
        }
    }
}

@Composable
fun TopPart(
    stack: Double,
    bet: Double,
    dealerHand: List<Card>?,
    sizeOfDeck: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeckComponent(sizeOfDeck)
            StackComponent(stack, bet)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Label("DEALER")
        Spacer(modifier = Modifier.height(8.dp))
        HandDisplay(dealerHand)
    }
}

@Composable
fun BottomPart(
    playerHand: List<Card>?,
    onHit: ButtonState,
    onStand: ButtonState,
    onSurrender: ButtonState,
    onDouble: ButtonState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HandDisplay(playerHand)
        Spacer(modifier = Modifier.height(8.dp))
        Label("PLAYER")

        Spacer(modifier = Modifier.height(24.dp))

        ActionsButtons(onHit = onHit, onStand = onStand, onSurrender = onSurrender, onDouble = onDouble)
    }
}

@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun DeckComponent(
    size: Int,
    modifier: Modifier = Modifier
) {
    val cardWidth = 60.dp
    val cardHeight = 85.dp
    val shape = RoundedCornerShape(5.dp)
    val maxOffset = 6.dp

    Box(
        modifier = modifier.size(cardWidth + maxOffset, cardHeight + maxOffset),
        contentAlignment = Alignment.TopStart
    ) {
        for (i in 2 downTo 0) {
            val offset = (i * 3).dp
            Surface(
                modifier = Modifier
                    .offset(x = offset, y = offset)
                    .size(cardWidth, cardHeight),
                shape = shape,
                shadowElevation = if (i == 0) 4.dp else 2.dp,
                color = Color.Transparent
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.card_back),
                        contentDescription = if (i == 0) "Deck Back" else null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.White.copy(alpha = 0.5f), shape)
                    )

                    if (i == 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "DECK",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = size.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StackComponent(
    stack: Double,
    bet: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.End) {
            Text(
                text = "CHIPS: $${stack.toInt()}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if (bet > 0) {
                Text(
                    text = "BET: $${bet.toInt()}",
                    color = GoldAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun HandDisplay(
    cards: List<Card>?,
    modifier: Modifier = Modifier
) {
    if (cards.isNullOrEmpty()) return

    LazyRow(
        modifier = modifier
            .height(110.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((-30).dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = cards,
            key = { card -> card.uuid }
        ) { card ->
            Box(
                modifier = Modifier.animateItem(
                    fadeInSpec = tween(400),
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                BlackjackCard(card)
            }
        }
    }
}

@Composable
fun BlackjackCard(
    card: Card,
    modifier: Modifier = Modifier
) {
    val (suitColor, suitSymbol) = when (card.suit) {
        Suit.HEARTS -> CardRed to "♥"
        Suit.DIAMONDS -> CardRed to "♦"
        Suit.SPADES -> CardBlack to "♠"
        Suit.CLUBS -> CardBlack to "♣"
    }
    val valueStr = when (card.value) {
        Value.ACE -> "A"
        Value.KING -> "K"
        Value.QUEEN -> "Q"
        Value.JACK -> "J"
        else -> card.value.value.toString()
    }

    ElevatedCard(
        modifier = modifier.size(width = 70.dp, height = 100.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            Text(
                text = valueStr + suitSymbol,
                color = suitColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = suitSymbol,
                color = suitColor,
                fontSize = 32.sp,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = valueStr + suitSymbol,
                color = suitColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

data class ButtonState(val isEnabled: Boolean, val onClick: () -> Unit)

@Composable
fun ActionsButtons(
    onHit: ButtonState,
    onStand: ButtonState,
    onSurrender: ButtonState,
    onDouble: ButtonState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val btnModifier = Modifier.weight(1f)

        if(onHit.isEnabled)
            GameButton(
                text = "HIT",
                state = onHit,
                modifier = btnModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionHit,
                    contentColor = Color.White
                )
            )

        if (onStand.isEnabled)
            GameButton(
                text = "STAND",
                state = onStand,
                modifier = btnModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionStand,
                    contentColor = Color.White
                )
            )

        if (onDouble.isEnabled)
            GameButton(
                text = "X2",
                state = onDouble,
                modifier = btnModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionDouble,
                    contentColor = Color.White
                )
            )

        if (onSurrender.isEnabled)
            GameButton(
                text = "FOLD",
                state = onSurrender,
                modifier = btnModifier,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionFold,
                    contentColor = Color.White
                )
            )
    }
}

@Composable
fun GameButton(
    text: String,
    state: ButtonState,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonDefaults.buttonColors()
) {
    Button(
        onClick = state.onClick,
        enabled = state.isEnabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = colors,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}