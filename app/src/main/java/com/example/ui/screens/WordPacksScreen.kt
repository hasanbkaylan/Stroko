package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repository.WordPackRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordPacksScreen(
    repository: WordPackRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val packs by repository.allWordPacks.collectAsState(initial = emptyList())
    var isDownloading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelime Paketleri") },
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
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isDownloading = true
                        repository.fetchAndSaveWordPacks()
                        isDownloading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isDownloading
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Paketleri İndir / Güncelle")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(packs, key = { it.id }) { pack ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(pack.name, style = MaterialTheme.typography.titleMedium)
                            Text(pack.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Kelime sayısı: ${pack.words.size} • Sürüm: ${pack.version}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
