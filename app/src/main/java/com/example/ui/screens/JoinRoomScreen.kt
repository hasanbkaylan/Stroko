package com.example.ui.screens

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.local.SettingsRepository
import com.example.data.network.NsdHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomScreen(
    nsdHelper: NsdHelper,
    settingsRepository: SettingsRepository,
    onBack: () -> Unit,
    onJoin: (String, Int, String) -> Unit
) {
    val username by settingsRepository.usernameFlow.collectAsState(initial = "Player")
    val services by nsdHelper.discoverServices().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Odaya Katıl") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Force recompose/restart flow if needed, but flow is continuous */ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Yenile")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Aynı Wi-Fi ağındaki odalar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            if (services.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Odalar aranıyor...", modifier = Modifier.padding(top = 64.dp))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(services, key = { it.serviceName }) { service ->
                        RoomCard(service = service, onClick = {
                            // On newer APIs, host is an InetAddress.
                            val hostAddress = service.host?.hostAddress ?: "127.0.0.1"
                            onJoin(hostAddress, service.port, username)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun RoomCard(service: NsdServiceInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = service.serviceName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Button(onClick = onClick) {
                Text("Katıl")
            }
        }
    }
}
