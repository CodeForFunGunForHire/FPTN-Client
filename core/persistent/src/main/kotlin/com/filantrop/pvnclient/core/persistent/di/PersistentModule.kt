package com.filantrop.pvnclient.core.persistent.di

import com.filantrop.pvnclient.core.persistent.PreferenceStore
import com.filantrop.pvnclient.core.persistent.PreferencesStoreImpl
import org.koin.dsl.module

val persistentModule =
    module {
        single<PreferenceStore> { PreferencesStoreImpl(get()) }
    }
