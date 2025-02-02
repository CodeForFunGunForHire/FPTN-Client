package com.filantrop.pvnclient.home.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute // route to Home screen

const val HOME_ROUTE = "homeRoute"

@Serializable data object HomeBaseRoute // route to base navigation graph

const val HOME_BASE_ROUTE = "homeRouteStr"

fun NavController.navigateToHome(navOptions: NavOptions) = navigate(route = HomeRoute, navOptions)
