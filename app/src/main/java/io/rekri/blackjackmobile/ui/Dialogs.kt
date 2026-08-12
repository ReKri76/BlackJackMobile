package io.rekri.blackjackmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
    text: Pair<String, Color>,
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
            title = { Text(text = "Round Finished", fontWeight = FontWeight.Bold, color = text.second) },
            text = { Text(text = text.first, fontSize = 18.sp, color = text.second) },
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
