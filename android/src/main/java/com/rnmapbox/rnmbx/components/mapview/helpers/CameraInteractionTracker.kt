package com.rnmapbox.rnmbx.components.mapview.helpers

import com.mapbox.maps.plugin.animation.MapAnimationOwnerRegistry
import com.rnmapbox.rnmbx.components.mapview.MapGestureType
import java.util.IdentityHashMap

/** Camera interaction state, independent of renderer activity and MapIdle. */
internal class CameraInteractionTracker {
    private val gestures = mutableSetOf<MapGestureType>()
    private val animations = IdentityHashMap<Any, Boolean>()

    val isGestureActive: Boolean get() = gestures.isNotEmpty()
    val activeGestureAnimations: Int get() = animations.values.count { it }
    val isUserInteraction: Boolean get() = isGestureActive || activeGestureAnimations > 0

    fun gestureStarted(type: MapGestureType) { gestures.add(type) }
    fun gestureEnded(type: MapGestureType) { gestures.remove(type) }

    fun animationStarted(animator: Any, owner: String?) {
        animations[animator] = owner == MapAnimationOwnerRegistry.GESTURES
    }

    // Cancellation and end can refer to the same animator. Remove by identity, once.
    fun animationEnded(animator: Any): Boolean {
        val wasGestureAnimation = animations[animator] == true
        animations.remove(animator)
        return wasGestureAnimation
    }

    fun clear() {
        gestures.clear()
        animations.clear()
    }
}
