package com.aman.auramusic.ui.navigation

sealed class Screen(val route: String) {

    object Library : Screen("library")

    object Player : Screen("player")
}