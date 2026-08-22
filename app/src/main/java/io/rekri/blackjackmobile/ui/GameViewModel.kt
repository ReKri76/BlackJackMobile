// SPDX-License-Identifier: GPL-3.0-only
package io.rekri.blackjackmobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.rekri.blackjackengine.API
import io.rekri.blackjackengine.card.Card
import io.rekri.blackjackengine.engine.Engine
import io.rekri.blackjackengine.engine.Status
import io.rekri.blackjackengine.engine.config.Config
import io.rekri.blackjackengine.engine.config.HideCard
import io.rekri.blackjackengine.engine.config.Surrender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack
import kotlin.time.Duration.Companion.milliseconds

class GameViewModel : ViewModel() {
    private var engine = API()
    private var stack: Double = 0.0
    private var currentBet: Double = 0.0
    private var stackDelta: Double? = null
    private var config: Config? = null

    data class UiState(
        val dealerHand: List<Card>?,
        val playerHand: List<Card>,
        val status: Status,
        val stack: Double,
        val sizeOfDeck: Int?,
        val isInsuranceOffered: Boolean,
        val isSplitAvailable: Boolean,
        val currentBet: Double,
        val split: Int,
        val isAmericanRules: Boolean,
        val isDoubleAvailable: Boolean,
        val isSurrenderAvailable : Boolean
    )

    private val _uiState = MutableStateFlow(
        UiState(
            dealerHand = emptyList(),
            playerHand = emptyList(),
            status = Status.START,
            stack = 0.0,
            sizeOfDeck = 0,
            isInsuranceOffered = false,
            isSplitAvailable = false,
            currentBet = 0.0,
            split = 0,
            isAmericanRules = isAmericanRules(),
            isDoubleAvailable = isDoubleAvailable(false),
            isSurrenderAvailable = isSurrenderAvailable(0)
        )
    )

    private val splits = Stack<API>()
    private var currentEngine: API = engine
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun changeRules(config: Config) {
        engine = API(config)
        currentBet = 0.0
        stackDelta = null
        splits.clear()
        currentEngine = engine
        this.config = config

        _uiState.value =
            UiState(
                dealerHand = emptyList(),
                playerHand = emptyList(),
                status = Status.START,
                stack = 0.0,
                sizeOfDeck = 0,
                isInsuranceOffered = false,
                isSplitAvailable = false,
                currentBet = 0.0,
                split = 0,
                isAmericanRules = isAmericanRules(),
                isDoubleAvailable = isDoubleAvailable(false),
                isSurrenderAvailable = isSurrenderAvailable(0)
            )
    }

    fun stopGame() {

        if (splits.isNotEmpty()) {
            viewModelScope.launch {
                delay(500.milliseconds)
                currentEngine = splits.pop()
                update(currentEngine.currentResponse)
            }
        } else {
            currentBet = 0.0
            stackDelta = null
            _uiState.update { currentState ->
                currentState.copy(
                    currentBet = 0.0,
                    status = Status.STOP
                )
            }
        }
    }

    fun addStack(amount: Double) {
        stack += amount

        _uiState.update { currentState ->
            currentState.copy(
                stack = stack,
                status = Status.STOP
            )
        }
    }

    fun startGame(bet: Double) {
        if (stack < bet || bet <= 0) {
            _uiState.update { currentState -> currentState.copy(status = Status.ERROR) }
            return
        }

        currentBet = bet

        val response = currentEngine.newGame(bet)

        response.win?.let {
            if (splits.isNotEmpty())
                stackDelta = if (stackDelta != null) stackDelta!! + it else it
            else {
                stack += if (stackDelta == null) it else stackDelta!! + it
                currentBet = 0.0
            }
        }

        update(response)
    }

    fun hit() {
        var response = currentEngine.hit()

        response.win?.let {
            if (splits.isNotEmpty())
                stackDelta = if (stackDelta != null) stackDelta!! + it else it
            else {
                stack += if (stackDelta == null) it else stackDelta!! + it
                currentBet = 0.0
            }
        }

        if ((splits.isNotEmpty() || stackDelta != null) &&
            response.state.status == Status.DEALER_IS_TOO_MUCH
        ) {

            response = API.Response(
                Engine.State(
                    listOf(response.state.dealer[0]),
                    response.state.player,
                    Status.CONTINUE
                ),
                response.insuranceIsOffered,
                null,
                response.deckSize
            )
        }

        update(response)
    }

    fun double() {
        if (stack < currentBet) return

        currentBet *= 2

        var response = currentEngine.doubleBet()

        response.win?.let {
            if (splits.isNotEmpty())
                stackDelta = if (stackDelta != null) stackDelta!! + it else it
            else {
                stack += if (stackDelta == null) it else stackDelta!! + it
                currentBet = 0.0
            }
        }

        if (splits.isNotEmpty()) {
            response = API.Response(
                Engine.State(
                    listOf(response.state.dealer[0]),
                    response.state.player,
                    response.state.status
                ),
                response.insuranceIsOffered,
                null,
                response.deckSize
            )
        }

        update(response)
    }

    fun stand() {
        var response = currentEngine.stand()

        response.win?.let {
            if (splits.isNotEmpty())
                stackDelta = if (stackDelta != null) stackDelta!! + it else it
            else {
                stack += if (stackDelta == null) it else stackDelta!! + it
                currentBet = 0.0
            }
        }

        if (splits.isNotEmpty()) {
            response = API.Response(
                Engine.State(
                    listOf(response.state.dealer[0]),
                    response.state.player,
                    response.state.status
                ),
                response.insuranceIsOffered,
                null,
                response.deckSize
            )
        } else {
            _uiState.update { it.copy(status = Status.WAITING) }
            for (i in 1 until response.state.dealer.size) {
                val newDealerHand = _uiState.value.dealerHand!!.toMutableList()
                newDealerHand.add(response.state.dealer[i])
                _uiState.update { it.copy(dealerHand = newDealerHand.toList()) }
            }
        }

        update(response)
    }

    fun surrender() {
        var response: API.Response

        try {
            response = currentEngine.surrender()
        } catch (e: IllegalStateException) {
            return
        }

        response.win?.let {
            if (splits.isNotEmpty())
                stackDelta = if (stackDelta != null) stackDelta!! + it else it
            else {
                stack += if (stackDelta == null) it else stackDelta!! + it
                currentBet = 0.0
            }
        }

        update(response)
    }

    fun insurance() {
        val response = currentEngine.makeInsurance()

        currentBet *= 1.5

        response.win?.let {
            if (splits.isNotEmpty())
                stackDelta = if (stackDelta != null) stackDelta!! + it else it
            else {
                stack += if (stackDelta == null) it else stackDelta!! + it
                currentBet = 0.0
            }
        }

        update(response)
    }

    fun skipInsurance() {
        val res = engine.currentResponse
        update(res)
    }

    fun split() {
        if (!_uiState.value.isSplitAvailable)
            return

        val tmpEngine = currentEngine.split()

        splits.push(API(currentEngine))
        currentEngine = tmpEngine

        update(tmpEngine.currentResponse)
    }

    fun skipSplit() {
        _uiState.update { it.copy(isSplitAvailable = false) }
    }

    fun viewConfig() : Config?{
        return this.config
    }

    private fun update(response: API.Response) {

        val isSplitAvailable = (response.state.player.size == 2
                && response.state.player[0].value == response.state.player[1].value
                )

        _uiState.value = UiState(
            dealerHand = if (response.state.status == Status.CONTINUE || response.state.status == Status.WAITING)
                listOf(response.state.dealer[0]) else response.state.dealer,
            playerHand = response.state.player,
            status = response.state.status,
            stack = stack,
            sizeOfDeck = response.deckSize,
            isInsuranceOffered = response.insuranceIsOffered,
            isSplitAvailable = isSplitAvailable,
            currentBet = currentBet,
            split = splits.size,
            isAmericanRules = isAmericanRules(),
            isDoubleAvailable = isDoubleAvailable(response.state.player.isNotEmpty()),
            isSurrenderAvailable = isSurrenderAvailable(response.state.player.size)
        )
    }

    private fun isDoubleAvailable(hasActiveHand: Boolean): Boolean {
        return hasActiveHand && currentEngine.isDoubleAvailable
    }

    private fun isAmericanRules(): Boolean {
        return if (config == null)
            false
        else
            config!!.hideCardRules == HideCard.AMERICAN
    }

    private fun isSurrenderAvailable(playerHandSize: Int): Boolean {
        return playerHandSize == 2 && config?.surrender != Surrender.NO_SURRENDER
    }
}