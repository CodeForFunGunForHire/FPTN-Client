package com.filantrop.pvnclient.auth.domain.di

import com.filantrop.pvnclient.auth.domain.AuthInteractor
import com.filantrop.pvnclient.auth.domain.AuthInteractorImpl
import org.koin.dsl.module

val authDomainModule =
    module {
        single<AuthInteractor> { AuthInteractorImpl(get()) }
    }
