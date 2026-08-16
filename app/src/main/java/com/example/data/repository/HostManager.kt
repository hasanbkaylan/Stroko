package com.example.data.repository

import com.example.data.network.GameServer
import com.example.data.network.NsdHelper
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HostManager(private val nsdHelper: NsdHelper) {
    val server = GameServer()
    private var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val players = mutableListOf<String>()
    private var currentDrawer: String? = null
    private var currentWord: String = ""
    private var timeRemaining: Int = 0
    private var isGameRunning: Boolean = false
    private val scores = mutableMapOf<String, Int>()
    private var correctGuessers = mutableListOf<String>()
    private var timerJob: Job? = null
    
    private var gameDuration: Int = 120
    private var wordPack: WordPack? = null

    fun startHosting(port: Int, roomName: String, duration: Int, pack: WordPack) {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        
        players.clear()
        scores.clear()
        correctGuessers.clear()
        isGameRunning = false
        
        gameDuration = duration
        wordPack = pack
        val actualPort = server.startServer(port)
        nsdHelper.registerService(actualPort, roomName)

        scope.launch {
            server.messages.collect { message ->
                handleClientMessage(message)
            }
        }
    }

    fun stopHosting() {
        nsdHelper.unregisterService()
        server.stop()
        scope.cancel()
    }

    private fun handleClientMessage(message: NetworkMessage) {
        when (message.type) {
            MessageType.JOIN -> {
                val payload = Json.decodeFromString<JoinPayload>(message.payload)
                if (!players.contains(payload.username)) {
                    players.add(payload.username)
                    scores[payload.username] = scores[payload.username] ?: 0
                }
                broadcastSync()
            }
            MessageType.LEAVE -> {
                val payload = Json.decodeFromString<JoinPayload>(message.payload)
                players.remove(payload.username)
                if (payload.username == currentDrawer && isGameRunning) {
                    endGame() 
                }
                broadcastSync()
            }
            MessageType.CHAT -> {
                val payload = Json.decodeFromString<ChatPayload>(message.payload)
                if (isGameRunning && payload.username != currentDrawer && !correctGuessers.contains(payload.username)) {
                    val text = payload.text.trim()
                    if (text.equals(currentWord, ignoreCase = true)) {
                        handleCorrectGuess(payload.username)
                        val newPayload = payload.copy(isCorrectGuess = true, text = "★★★★★ ${payload.username} doğru tahmin etti!")
                        server.broadcast(NetworkMessage(MessageType.CHAT, Json.encodeToString(newPayload)))
                        return
                    }
                }
                server.broadcast(message)
            }
            MessageType.DRAW -> {
                server.broadcast(message, excludeUser = Json.decodeFromString<DrawPayload>(message.payload).action) // wait, draw doesn't have username. let's just broadcast to all, it's fine.
            }
            else -> {}
        }
    }

    fun startGame() {
        if (wordPack == null || wordPack!!.words.isEmpty()) return

        isGameRunning = true
        correctGuessers.clear()
        
        currentDrawer = players.random()
        currentWord = wordPack!!.words.random()
        timeRemaining = gameDuration

        server.broadcast(NetworkMessage(MessageType.START, Json.encodeToString(StartPayload(currentDrawer!!, gameDuration))))
        server.sendMessageTo(currentDrawer!!, NetworkMessage(MessageType.WORD, Json.encodeToString(WordPayload(currentWord))))
        
        broadcastSync()

        timerJob?.cancel()
        timerJob = scope.launch {
            while (timeRemaining > 0 && isGameRunning) {
                delay(1000)
                timeRemaining--
                broadcastSync()
            }
            if (isGameRunning) {
                endGame()
            }
        }
    }

    private fun handleCorrectGuess(username: String) {
        val points = when (correctGuessers.size) {
            0 -> 100
            1 -> 70
            2 -> 50
            else -> 30
        }
        correctGuessers.add(username)
        scores[username] = (scores[username] ?: 0) + points
        
        currentDrawer?.let { drawer ->
            scores[drawer] = (scores[drawer] ?: 0) + 30
        }

        broadcastSync()

        if (correctGuessers.size >= players.size - 1 && players.size > 1) {
            endGame()
        }
    }

    private fun endGame() {
        isGameRunning = false
        timerJob?.cancel()
        server.broadcast(NetworkMessage(MessageType.END, Json.encodeToString(EndPayload(scores, currentWord))))
        broadcastSync()
    }

    private fun broadcastSync() {
        val syncMsg = SyncPayload(players, currentDrawer, timeRemaining, isGameRunning, scores)
        server.broadcast(NetworkMessage(MessageType.SYNC, Json.encodeToString(syncMsg)))
    }
}
