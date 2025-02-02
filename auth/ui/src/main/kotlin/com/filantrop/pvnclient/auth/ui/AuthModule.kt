package com.filantrop.pvnclient.auth.ui

import com.filantrop.pvnclient.auth.data.di.authDataModule
import com.filantrop.pvnclient.auth.domain.di.authDomainModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { AuthViewModel() }
    }

val authModule =
    module {
        includes(viewModelModule, authDataModule, authDomainModule)
    }
