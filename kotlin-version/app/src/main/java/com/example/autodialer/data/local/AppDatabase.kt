package com.example.autodialer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.autodialer.data.local.dao.AudioFileDao
import com.example.autodialer.data.local.dao.CallLogDao
import com.example.autodialer.data.local.entity.AudioFileEntity
import com.example.autodialer.data.local.entity.CallLogEntity

@Database(
    entities = [AudioFileEntity::class, CallLogEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioFileDao(): AudioFileDao
    abstract fun callLogDao(): CallLogDao
}
