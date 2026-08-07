package io.rekri.blackjackmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import api.Status
import card.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import card.Suit
import card.Value
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MainWidget(modifier: Modifier = Modifier, viewModel: GameViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    when(state.status){
        Status.START -> StartGameDialog(
            onConfirm = { initialStack -> viewModel.addStack(initialStack) }
        )
        Status.STOP -> StartRoundDialog(
            maxBet = state.stack,
            onConfirm = { bet -> viewModel.startGame(bet) }
        )
        Status.ERROR-> EndOfRoundDialog(text= "ERROR") { viewModel.stopGame() }
        Status.LOSE, Status.PLAYER_IS_TOO_MUCH -> EndOfRoundDialog(text = "LOSE")
            { viewModel.stopGame()}
        Status.DEALER_IS_TOO_MUCH, Status.PLAYER_BLACKJACK, Status.WIN ->
            EndOfRoundDialog(text = "WIN") { viewModel.stopGame()}
        Status.PUSH->EndOfRoundDialog(text = "PUSH") { viewModel.stopGame()}
        Status.CONTINUE -> {}
        Status.WAITING -> {}
    }

    if (state.isInsuranceOffered)
        InsuranceOffered(
            onConfirm = { viewModel.insurance() },
            onDismiss = { viewModel.skipInsurance() }
        )

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
            isEnabled = state.playerHand.size==2 && state.status == Status.CONTINUE,
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
fun InsuranceOffered(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Insurance?", fontWeight = FontWeight.Bold) },
        text = { Text("Dealer has an Ace. Would you like to take insurance for half your bet?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}

@Composable
fun EndOfRoundDialog(text: String, onClick: () -> Unit){
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1000.milliseconds)
        isVisible = true
    }

    if (isVisible)
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Round Finished", fontWeight = FontWeight.Bold) },
        text = { Text(text= text, fontSize = 18.sp) },
        confirmButton = {
            Button(onClick = onClick) {
                Text("Next Round")
            }
        }
    )
}

@Composable
fun StartRoundDialog(maxBet: Double, onConfirm: (Double) -> Unit) {
    var betAmount by remember { mutableStateOf(10.0) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Place your bet", fontWeight = FontWeight.Bold) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            ) {
                IconButton(
                    onClick = { if (betAmount >= 20) betAmount -= 10 },
                    enabled = betAmount >= 20
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
                    onClick = { if (betAmount + 10 <= maxBet) betAmount += 10 },
                    enabled = betAmount + 10 <= maxBet
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(betAmount) }
            ) {
                Text("Deal")
            }
        }
    )
}

@Composable
fun StartGameDialog(onConfirm: (Double) -> Unit) {
    var inputValue by remember { mutableStateOf("1000") }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(text = "Welcome to Blackjack", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { if (it.all { char -> char.isDigit() }) inputValue = it },
                label = { Text("Starting chips") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val data = inputValue.toDoubleOrNull() ?: 0.0
                    onConfirm(data)
                },
                enabled = inputValue.isNotBlank()
            ) {
                Text("Start Game")
            }
        }
    )
}

@Composable
fun Table(
    stack: Double, bet: Double,
    dealerHand : List<Card>?,
    sizeOfDeck : Int,
    playerHand : List<Card>?,
    onHit : ButtonState,
    onStand : ButtonState,
    onSurrender : ButtonState,
    onDouble : ButtonState,
    modifier: Modifier = Modifier) {

    Scaffold(
        containerColor = Color(0xFF1B5E20),
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
fun TopPart(stack: Double, bet: Double,
            dealerHand : List<Card>?,
            sizeOfDeck : Int,
            modifier: Modifier = Modifier){
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
fun BottomPart(playerHand : List<Card>?,
               onHit : ButtonState,
               onStand : ButtonState,
               onSurrender : ButtonState,
               onDouble : ButtonState,
               modifier: Modifier = Modifier){
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        HandDisplay(playerHand)
        Spacer(modifier = Modifier.height(8.dp))
        Label("PLAYER")
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ActionsButtons(onHit=onHit, onStand=onStand,onSurrender=onSurrender, onDouble=onDouble)
    }
}

@Composable
fun Label(text: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(4.dp)
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
fun DeckComponent(size: Int) {
    Box(
        modifier = Modifier
            .size(50.dp, 70.dp)
            .background(Color(0xFFB71C1C), RoundedCornerShape(4.dp))
            .border(1.dp, Color.White.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("DECK", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
            Text(text = size.toString(), color = Color.White, fontWeight = FontWeight.Bold,
                fontSize = 16.sp)
        }
    }
}

@Composable
fun StackComponent(stack: Double, bet: Double) {
    Surface(
        color = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation
            .BorderStroke(1.dp, Color.Yellow.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.End) {
            Text(text = "CHIPS: $${stack.toInt()}",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (bet > 0)
                Text(text = "BET: $${bet.toInt()}", color = Color.Yellow,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun HandDisplay(cards: List<Card>?) {
    if (cards.isNullOrEmpty()) return

    LazyRow(
        modifier = Modifier
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
fun BlackjackCard(card: Card) {
    val (suitColor, suitSymbol) = when (card.suit) {
        Suit.HEARTS -> Color(0xFFD32F2F) to "♥"
        Suit.DIAMONDS -> Color(0xFFD32F2F) to "♦"
        Suit.SPADES -> Color(0xFF212121) to "♠"
        Suit.CLUBS -> Color(0xFF212121) to "♣"
    }
    val valueStr = when(card.value){
        Value.ACE->"A"
        Value.KING->"K"
        Value.QUEEN->"Q"
        Value.JACK->"J"
        else -> card.value.value.toString()
    }

    ElevatedCard(
        modifier = Modifier.size(width = 70.dp, height = 100.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
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

data class ButtonState(val isEnabled : Boolean, val onClick : ()->Unit)

@Composable
fun ActionsButtons(
    onHit : ButtonState,
    onStand : ButtonState,
    onSurrender : ButtonState,
    onDouble : ButtonState,
    modifier: Modifier = Modifier
){
    Row(
        modifier= modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ){
        val btnModifier = Modifier.weight(1f)
        
        GameButton("HIT", onHit, btnModifier,
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E7D32), contentColor = Color.White))
        GameButton("STAND", onStand, btnModifier,
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFFC62828), contentColor = Color.White))

        GameButton("X2", onDouble, btnModifier,
            ButtonDefaults.buttonColors(
                containerColor = Color(0, 187, 255), contentColor = Color.White))

        GameButton("FOLD", onSurrender, btnModifier,
            ButtonDefaults.buttonColors(
                containerColor = Color(0xFF212121), contentColor = Color.White))
    }
}

@Composable
fun GameButton(text: String, state: ButtonState,
               modifier: Modifier, colors: ButtonColors = ButtonDefaults.buttonColors()) {

    if (state.isEnabled)
        Button(
            onClick = state.onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = colors,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

}
