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
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class GameServer {
    private var serverSocket: ServerSocket? = null
    private val clients = ConcurrentHashMap<String, PrintWriter>()
    private val clientSockets = ConcurrentHashMap<String, Socket>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messages = MutableSharedFlow<NetworkMessage>(extraBufferCapacity = 100)
    val messages = _messages.asSharedFlow()

    fun startServer(port: Int): Int {
        serverSocket = ServerSocket(port)
        val actualPort = serverSocket!!.localPort
        
        scope.launch {
            try {
                while (isActive) {
                    val socket = serverSocket!!.accept()
                    handleClient(socket)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return actualPort
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            var username: String? = null
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                while (isActive) {
                    val line = reader.readLine() ?: break
                    val message = Json.decodeFromString<NetworkMessage>(line)
                    
                    if (message.type == MessageType.JOIN) {
                        val payload = Json.decodeFromString<JoinPayload>(message.payload)
                        username = payload.username
                        clients[username] = writer
                        clientSockets[username] = socket
                        _messages.emit(message)
                    } else {
                        _messages.emit(message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                username?.let {
                    clients.remove(it)
                    clientSockets.remove(it)
                    _messages.emit(NetworkMessage(MessageType.LEAVE, Json.encodeToString(JoinPayload(it))))
                }
                socket.close()
            }
        }
    }

    fun broadcast(message: NetworkMessage, excludeUser: String? = null) {
        val jsonString = Json.encodeToString(message)
        clients.forEach { (user, writer) ->
            if (user != excludeUser) {
                scope.launch {
                    try {
                        writer.println(jsonString)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun sendMessageTo(username: String, message: NetworkMessage) {
        val jsonString = Json.encodeToString(message)
        scope.launch {
            clients[username]?.println(jsonString)
        }
    }

    fun stop() {
        scope.cancel()
        serverSocket?.close()
        clientSockets.values.forEach { it.close() }
        clients.clear()
        clientSockets.clear()
    }
}
