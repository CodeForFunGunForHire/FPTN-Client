package com.filantrop.pvnclient.auth.data.di

import com.filantrop.pvnclient.auth.data.AuthRepositoryImpl
import com.filantrop.pvnclient.auth.domain.AuthRepository
import org.koin.dsl.module

val authDataModule =
    module {
        single<AuthRepository> { AuthRepositoryImpl(get()) }
    }
