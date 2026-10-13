package com.rnmapbox.rnmbx.components.mapview.helpers

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsLifecycleListener
import com.mapbox.maps.plugin.animation.CameraAnimatorType
import com.mapbox.maps.plugin.animation.MapAnimationOwnerRegistry
import com.rnmapbox.rnmbx.components.mapview.MapGestureType

class MapSteadyDetector(
    private val mapboxMap: MapboxMap,
    quietPeriodMs: Double = 200.0,
    maxIntervalMs: Double? = null,
) {
    var onSteady: ((Double, MapGestureType?) -> Unit)? = null
    var onTimeout: ((MapGestureType?) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val interaction = CameraInteractionTracker()
    private var attached = false
    private var hasActivity = false
    private var lastGestureType: MapGestureType? = null
    var lastTransitionEndedAtMs: Double? = null
        private set
    val activeAnimations: Int get() = interaction.activeGestureAnimations
    val isGestureActive: Boolean get() = interaction.isGestureActive
    private var quietRunnable: Runnable? = null
    private var timeoutRunnable: Runnable? = null

    var quietPeriodMs: Double = quietPeriodMs
        set(value) {
            field = value
            if (hasActivity) scheduleQuietCheck()
        }
    var maxIntervalMs: Double? = maxIntervalMs
        set(value) {
            field = value
            cancelTimeoutTimer()
            if (hasActivity) scheduleTimeoutTimer()
        }

    private fun nowMs(): Double = System.currentTimeMillis().toDouble()
    private val gestures = CameraGestureListeners(::handleGestureBegin, ::handleGestureEnd)
    private val animations = object : CameraAnimationsLifecycleListener {
        override fun onAnimatorStarting(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            if (!attached || owner != MapAnimationOwnerRegistry.GESTURES) return
            interaction.animationStarted(animator, owner)
            lastTransitionEndedAtMs = null
            markActivity()
        }

        override fun onAnimatorEnding(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            handleAnimatorEnd(animator)
        }

        override fun onAnimatorCancelling(type: CameraAnimatorType, animator: ValueAnimator, owner: String?) {
            handleAnimatorEnd(animator)
        }

        override fun onAnimatorInterrupting(
            type: CameraAnimatorType,
            runningAnimator: ValueAnimator,
            runningAnimatorOwner: String?,
            newAnimator: ValueAnimator,
            newAnimatorOwner: String?
        ) {
            // Start/cancel callbacks track the replacement and previous animator by identity.
        }
    }

    fun attach() {
        if (attached) return
        attached = true
        gestures.attach(mapboxMap)
        mapboxMap.cameraAnimationsPlugin { addCameraAnimationsLifecycleListener(animations) }
    }

    fun detach() {
        if (!attached) return
        attached = false
        cancelQuietTimer()
        cancelTimeoutTimer()
        gestures.detach(mapboxMap)
        mapboxMap.cameraAnimationsPlugin { removeCameraAnimationsLifecycleListener(animations) }
        interaction.clear()
        hasActivity = false
        lastGestureType = null
        lastTransitionEndedAtMs = null
    }

    private fun cancelQuietTimer() {
        quietRunnable?.let { handler.removeCallbacks(it) }
        quietRunnable = null
    }

    private fun cancelTimeoutTimer() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun scheduleQuietCheck() {
        cancelQuietTimer()
        if (!attached) return
        if (quietPeriodMs <= 0) {
            maybeEmitSteady()
            return
        }
        val runnable = Runnable { maybeEmitSteady() }
        quietRunnable = runnable
        handler.postDelayed(runnable, quietPeriodMs.toLong())
    }

    private fun scheduleTimeoutTimer() {
        if (!attached || timeoutRunnable != null) return
        val delay = maxIntervalMs ?: return
        if (delay <= 0) return
        val runnable = Runnable {
            timeoutRunnable = null
            if (attached) {
                cancelQuietTimer()
                onTimeout?.invoke(lastGestureType)
                scheduleTimeoutTimer()
            }
        }
        timeoutRunnable = runnable
        handler.postDelayed(runnable, delay.toLong())
    }

    private fun markActivity() {
        hasActivity = true
        scheduleQuietCheck()
        scheduleTimeoutTimer()
    }

    private fun maybeEmitSteady() {
        if (!attached || interaction.isUserInteraction) return
        val lastEnd = lastTransitionEndedAtMs ?: return
        val sinceEnd = nowMs() - lastEnd
        if (sinceEnd < quietPeriodMs) {
            cancelQuietTimer()
            val runnable = Runnable { maybeEmitSteady() }
            quietRunnable = runnable
            handler.postDelayed(runnable, (quietPeriodMs - sinceEnd).toLong().coerceAtLeast(1))
            return
        }
        cancelQuietTimer()
        cancelTimeoutTimer()
        hasActivity = false
        val gesture = lastGestureType
        lastGestureType = null
        lastTransitionEndedAtMs = null
        onSteady?.invoke(sinceEnd, gesture)
    }

    private fun handleAnimatorEnd(animator: ValueAnimator) {
        if (!attached || !interaction.animationEnded(animator)) return
        lastTransitionEndedAtMs = if (interaction.isUserInteraction) null else nowMs()
        scheduleQuietCheck()
    }

    private fun handleGestureBegin(type: MapGestureType) {
        if (!attached) return
        interaction.gestureStarted(type)
        lastGestureType = type
        lastTransitionEndedAtMs = null
        markActivity()
    }

    private fun handleGestureEnd(type: MapGestureType) {
        if (!attached) return
        interaction.gestureEnded(type)
        lastGestureType = type
        lastTransitionEndedAtMs = if (interaction.isUserInteraction) null else nowMs()
        markActivity()
    }
}
