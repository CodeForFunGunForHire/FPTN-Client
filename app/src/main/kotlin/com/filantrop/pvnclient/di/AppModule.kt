package com.filantrop.pvnclient.di

import com.filantrop.pvnclient.auth.ui.authModule
import com.filantrop.pvnclient.core.common.commonModule
import com.filantrop.pvnclient.core.network.networkModule
import com.filantrop.pvnclient.core.persistent.di.persistentModule
import com.filantrop.pvnclient.viewmodel.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        viewModel { MainViewModel(get()) }
    }

val appModule: Module =
    module {
        includes(
            authModule,
            commonModule,
            persistentModule,
            networkModule,
            viewModelModule,
        )
    }
