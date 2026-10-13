package com.rnmapbox.rnmbx.components.mapview.helpers

import android.animation.ValueAnimator
import com.mapbox.common.Cancelable
import com.mapbox.maps.CameraChanged
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsLifecycleListener
import com.mapbox.maps.plugin.animation.CameraAnimatorType

/** Derives camera interaction from gestures and animators, never from MapIdle. */
class MapCameraChangeDetector(private val mapboxMap: MapboxMap) {
    var onCameraChanged: ((CameraChanged, Boolean, Boolean) -> Unit)? = null
    private val interaction = CameraInteractionTracker()
    private var attached = false
    private var cameraChangedSubscription: Cancelable? = null
    private val gestures = CameraGestureListeners(
        { if (attached) interaction.gestureStarted(it) },
        { if (attached) interaction.gestureEnded(it) },
    )

    private val animations = object : CameraAnimationsLifecycleListener {
        override fun onAnimatorStarting(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            if (attached) interaction.animationStarted(animator, owner)
        }

        override fun onAnimatorEnding(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            interaction.animationEnded(animator)
        }

        override fun onAnimatorCancelling(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            interaction.animationEnded(animator)
        }

        override fun onAnimatorInterrupting(
            type: CameraAnimatorType,
            runningAnimator: ValueAnimator,
            runningAnimatorOwner: String?,
            newAnimator: ValueAnimator,
            newAnimatorOwner: String?
        ) {
            // The SDK starts the replacement before notifying interruption, then cancels
            // the previous animator. Its identity must not clear the replacement's state.
        }
    }

    fun attach() {
        if (attached) return
        attached = true
        gestures.attach(mapboxMap)
        mapboxMap.cameraAnimationsPlugin { addCameraAnimationsLifecycleListener(animations) }
        cameraChangedSubscription = mapboxMap.subscribeCameraChanged {
            if (attached) onCameraChanged?.invoke(it, interaction.isGestureActive, interaction.isUserInteraction)
        }
    }

    fun detach() {
        if (!attached) return
        attached = false
        cameraChangedSubscription?.cancel()
        cameraChangedSubscription = null
        gestures.detach(mapboxMap)
        mapboxMap.cameraAnimationsPlugin { removeCameraAnimationsLifecycleListener(animations) }
        interaction.clear()
    }
}
