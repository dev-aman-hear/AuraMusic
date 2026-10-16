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
    compact: Boolean = false,
    compactPlayer: (@Composable () -> Unit)? = null
) {
    val allTabs = remember { listOf(AppTab.Home, AppTab.Online, AppTab.Library, AppTab.Search) }
    val navShape = RoundedCornerShape(if (compact) 24.dp else 27.dp)

    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 2.dp)
                .animateContentSize(animationSpec = tween(220)),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isHomeSelected = selectedTab == AppTab.Home
            val homeTint by animateColorAsState(
                targetValue = if (isHomeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "compactHomeTint"
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(navShape)
                    .background(MaterialTheme.colorScheme.surface, navShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), navShape)
                    .clickable { onTabSelected(AppTab.Home) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = AppTab.Home.selectedIcon,
                        contentDescription = AppTab.Home.label,
                        tint = homeTint,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = AppTab.Home.label,
                        fontSize = 8.5.sp,
                        fontWeight = if (isHomeSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = homeTint,
                        maxLines = 1
                    )
                }
            }

            if (compactPlayer != null) {
                Box(modifier = Modifier.weight(1f)) {
                    compactPlayer()
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(navShape)
                        .background(MaterialTheme.colorScheme.surface, navShape)
                        .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), navShape)
                        .clickable { onTabSelected(AppTab.Home) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "AuraMusic",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val isSearchSelected = selectedTab == AppTab.Search
            val searchTint by animateColorAsState(
                targetValue = if (isSearchSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "compactSearchTint"
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(navShape)
                    .background(MaterialTheme.colorScheme.surface, navShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), navShape)
                    .clickable { onTabSelected(AppTab.Search) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppTab.Search.selectedIcon,
                    contentDescription = "Search",
                    tint = searchTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 6.dp, top = 2.dp)
                .animateContentSize(animationSpec = tween(220)),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(navShape)
                    .background(MaterialTheme.colorScheme.surface, navShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), navShape)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allTabs.forEach { tab ->
                        if (tab != AppTab.Search) {
                            val selected = selectedTab == tab
                            val tabTint by animateColorAsState(
                                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(220),
                                label = "tabTint_${tab.name}"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(22.dp))
                                    .clickable { onTabSelected(tab) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                        tint = tabTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tab.label,
                                        fontSize = 9.5.sp,
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
            }

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(navShape)
                    .background(MaterialTheme.colorScheme.surface, navShape)
                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline), navShape)
                    .clickable { onTabSelected(AppTab.Search) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppTab.Search.selectedIcon,
                    contentDescription = "Search",
                    tint = if (selectedTab == AppTab.Search) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}
