// SPDX-License-Identifier: GPL-3.0-only
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.rekri.blackjackengine.card.Card
import io.rekri.blackjackengine.card.Suit
import io.rekri.blackjackengine.card.Value
import io.rekri.blackjackengine.engine.Status
import io.rekri.blackjackmobile.R
import io.rekri.blackjackmobile.ui.theme.*

@Composable
fun MainWidget(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val isSplit = state.split!=0

    when (state.status) {
        Status.START -> StartGameDialog(
            onConfirm = { initialStack -> viewModel.addStack(initialStack) }
        )
        Status.STOP -> StartRoundDialog(
            maxBet = state.stack,
            onConfirm = { bet -> viewModel.startGame(bet) }
        )
        Status.ERROR -> EndOfRoundDialog(
            text = "ERROR" to MaterialTheme.colorScheme.error,
            isSplit = isSplit,
            icon = Icons.Default.Warning
        ) { viewModel.stopGame() }
        Status.LOSE, Status.PLAYER_IS_TOO_MUCH, Status.DEALER_BLACKJACK -> EndOfRoundDialog(
            text = "Lose" to ResultLoss,
            isSplit = isSplit,
            icon = Icons.Default.Close
        ) { viewModel.stopGame() }
        Status.DEALER_IS_TOO_MUCH, Status.WIN -> EndOfRoundDialog(
            text = "Win" to ResultWin,
            isSplit = isSplit,
            icon = Icons.Default.Done
        ) { viewModel.stopGame() }
        Status.PLAYER_BLACKJACK -> EndOfRoundDialog(
            text = "Blackjack!" to ResultBlackjack,
            isSplit = isSplit,
            icon = Icons.Default.Star
        ) { viewModel.stopGame() }
        Status.PUSH -> EndOfRoundDialog(
            text = "Push" to ResultPush,
            isSplit = isSplit,
            icon = Icons.Default.Info
        ) { viewModel.stopGame() }
        Status.CONTINUE, Status.WAITING -> {}
    }

    var configWindow by remember { mutableStateOf(true) }

    if (configWindow)
        RulesDialog(
            config = viewModel.viewConfig(),
            onConfirm = {
                viewModel.changeRules(it)
                configWindow = false
            },
            onDismiss = {configWindow = false},
            modifier = modifier
        )

    if (state.isInsuranceOffered && !state.isSplitAvailable && state.status == Status.CONTINUE)
        InsuranceOffered(
            onConfirm = { viewModel.insurance() },
            onDismiss = { viewModel.skipInsurance() }
        )

    if (state.isSplitAvailable && state.status == Status.CONTINUE)
        SplitOffered(
            onConfirm = {viewModel.split()},
            onDismiss = {viewModel.skipSplit()}
        )

    Table(
        stack = state.stack,
        bet = state.currentBet,
        dealerHand = state.dealerHand,
        sizeOfDeck = state.sizeOfDeck ?: 0,
        playerHand = state.playerHand,
        countOfSplits = state.split,
        onHit = ButtonState(
            isEnabled = state.status == Status.CONTINUE,
            onClick = { viewModel.hit() }
        ),
        onStand = ButtonState(
            isEnabled = state.status == Status.CONTINUE,
            onClick = { viewModel.stand() }
        ),
        onSurrender = ButtonState(
            isEnabled = state.isSurrenderAvailable && state.status == Status.CONTINUE,
            onClick = { viewModel.surrender() }
        ),
        onDouble = ButtonState(
            isEnabled = state.isDoubleAvailable && state.status == Status.CONTINUE,
            onClick = { viewModel.double() }
        ),
        isAmericanRules = state.isAmericanRules,
        openConfigWindow = {configWindow = true},
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
    countOfSplits : Int,
    isAmericanRules: Boolean,
    openConfigWindow : () -> Unit,
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
            TopPart(stack, bet, dealerHand, sizeOfDeck, countOfSplits,
                isAmericanRules, openConfigWindow)

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
    countOfSplits : Int,
    isAmericanRules : Boolean,
    openConfigWindow : () -> Unit,
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

            IconButton(onClick =  openConfigWindow) {
                Icon(Icons.Default.Settings, contentDescription = "Rules", tint = Color.White)
            }

            StackComponent(stack, bet, countOfSplits)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Label("DEALER")
        Spacer(modifier = Modifier.height(8.dp))
        DealerHandDisplay(dealerHand, isAmericanRules)
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
        PlayerHandDisplay(playerHand)
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
            style = MaterialTheme.typography.labelSmall,
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
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = size.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
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
    countOfSplits : Int,
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
                text = "CHIPS: $${stack}",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (bet > 0)
                Text(
                    text = "BET: $${bet}",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

            if (countOfSplits > 0)
                Text(
                    text = "SPLITS: ${countOfSplits}",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
        }
    }
}

@Composable
fun DealerHandDisplay(
    inputCards: List<Card>?,
    isAmericanRules: Boolean,
    modifier: Modifier = Modifier
) {
    if (inputCards.isNullOrEmpty()) return

    val cards : List<Card?> = if (inputCards.size==1 && isAmericanRules)
        listOf(inputCards[0], null)
    else
        inputCards


    LazyRow(
        modifier = modifier
            .height(110.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((-30).dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = cards,
            key = { card -> if (card!=null) card.uuid else "placeholder_id" }
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
                if (card!=null)
                    BlackjackCard(card)
                else
                    Image(
                        painter = painterResource(id = R.drawable.card_back),
                        contentDescription = "Card Back",
                        contentScale = ContentScale.Crop,
                        modifier = modifier.size(width = 70.dp, height = 100.dp),
                    )
            }
        }
    }
}

@Composable
fun PlayerHandDisplay(
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = suitSymbol,
                color = suitColor,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = valueStr + suitSymbol,
                color = suitColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
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
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}