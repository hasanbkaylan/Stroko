package com.example.data.repository

import com.example.data.local.WordPackDao
import com.example.model.WordPack
import com.example.model.WordPackIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class WordPackRepository(private val wordPackDao: WordPackDao) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    val allWordPacks: Flow<List<WordPack>> = wordPackDao.getAllWordPacks()

    suspend fun fetchAndSaveWordPacks() {
        withContext(Dispatchers.IO) {
            try {
                val indexRequest = Request.Builder()
                    .url("https://raw.githubusercontent.com/hasanbkaylan/StrokoWords/main/index.json")
                    .build()
                val indexResponse = client.newCall(indexRequest).execute()
                if (!indexResponse.isSuccessful) return@withContext
                
                val indexBody = indexResponse.body?.string() ?: return@withContext
                val index = json.decodeFromString<WordPackIndex>(indexBody)

                val fetchedPacks = mutableListOf<WordPack>()
                for ((language, files) in index.languages) {
                    for (file in files) {
                        val packRequest = Request.Builder()
                            .url("https://raw.githubusercontent.com/hasanbkaylan/StrokoWords/main/$file")
                            .build()
                        val packResponse = client.newCall(packRequest).execute()
                        if (packResponse.isSuccessful) {
                            val packBody = packResponse.body?.string() ?: continue
                            val pack = json.decodeFromString<WordPack>(packBody)
                            
                            val existing = wordPackDao.getWordPackById(pack.id)
                            if (existing == null || existing.version < pack.version) {
                                fetchedPacks.add(pack)
                            }
                        }
                    }
                }
                
                if (fetchedPacks.isNotEmpty()) {
                    wordPackDao.insertWordPacks(fetchedPacks)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
