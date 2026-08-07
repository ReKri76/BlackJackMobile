package io.rekri.blackjackmobile.ui

import API
import androidx.lifecycle.ViewModel
import api.Status
import card.Card
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class GameViewModel : ViewModel() {
    private val engine = API()
    private var stack : Double = 0.0
    private var currentBet : Double = 0.0

    data class UiState(
        val dealerHand : List<Card>?,
        val playerHand : List<Card>,
        val status : Status,
        val stack : Double,
        val sizeOfDeck : Int?,
        val isInsuranceOffered : Boolean,
        val isSplitAvailable: Boolean,
        val currentBet : Double
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
            currentBet = 0.0
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun stopGame() {
        currentBet = 0.0
        _uiState.update { currentState ->
            currentState.copy(
                currentBet = 0.0,
                status = Status.STOP
            )
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

    fun startGame(bet: Double){
        if (stack < bet || bet<=0) {
            _uiState.update { currentState -> currentState.copy(status = Status.ERROR)}
            return
        }

        currentBet = bet

        val response = engine.newGame(bet)

        response.win?.let {
            stack += it
            currentBet = 0.0
        }

        update(response)
    }

    fun hit(){
        val response = engine.hit()

        response.win?.let {
            stack+=it
            currentBet=0.0
        }

        update(response)
    }

    fun double(){
        if (stack < currentBet) return

        stack -= currentBet
        currentBet *= 2

        val response = engine.doubleBet()

        response.win?.let {
            stack += it
            currentBet = 0.0
        }

        update(response)
    }

    fun stand(){
        val response = engine.stand()

            _uiState.update { it.copy(status = Status.WAITING) }
            for (i in 1 until response.state.dealer.size){
                val newDealerHand = _uiState.value.dealerHand!!.toMutableList()
                newDealerHand.add(response.state.dealer[i])
                _uiState.update { it.copy(dealerHand = newDealerHand.toList()) }

            }

        response.win?.let {
            stack += it
            currentBet = 0.0
        }

        update(response)
    }

    fun surrender(){
        var response : API.Response

        try {
            response = engine.surrender()
        } catch (e : IllegalStateException){
            return
        }

        response.win?.let {
            stack += it
            currentBet = 0.0
        }

        update(response)
    }

    fun insurance(){
        val response = engine.makeInsurance()

        currentBet*=1.5

        update(response)
    }

    fun skipInsurance() {
        _uiState.update { it.copy(isInsuranceOffered = false) }
    }

    private fun update(response : API.Response){

        val isSplitAvailable = (response.state.player[0].value.value ==
                response.state.player[1].value.value)

        _uiState.value = UiState(
            response.state.dealer,
            response.state.player,
            response.state.status,
            stack,
            response.deckSize,
            response.insuranceIsOffered,
            isSplitAvailable,
            currentBet
        )
    }
}