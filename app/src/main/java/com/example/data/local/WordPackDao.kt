package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.WordPack
import kotlinx.coroutines.flow.Flow

@Dao
interface WordPackDao {
    @Query("SELECT * FROM word_packs")
    fun getAllWordPacks(): Flow<List<WordPack>>

    @Query("SELECT * FROM word_packs WHERE id = :id")
    suspend fun getWordPackById(id: String): WordPack?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWordPacks(packs: List<WordPack>)

    @Query("DELETE FROM word_packs")
    suspend fun clearWordPacks()
}
