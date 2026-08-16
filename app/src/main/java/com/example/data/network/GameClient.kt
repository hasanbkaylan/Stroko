package com.example.data.network

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class GameClient {
    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 100)
    val messages = _messages.asSharedFlow()

    fun connect(host: String, port: Int, username: String) {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scope.launch {
            try {
                socket = Socket(host, port)
                writer = PrintWriter(socket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

                val joinMsg = NetworkMessage(
                    type = MessageType.JOIN,
                    payload = Json.encodeToString(JoinPayload(username))
                )
                sendMessage(joinMsg)

                while (isActive) {
                    val line = reader.readLine() ?: break
                    val message = Json.decodeFromString<NetworkMessage>(line)
                    _messages.emit(message)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                disconnect()
            }
        }
    }

    fun sendMessage(message: NetworkMessage) {
        scope.launch {
            try {
                writer?.println(Json.encodeToString(message))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        scope.cancel()
        socket?.close()
        writer = null
    }
}
