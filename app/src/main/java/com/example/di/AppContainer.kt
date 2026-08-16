package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsRepository
import com.example.data.network.NsdHelper
import com.example.data.repository.HostManager
import com.example.data.repository.WordPackRepository

class AppContainer(private val context: Context) {
    val database by lazy { AppDatabase.getDatabase(context) }
    val wordPackRepository by lazy { WordPackRepository(database.wordPackDao()) }
    val settingsRepository by lazy { SettingsRepository(context) }
    val nsdHelper by lazy { NsdHelper(context) }
    val hostManager by lazy { HostManager(nsdHelper) }
}
