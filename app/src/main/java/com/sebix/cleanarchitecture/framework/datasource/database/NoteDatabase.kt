package com.sebix.cleanarchitecture.framework.datasource.database

import androidx.room.Database
import com.sebix.cleanarchitecture.framework.datasource.cache.model.NoteCacheEntity

@Database(entities = [NoteCacheEntity::class], version = 1)
abstract class NoteDatabase {
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "note_db"
    }
}