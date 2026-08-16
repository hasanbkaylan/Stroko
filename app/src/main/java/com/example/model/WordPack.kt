package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "word_packs")
data class WordPack(
    @PrimaryKey val id: String,
    val name: String,
    val language: String,
    val version: Int,
    val author: String,
    val description: String,
    val words: List<String>
)

@Serializable
data class WordPackIndex(
    val version: Int,
    val languages: Map<String, List<String>>
)
