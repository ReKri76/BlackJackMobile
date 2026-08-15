package io.rekri.blackjackmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = {
            Text(
                text = "Insurance?",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Dealer has an Ace. Would you like to take insurance for half your bet?",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
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
fun SplitOffered(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null) },
        title = {
            Text(
                text = "Split?",
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "You have two identical cards. Would you like to split them?",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
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
    isSplit: Boolean,
    icon: ImageVector = Icons.Default.Star,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    if (isSplit) {
        LaunchedEffect(Unit) {
            onClick()
        }
    } else {
        var isVisible by remember(text) { mutableStateOf(false) }

        LaunchedEffect(text) {
            delay(1000.milliseconds)
            isVisible = true
        }

        if (isVisible) {
            AlertDialog(
                onDismissRequest = {},
                icon = { Icon(icon, contentDescription = null, tint = text.second) },
                title = {
                    Text(
                        text = "Round Finished",
                        color = text.second,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = text.first,
                        style = MaterialTheme.typography.bodyLarge,
                        color = text.second,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(onClick = onClick) {
                        Text("Next Round")
                    }
                },
                modifier = modifier
            )
        }
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
        title = { Text(text = "Place your bet") },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { if (betAmount - 1 >= minBet) betAmount -= 1 },
                    enabled = betAmount - 1 >= minBet
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease")
                }

                Text(
                    text = "$${betAmount.toInt()}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                FilledTonalIconButton(
                    onClick = { if (betAmount + 1 <= maxBet) betAmount += 1 },
                    enabled = betAmount + 1 <= maxBet
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
        title = { Text(text = "Welcome to Blackjack") },
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