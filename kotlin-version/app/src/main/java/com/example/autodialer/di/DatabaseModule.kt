package com.example.autodialer.di

import android.content.Context
import androidx.room.Room
import com.example.autodialer.data.local.AppDatabase
import com.example.autodialer.data.local.dao.AudioFileDao
import com.example.autodialer.data.local.dao.CallLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "autodialer.db"
    ).build()

    @Provides
    @Singleton
    fun provideAudioFileDao(db: AppDatabase): AudioFileDao = db.audioFileDao()

    @Provides
    @Singleton
    fun provideCallLogDao(db: AppDatabase): CallLogDao = db.callLogDao()
}
