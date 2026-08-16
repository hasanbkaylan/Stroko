package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object Splash
@Serializable data object Main
@Serializable data object CreateRoom
@Serializable data object JoinRoom
@Serializable data class Game(val hostAddress: String, val port: Int, val username: String, val isHost: Boolean)
@Serializable data object WordPacks
@Serializable data object Settings
@Serializable data object About
