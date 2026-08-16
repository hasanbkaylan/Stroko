package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repository.HostManager
import com.example.data.local.SettingsRepository
import com.example.data.repository.WordPackRepository
import com.example.model.WordPack
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomScreen(
    hostManager: HostManager,
    wordPackRepository: WordPackRepository,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onGameCreated: (Int, String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val username by settingsRepository.usernameFlow.collectAsState(initial = "Host")
    val packs by wordPackRepository.allWordPacks.collectAsState(initial = emptyList())
    
    var selectedDuration by remember { mutableStateOf(120) }
    val durations = listOf(120, 180, 240, 300, 360, 420, 480, 600)
    
    var selectedPack by remember { mutableStateOf<WordPack?>(null) }
    var durationExpanded by remember { mutableStateOf(false) }
    var packExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(packs) {
        if (selectedPack == null && packs.isNotEmpty()) {
            selectedPack = packs.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oda Oluştur") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Oda Adı", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${username}'in Odası", style = MaterialTheme.typography.bodyLarge)
                }
            }

            // Duration Dropdown
            ExposedDropdownMenuBox(
                expanded = durationExpanded,
                onExpandedChange = { durationExpanded = !durationExpanded }
            ) {
                OutlinedTextField(
                    value = "$selectedDuration saniye",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Oyun Süresi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = durationExpanded,
                    onDismissRequest = { durationExpanded = false }
                ) {
                    durations.forEach { duration ->
                        DropdownMenuItem(
                            text = { Text("$duration saniye") },
                            onClick = {
                                selectedDuration = duration
                                durationExpanded = false
                            }
                        )
                    }
                }
            }

            // Word Pack Dropdown
            ExposedDropdownMenuBox(
                expanded = packExpanded,
                onExpandedChange = { packExpanded = !packExpanded }
            ) {
                OutlinedTextField(
                    value = selectedPack?.name ?: "Paket Seçin",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kelime Paketi") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = packExpanded,
                    onDismissRequest = { packExpanded = false }
                ) {
                    if (packs.isEmpty()) {
                        DropdownMenuItem(text = { Text("Paket bulunamadı, önce indirin.") }, onClick = { packExpanded = false })
                    } else {
                        packs.forEach { pack ->
                            DropdownMenuItem(
                                text = { Text(pack.name) },
                                onClick = {
                                    selectedPack = pack
                                    packExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (selectedPack != null) {
                        coroutineScope.launch {
                            val port = Random.nextInt(10000, 60000)
                            hostManager.startHosting(port, "${username}'in Odası", selectedDuration, selectedPack!!)
                            onGameCreated(port, username)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedPack != null
            ) {
                Text("Oluştur ve Katıl", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
