package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.network.GameClient
import com.example.di.AppContainer
import com.example.model.DrawPayload
import com.example.ui.navigation.Game
import com.example.viewmodel.GameViewModel

class GameViewModelFactory(private val client: GameClient, private val username: String, private val isHost: Boolean) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(client, username, isHost) as T
    }
}

@Composable
fun GameScreenWrapper(appContainer: AppContainer, args: Game, onNavigateMain: () -> Unit) {
    val client = remember { GameClient() }
    
    DisposableEffect(Unit) {
        client.connect(args.hostAddress, args.port, args.username)
        onDispose {
            client.disconnect()
            if (args.isHost) {
                appContainer.hostManager.stopHosting()
            }
        }
    }

    val viewModel: GameViewModel = viewModel(factory = GameViewModelFactory(client, args.username, args.isHost))
    GameScreen(viewModel = viewModel, hostManager = appContainer.hostManager, onLeave = onNavigateMain)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(viewModel: GameViewModel, hostManager: com.example.data.repository.HostManager, onLeave: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val isHost = viewModel.isHost
    val isDrawer = uiState.currentDrawer == viewModel.myUsername

    if (uiState.gameEnded) {
        ResultScreen(uiState.scores, uiState.correctWord ?: "", isHost, { hostManager.startGame() }, onLeave)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isDrawer && uiState.myWord != null) {
                        Text("Çiz: ${uiState.myWord}")
                    } else if (uiState.isGameRunning) {
                        Text("Tahmin Et! (${uiState.currentDrawer} çiziyor)")
                    } else {
                        Text("Lobi")
                    }
                },
                actions = {
                    Text("Süre: ${uiState.timeRemaining}", modifier = Modifier.padding(end = 16.dp))
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (!uiState.isGameRunning) {
                LobbyView(uiState.players, isHost, { hostManager.startGame() }, onLeave)
            } else {
                // Game View
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp)) {
                    DrawingArea(viewModel = viewModel, isDrawer = isDrawer, lastDrawEvent = uiState.lastDrawEvent)
                }
                
                if (isDrawer) {
                    DrawingTools(viewModel)
                }

                // Chat Area
                ChatArea(viewModel, isDrawer, Modifier.weight(0.6f))
            }
        }
    }
}

@Composable
fun LobbyView(players: List<String>, isHost: Boolean, onStart: () -> Unit, onLeave: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Oyuncular Bekleniyor", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        players.forEach {
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (isHost) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Oyunu Başlat")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Odadan Ayrıl")
        }
    }
}

data class Line(val path: Path, val color: Color, val strokeWidth: Float)

@Composable
fun DrawingArea(viewModel: GameViewModel, isDrawer: Boolean, lastDrawEvent: DrawPayload?) {
    val lines = remember { mutableStateListOf<Line>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStroke by remember { mutableStateOf(10f) }

    // Sync remote draws
    LaunchedEffect(lastDrawEvent) {
        lastDrawEvent?.let { event ->
            if (event.type == "clear") {
                lines.clear()
            } else if (event.type == "undo") {
                if (lines.isNotEmpty()) lines.removeLast()
            } else if (event.type == "path") {
                val color = Color(event.color)
                when (event.action) {
                    "down" -> {
                        currentPath = Path().apply { moveTo(event.x, event.y) }
                        currentColor = color
                        currentStroke = event.strokeWidth
                    }
                    "move" -> {
                        currentPath?.lineTo(event.x, event.y)
                    }
                    "up" -> {
                        currentPath?.let { lines.add(Line(it, currentColor, currentStroke)) }
                        currentPath = null
                    }
                }
            }
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White, RoundedCornerShape(8.dp))
            .pointerInput(isDrawer) {
                if (!isDrawer) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = Path().apply { moveTo(offset.x, offset.y) }
                        viewModel.sendDrawEvent(DrawPayload("path", offset.x, offset.y, currentColor.toArgb(), currentStroke, "down"))
                    },
                    onDragEnd = {
                        currentPath?.let { lines.add(Line(it, currentColor, currentStroke)) }
                        currentPath = null
                        viewModel.sendDrawEvent(DrawPayload("path", action = "up"))
                    },
                    onDragCancel = {
                        currentPath?.let { lines.add(Line(it, currentColor, currentStroke)) }
                        currentPath = null
                        viewModel.sendDrawEvent(DrawPayload("path", action = "up"))
                    }
                ) { change, _ ->
                    currentPath?.lineTo(change.position.x, change.position.y)
                    viewModel.sendDrawEvent(DrawPayload("path", change.position.x, change.position.y, currentColor.toArgb(), currentStroke, "move"))
                }
            }
    ) {
        lines.forEach { line ->
            drawPath(
                path = line.path,
                color = line.color,
                style = Stroke(width = line.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        currentPath?.let { path ->
            drawPath(
                path = path,
                color = currentColor,
                style = Stroke(width = currentStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun DrawingTools(viewModel: GameViewModel) {
    // Basic tools for drawer
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = { viewModel.sendDrawEvent(DrawPayload("undo")) }) {
            Icon(Icons.Default.Undo, "Geri Al")
        }
        IconButton(onClick = { viewModel.sendDrawEvent(DrawPayload("clear")) }) {
            Icon(Icons.Default.Clear, "Temizle")
        }
        // Colors could be added here
    }
}

@Composable
fun ChatArea(viewModel: GameViewModel, isDrawer: Boolean, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(uiState.chatMessages) { msg ->
                val color = if (msg.isCorrectGuess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
                Text("${msg.username}: ${msg.text}", color = color, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
        
        if (!isDrawer) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tahmin et...") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        viewModel.sendChat(text)
                        text = ""
                    })
                )
                IconButton(onClick = {
                    viewModel.sendChat(text)
                    text = ""
                }) {
                    Icon(Icons.Default.Send, "Gönder")
                }
            }
        }
    }
}

@Composable
fun ResultScreen(scores: Map<String, Int>, word: String, isHost: Boolean, onNewGame: () -> Unit, onLeave: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Oyun Bitti!", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Kelime: $word", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        
        scores.entries.sortedByDescending { it.value }.forEachIndexed { index, entry ->
            Text("${index + 1}. ${entry.key} - ${entry.value} Puan", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        if (isHost) {
            Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Yeni Oyun")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(onClick = onLeave, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("Odadan Ayrıl")
        }
    }
}
