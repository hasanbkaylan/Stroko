package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.network.GameClient
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class GameUiState(
    val isConnected: Boolean = false,
    val players: List<String> = emptyList(),
    val currentDrawer: String? = null,
    val isGameRunning: Boolean = false,
    val timeRemaining: Int = 0,
    val myWord: String? = null, 
    val scores: Map<String, Int> = emptyMap(),
    val chatMessages: List<ChatPayload> = emptyList(),
    val lastDrawEvent: DrawPayload? = null,
    val gameEnded: Boolean = false,
    val correctWord: String? = null
)

class GameViewModel(private val client: GameClient, val myUsername: String, val isHost: Boolean) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            client.messages.collect { message ->
                handleMessage(message)
            }
        }
    }

    private fun handleMessage(message: NetworkMessage) {
        when (message.type) {
            MessageType.SYNC -> {
                val payload = try { Json.decodeFromString<SyncPayload>(message.payload) } catch (e: Exception) { return }
                
                val wasGameEnded = _uiState.value.gameEnded
                _uiState.value = _uiState.value.copy(
                    players = payload.players,
                    currentDrawer = payload.currentDrawer,
                    timeRemaining = payload.timeRemaining,
                    isGameRunning = payload.isGameRunning,
                    scores = payload.scores,
                    isConnected = true
                )
                
                if (payload.isGameRunning && wasGameEnded) {
                    _uiState.value = _uiState.value.copy(
                        gameEnded = false,
                        correctWord = null,
                        myWord = null,
                        chatMessages = emptyList()
                    )
                }
            }
            MessageType.CHAT -> {
                val payload = try { Json.decodeFromString<ChatPayload>(message.payload) } catch (e: Exception) { return }
                val newList = _uiState.value.chatMessages.toMutableList()
                newList.add(payload)
                _uiState.value = _uiState.value.copy(chatMessages = newList)
            }
            MessageType.WORD -> {
                val payload = try { Json.decodeFromString<WordPayload>(message.payload) } catch (e: Exception) { return }
                _uiState.value = _uiState.value.copy(myWord = payload.word)
            }
            MessageType.DRAW -> {
                val payload = try { Json.decodeFromString<DrawPayload>(message.payload) } catch (e: Exception) { return }
                if (_uiState.value.currentDrawer != myUsername) {
                    _uiState.value = _uiState.value.copy(lastDrawEvent = payload)
                }
            }
            MessageType.START -> {
                _uiState.value = _uiState.value.copy(
                    gameEnded = false,
                    correctWord = null,
                    myWord = null,
                    chatMessages = emptyList()
                )
            }
            MessageType.END -> {
                val payload = try { Json.decodeFromString<EndPayload>(message.payload) } catch (e: Exception) { return }
                _uiState.value = _uiState.value.copy(
                    gameEnded = true,
                    correctWord = payload.correctWord,
                    scores = payload.scores
                )
            }
            else -> {}
        }
    }

    fun sendChat(text: String) {
        if (text.isBlank()) return
        val msg = NetworkMessage(MessageType.CHAT, Json.encodeToString(ChatPayload(myUsername, text, false)))
        client.sendMessage(msg)
    }

    fun sendDrawEvent(event: DrawPayload) {
        if (_uiState.value.currentDrawer == myUsername) {
            val msg = NetworkMessage(MessageType.DRAW, Json.encodeToString(event))
            client.sendMessage(msg)
        }
    }

    fun disconnect() {
        client.disconnect()
    }
}
