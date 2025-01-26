package com.filantrop.pvnclient.core.network

import org.koin.dsl.module

val networkModule =
    module {
        single<NetworkMonitor> { ConnectivityManagerNetworkMonitor(get(), get()) }
    }
