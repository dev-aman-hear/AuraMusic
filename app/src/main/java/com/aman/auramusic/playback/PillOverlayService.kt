package com.aman.auramusic.playback

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.aman.auramusic.AuraMusicPill
import kotlinx.coroutines.flow.MutableStateFlow

class PillOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    private val _orientation = MutableStateFlow(Configuration.ORIENTATION_PORTRAIT)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        _orientation.value = resources.configuration.orientation
        showOverlay()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        _orientation.value = newConfig.orientation
    }

    private fun showOverlay() {
        if (composeView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = 0
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PillOverlayService)
            setViewTreeViewModelStoreOwner(this@PillOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PillOverlayService)
            
            setContent {
                val song by PillStateManager.currentSong.collectAsState()
                val isPlaying by PillStateManager.isPlaying.collectAsState()
                val pillPosition by PillStateManager.pillPosition.collectAsState()
                val verticalOffsetDp by PillStateManager.pillVerticalOffset.collectAsState()
                val pillSizeScale by PillStateManager.pillSizeScale.collectAsState()
                val orientation by _orientation.collectAsState()
                val isLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE
                
                val density = LocalDensity.current
                val verticalOffsetPx = with(density) { verticalOffsetDp.dp.toPx() }.toInt()
                val horizontalPaddingPx = with(density) { 8.dp.toPx() }.toInt()
                val expansionOffsetPx = with(density) { 40.dp.toPx() }.toInt()
                
                var isExpanded by remember { mutableStateOf(value = false) }

                // Force window update on orientation or expansion change
                LaunchedEffect(isExpanded, isLandscape, pillPosition, verticalOffsetPx) {
                    composeView?.let { view ->
                        try {
                            if (isLandscape) {
                                params.width = 1
                                params.height = 1
                                params.alpha = 0f
                            } else {
                                params.width = WindowManager.LayoutParams.WRAP_CONTENT
                                params.height = WindowManager.LayoutParams.WRAP_CONTENT
                                params.alpha = 1f
                                
                                // Position based on pixel offset
                                params.y = if (isExpanded) verticalOffsetPx + expansionOffsetPx else verticalOffsetPx
                                
                                params.gravity = if (isExpanded) {
                                    Gravity.TOP or Gravity.CENTER_HORIZONTAL
                                } else {
                                    when (pillPosition) {
                                        0 -> Gravity.TOP or Gravity.START
                                        2 -> Gravity.TOP or Gravity.END
                                        else -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
                                    }
                                }
                                
                                // Add horizontal padding for side positions
                                if (!isExpanded) {
                                    params.x = when (pillPosition) {
                                        0 -> horizontalPaddingPx
                                        2 -> horizontalPaddingPx
                                        else -> 0
                                    }
                                } else {
                                    params.x = 0
                                }
                            }
                            windowManager.updateViewLayout(view, params)
                        } catch (_: Exception) {
                            // View might not be attached yet
                        }
                    }
                }

                if (isLandscape) {
                    // Reset expansion state when hiding in landscape
                    if (isExpanded) isExpanded = false
                    return@setContent
                }

                AuraMusicPill(
                    song = song,
                    isPlaying = isPlaying,
                    isExpanded = isExpanded,
                    sizeScale = pillSizeScale,
                    onExpandToggle = { isExpanded = !isExpanded },
                    onPlayPause = { PlaybackActionRegistry.onPlayPause?.invoke() },
                    onNext = { PlaybackActionRegistry.onNext?.invoke() },
                    onPrevious = { PlaybackActionRegistry.onPrevious?.invoke() },
                ) {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    isExpanded = false
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        composeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        composeView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
