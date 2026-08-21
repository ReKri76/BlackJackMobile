// SPDX-License-Identifier: GPL-3.0-only
package io.rekri.blackjackmobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.rekri.blackjackengine.engine.config.BlackJackRules
import io.rekri.blackjackengine.engine.config.Config
import io.rekri.blackjackengine.engine.config.DealerStand
import io.rekri.blackjackengine.engine.config.DoubleRules
import io.rekri.blackjackengine.engine.config.HideCard
import io.rekri.blackjackengine.engine.config.Surrender
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

@Composable
fun RulesDialog(
    config: Config?,
    onConfirm: (Config) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentConfig = config
        ?: Config(
            8,
            DealerStand.SOFT_17,
            Surrender.EARLY_SURRENDER,
            true,
            HideCard.EUROPEAN,
            DoubleRules.ANY,
            BlackJackRules.THREE_TO_TWO
        )

    var countOfDecks by remember { mutableIntStateOf(currentConfig.countOfDecks) }
    var dealerStand by remember { mutableStateOf(currentConfig.dealerStand) }
    var surrender by remember { mutableStateOf(currentConfig.surrender) }
    var isDaS by remember { mutableStateOf(currentConfig.isDaS) }
    var hideCardRules by remember { mutableStateOf(currentConfig.hideCardRules) }
    var doubleRules by remember { mutableStateOf(currentConfig.doubleRules) }
    var blackJackRules by remember { mutableStateOf(currentConfig.blackJackRules) }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Select Rules") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RuleDropdown(
                    label = "Decks",
                    options = listOf(3, 4, 5, 6, 7, 8),
                    selected = countOfDecks,
                    optionLabel = { "$it deck${if (it > 1) "s" else ""}" },
                    onSelected = { countOfDecks = it }
                )

                RuleDropdown(
                    label = "Dealer stands on",
                    options = DealerStand.entries,
                    selected = dealerStand,
                    optionLabel = { if (it == DealerStand.SOFT_17) "Soft 17" else "Hard 17" },
                    onSelected = { dealerStand = it }
                )

                RuleDropdown(
                    label = "Surrender",
                    options = Surrender.entries,
                    selected = surrender,
                    optionLabel = {
                        when (it) {
                            Surrender.NO_SURRENDER -> "Not allowed"
                            Surrender.LATE_SURRENDER -> "Late surrender"
                            Surrender.EARLY_SURRENDER -> "Early surrender"
                        }
                    },
                    onSelected = { surrender = it }
                )

                RuleDropdown(
                    label = "Double after split",
                    options = listOf(true, false),
                    selected = isDaS,
                    optionLabel = { if (it) "Allowed" else "Not allowed" },
                    onSelected = { isDaS = it }
                )

                RuleDropdown(
                    label = "Dealer hole card",
                    options = HideCard.entries,
                    selected = hideCardRules,
                    optionLabel = { if (it == HideCard.AMERICAN) "American rule" else "European rule" },
                    onSelected = { hideCardRules = it }
                )

                RuleDropdown(
                    label = "Doubling allowed on",
                    options = DoubleRules.entries,
                    selected = doubleRules,
                    optionLabel = {
                        when (it) {
                            DoubleRules.ANY -> "Any two cards"
                            DoubleRules.NINE_TEN_ELEVEN -> "9, 10, 11"
                            DoubleRules.TEN_ELEVEN -> "10, 11"
                        }
                    },
                    onSelected = { doubleRules = it }
                )

                RuleDropdown(
                    label = "Blackjack payout",
                    options = BlackJackRules.entries,
                    selected = blackJackRules,
                    optionLabel = { if (it == BlackJackRules.THREE_TO_TWO) "3 : 2" else "6 : 5" },
                    onSelected = { blackJackRules = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Config(
                            countOfDecks,
                            dealerStand,
                            surrender,
                            isDaS,
                            hideCardRules,
                            doubleRules,
                            blackJackRules
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        },
        modifier = modifier
    )
}

@Composable
fun <T> RuleDropdown(
    label: String,
    options: List<T>, //варианты
    selected: T, //текущий вариант
    optionLabel: (T) -> String, //функция перевода enum значения в string
    onSelected: (T) -> Unit, //callback
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(optionLabel(selected))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}