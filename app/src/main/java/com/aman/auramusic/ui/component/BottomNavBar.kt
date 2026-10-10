package com.aman.auramusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.ui.theme.GlassLevel
import com.aman.auramusic.ui.theme.LocalIsDark
import com.aman.auramusic.ui.theme.liquidGlass

enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("Home", Icons.Default.Home, Icons.Default.Home),
    Online("Explore", Icons.Default.Sensors, Icons.Default.Sensors),
    Library("Library", Icons.Default.LibraryMusic, Icons.Default.LibraryMusic),
    Search("Search", Icons.Default.Search, Icons.Default.Search);

    companion object {
        val ListenNow = Home
    }
}

/**
 * Floating Liquid Glass Navigation Bar inspired by iOS & Apple Music.
 * Features a dual floating surface system:
 * 1. A floating glass capsule containing the main navigation tabs with animated active selection pill.
 * 2. An attached floating circular glass orb for Instant Search.
 */
@Composable
fun BottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val isDark = LocalIsDark.current
    val shadowElevation = if (isDark) 16.dp else 10.dp
    val primaryColor = com.aman.auramusic.ui.theme.AppleMusicAccent
    val allTabs = remember { listOf(AppTab.Home, AppTab.Online, AppTab.Library, AppTab.Search) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 2.dp)
            .height(64.dp)
            .liquidGlass(
                shape = RoundedCornerShape(32.dp),
                level = GlassLevel.UltraThin,
                isDark = isDark,
                elevation = shadowElevation,
                borderWidth = 1.2.dp,
                sheenAlpha = if (isDark) 0.22f else 0.44f
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            allTabs.forEach { tab ->
                val selected = selectedTab == tab
                val tabTint by animateColorAsState(
                    targetValue = if (selected) {
                        if (isDark) Color.White else primaryColor
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF8E8E93)
                    },
                    animationSpec = tween(220),
                    label = "tabTint_${tab.name}"
                )
                val activePillBg by animateColorAsState(
                    targetValue = if (selected) {
                        if (isDark) primaryColor.copy(alpha = 0.85f) else primaryColor.copy(alpha = 0.14f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(220),
                    label = "pillBg_${tab.name}"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(activePillBg)
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                            tint = tabTint,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = tabTint,
                            maxLines = 1,
                            letterSpacing = 0.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Liquid Glass Navigation Bar.
 */
@Composable
fun LiquidGlassNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) = BottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
