package com.example.autodialer.di

import com.example.autodialer.data.repository.AudioRepositoryImpl
import com.example.autodialer.data.repository.CallRepositoryImpl
import com.example.autodialer.data.repository.DebtorRepositoryImpl
import com.example.autodialer.domain.repository.AudioRepository
import com.example.autodialer.domain.repository.CallRepository
import com.example.autodialer.domain.repository.DebtorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAudioRepository(
        impl: AudioRepositoryImpl
    ): AudioRepository

    @Binds
    @Singleton
    abstract fun bindDebtorRepository(
        impl: DebtorRepositoryImpl
    ): DebtorRepository

    @Binds
    @Singleton
    abstract fun bindCallRepository(
        impl: CallRepositoryImpl
    ): CallRepository
}
