package com.aman.auramusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aman.auramusic.ui.theme.LocalIsDark

enum class AppTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home("Home", Icons.Default.Home, Icons.Default.Home),
    Online("Online", Icons.Default.Sensors, Icons.Default.Sensors),
    Library("Library", Icons.Default.LibraryMusic, Icons.Default.LibraryMusic),
    Search("Search", Icons.Default.Search, Icons.Default.Search);

    companion object {
        val ListenNow = Home
    }
}

@Composable
fun BottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    val isDark = LocalIsDark.current
    val navBgColor = if (isDark) Color(0xD9181920) else Color.White.copy(alpha = 0.88f)
    val navBorderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.32f),
                Color.White.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color(0xFFD0D3DC).copy(alpha = 0.55f)
            )
        )
    }
    val shadowElevation = if (isDark) 14.dp else 8.dp
    val primaryColor = MaterialTheme.colorScheme.primary
    val isSearchSelected = selectedTab == AppTab.Search

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Floating Capsule for the 3 main tabs: Home, Online, Library
        Surface(
            color = navBgColor,
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .shadow(
                    elevation = shadowElevation,
                    shape = RoundedCornerShape(30.dp),
                    spotColor = if (isDark) Color.Black else Color(0x33000000)
                ),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, navBorderBrush)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val primaryTabs = listOf(AppTab.Home, AppTab.Online, AppTab.Library)
                primaryTabs.forEach { tab ->
                    val selected = selectedTab == tab
                    val tabTint by animateColorAsState(
                        targetValue = if (selected) {
                            primaryColor
                        } else {
                            if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF75757A)
                        },
                        animationSpec = tween(220),
                        label = "tabTint_${tab.name}"
                    )
                    val activePillBg by animateColorAsState(
                        targetValue = if (selected) {
                            primaryColor.copy(alpha = if (isDark) 0.20f else 0.15f)
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
                            .clip(RoundedCornerShape(22.dp))
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
                                modifier = Modifier.size(21.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = tabTint,
                                maxLines = 1,
                                letterSpacing = (-0.2).sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Separate Floating Circular Button for Search
        val searchBg by animateColorAsState(
            targetValue = if (isSearchSelected) {
                primaryColor.copy(alpha = if (isDark) 0.24f else 0.18f)
            } else {
                navBgColor
            },
            animationSpec = tween(220),
            label = "searchBg"
        )
        val searchTint by animateColorAsState(
            targetValue = if (isSearchSelected) {
                primaryColor
            } else {
                if (isDark) Color.White.copy(alpha = 0.80f) else Color(0xFF2C2C2E)
            },
            animationSpec = tween(220),
            label = "searchTint"
        )
        val searchBorder = if (isSearchSelected) {
            BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.50f))
        } else {
            BorderStroke(1.dp, navBorderBrush)
        }

        Surface(
            modifier = Modifier
                .size(58.dp)
                .shadow(
                    elevation = shadowElevation,
                    shape = CircleShape,
                    spotColor = if (isDark) Color.Black else Color(0x33000000)
                ),
            shape = CircleShape,
            color = searchBg,
            border = searchBorder
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable { onTabSelected(AppTab.Search) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search songs",
                    tint = searchTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
