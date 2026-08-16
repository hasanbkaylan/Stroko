package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkMessage(
    val type: MessageType,
    val payload: String // JSON string
)

enum class MessageType {
    JOIN, LEAVE, CHAT, START, END, DRAW, SYNC, WORD
}

@Serializable
data class JoinPayload(val username: String)

@Serializable
data class ChatPayload(val username: String, val text: String, val isCorrectGuess: Boolean)

@Serializable
data class StartPayload(val drawerUsername: String, val duration: Int)

@Serializable
data class WordPayload(val word: String) // Sent only to the drawer

@Serializable
data class EndPayload(val scores: Map<String, Int>, val correctWord: String)

@Serializable
data class DrawPayload(
    val type: String, // "path", "clear", "undo"
    val x: Float = 0f,
    val y: Float = 0f,
    val color: Int = 0,
    val strokeWidth: Float = 0f,
    val action: String = "" // "down", "move", "up"
)

@Serializable
data class SyncPayload(
    val players: List<String>,
    val currentDrawer: String?,
    val timeRemaining: Int,
    val isGameRunning: Boolean,
    val scores: Map<String, Int>
)
