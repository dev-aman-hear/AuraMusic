package com.aman.auramusic.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Online : Screen("online")
    object Library : Screen("library")
    object Search : Screen("search")
    object Player : Screen("player")
}