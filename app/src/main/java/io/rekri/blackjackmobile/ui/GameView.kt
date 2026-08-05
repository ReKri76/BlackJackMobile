package io.rekri.blackjackmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import card.Card
import card.Suit
import card.Value

@Composable
fun MainWidget(viewModel: GameViewModel = viewModel()) {

    val state by viewModel.uiState.collectAsState()

}

@Composable
fun Deck(size: Int, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(60.dp)
            .background(Color.White)
            .border(1.dp, Color.Black)
            .padding(8.dp)
    ){
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = size.toString(),
                color = Color.White,
                fontSize = 5.sp
            )
        }
    }
}

@Composable
fun DealerHand(cards : List<Card>?, modifier: Modifier = Modifier){
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards?.forEach { card ->
            Card(card)
        }
    }
}

@Composable
fun PlayerHand(cards: List<Card>?, modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards?.forEach { card ->
            Card(card)
        }
    }
}

@Composable
fun Card(
    card: Card,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, Color.Black)
            .padding(8.dp)
    ) {
        Text(
            text = asciCard(card),
            fontSize = 7.sp
        )
    }
}

@Composable
fun ActionsButton(
    modifier: Modifier,
    onHit : () -> Unit,
    onStand : () ->Unit,
    onSurrender : () -> Unit,
    onDouble : () -> Unit,
){
    Row(
        modifier= modifier
    ){
        Button( onClick = onHit){Text("HIT")}
        Button( onClick = onStand){Text("STAND")}
        Button( onClick = onSurrender){Text("SURRENDER")}
        Button( onClick = onDouble){Text("DOUBLE")}
    }
}

@Composable
fun Stack(modifier: Modifier, size : Int){
    Box(
        modifier = modifier
            .size(12.dp)
            .background(Color.Black)
            .border(1.dp, Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = size.toString(),
            color = Color.White,
            fontSize = 5.sp
        )
    }
}

private fun asciCard(card: Card): String{

    val value = when(card.value){
        Value.ACE->"A"
        Value.KING->"K"
        Value.QUEEN->"Q"
        Value.JACK->"J"
        else -> card.value.value.toString()
    }

    val suit = when(card.suit){
        Suit.HEARTS->"♥"
        Suit.SPADES->"♠"
        Suit.CLUBS->"♣"
        Suit.DIAMONDS->"♦"
    }

    return value+suit
}
