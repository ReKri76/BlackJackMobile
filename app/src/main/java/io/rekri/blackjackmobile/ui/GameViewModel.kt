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
        val isSplitAvailable: Boolean
    )

    private val _uiState = MutableStateFlow<UiState?>(null)
    val uiState: StateFlow<UiState?> = _uiState.asStateFlow()

    fun addStack(bet : Double){
        stack+=bet
    }

    fun startGame(bet: Double){
        if (stack < bet || bet<=0) {
            _uiState.update { currentState -> currentState?.copy(status = Status.ERROR)}
            return
        }

        stack-=bet

        val response = engine.newGame(bet)

        response.win?.let {
            stack+=it
            currentBet=0.0
        }

        update(response)

        currentBet=bet
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
        val response = engine.doubleBet()

        stack-=currentBet
        currentBet*=2
        stack+=response.win!!
        currentBet=0.0

        update(response)
    }

    fun stand(){
        val response = engine.stand()

        stack+=response.win!!
        currentBet=0.0

        update(response)
    }

    fun surrender(){
        var response : API.Response

        try {
            response = engine.surrender()
        } catch (e : IllegalStateException){
            return
        }

        stack+=response.win!!
        currentBet=0.0

        update(response)
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
            isSplitAvailable
        )
    }
}