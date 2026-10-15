package com.aman.auramusic.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

@Composable
fun BottomNavBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    compact: Boolean = false
) {
    val allTabs = remember { listOf(AppTab.Home, AppTab.Online, AppTab.Library, AppTab.Search) }
    val navShape = RoundedCornerShape(if (compact) 30.dp else 32.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = if (compact) 0.dp else 2.dp)
            .animateContentSize(animationSpec = tween(220)),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(if (compact) 56.dp else 64.dp)
                .clip(navShape)
                .background(MaterialTheme.colorScheme.surface, navShape)
                .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), navShape)
                .padding(5.dp)
                .animateContentSize(animationSpec = tween(220))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = if (compact) Arrangement.Center else Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visibleTabs = if (compact) listOf(selectedTab.takeIf { it != AppTab.Search } ?: AppTab.Home) else allTabs.filter { it != AppTab.Search }
                visibleTabs.forEach { tab ->
                    val selected = selectedTab == tab
                    val tabTint by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(220),
                        label = "tabTint_${tab.name}"
                    )
                    val activePillBg by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        animationSpec = tween(220),
                        label = "pillBg_${tab.name}"
                    )
                    Box(
                        modifier = Modifier
                            .then(if (compact) Modifier.fillMaxSize() else Modifier.weight(1f).fillMaxHeight())
                            .clip(RoundedCornerShape(24.dp))
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
                                modifier = Modifier.size(if (compact) 23.dp else 22.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = tabTint,
                                maxLines = 1,
                                letterSpacing = 0.sp
                            )
                        }
                    }
                }
            }
        }

        if (compact) {
            // The Figma collapsed state keeps Search as a separate round-ended control.
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(navShape)
                    .background(MaterialTheme.colorScheme.surface, navShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), navShape)
                    .clickable { onTabSelected(AppTab.Search) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppTab.Search.selectedIcon,
                    contentDescription = "Search",
                    tint = if (selectedTab == AppTab.Search) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
