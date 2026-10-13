package com.rnmapbox.rnmbx.components.mapview.helpers

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.common.Cancelable
import com.mapbox.maps.CameraChanged
import com.mapbox.maps.CameraChangedCallback
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsLifecycleListener
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.maps.plugin.animation.CameraAnimatorType
import com.mapbox.maps.plugin.animation.MapAnimationOwnerRegistry
import com.mapbox.maps.plugin.gestures.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

class CameraGestureDetectorsTest {
    private class Fixture {
        val map = mock<MapboxMap>()
        val gestures = mock<GesturesPlugin>()
        val animations = mock<CameraAnimationsPlugin>()
        val subscription = mock<Cancelable>()
        val moves = mutableListOf<OnMoveListener>()
        val scales = mutableListOf<OnScaleListener>()
        val rotates = mutableListOf<OnRotateListener>()
        val shoves = mutableListOf<OnShoveListener>()
        val lifecycle = mutableListOf<CameraAnimationsLifecycleListener>()
        lateinit var camera: CameraChangedCallback
        init {
            Mockito.doAnswer {
                it.getArgument<GesturesPlugin.() -> Unit>(0).invoke(gestures)
                Unit
            }.`when`(map).gesturesPlugin(any())
            Mockito.doAnswer {
                it.getArgument<CameraAnimationsPlugin.() -> Unit>(0).invoke(animations)
                Unit
            }.`when`(map).cameraAnimationsPlugin(any())
            Mockito.doAnswer { camera = it.getArgument(0); subscription }.`when`(map).subscribeCameraChanged(any())
            Mockito.doAnswer { moves.add(it.getArgument(0)); Unit }.`when`(gestures).addOnMoveListener(any())
            Mockito.doAnswer { moves.remove(it.getArgument<OnMoveListener>(0)); Unit }.`when`(gestures).removeOnMoveListener(any())
            Mockito.doAnswer { scales.add(it.getArgument(0)); Unit }.`when`(gestures).addOnScaleListener(any())
            Mockito.doAnswer { scales.remove(it.getArgument<OnScaleListener>(0)); Unit }.`when`(gestures).removeOnScaleListener(any())
            Mockito.doAnswer { rotates.add(it.getArgument(0)); Unit }.`when`(gestures).addOnRotateListener(any())
            Mockito.doAnswer { rotates.remove(it.getArgument<OnRotateListener>(0)); Unit }.`when`(gestures).removeOnRotateListener(any())
            Mockito.doAnswer { shoves.add(it.getArgument(0)); Unit }.`when`(gestures).addOnShoveListener(any())
            Mockito.doAnswer { shoves.remove(it.getArgument<OnShoveListener>(0)); Unit }.`when`(gestures).removeOnShoveListener(any())
            Mockito.doAnswer { lifecycle.add(it.getArgument(0)); Unit }.`when`(animations).addCameraAnimationsLifecycleListener(any())
            Mockito.doAnswer { lifecycle.remove(it.getArgument<CameraAnimationsLifecycleListener>(0)); Unit }.`when`(animations).removeCameraAnimationsLifecycleListener(any())
        }
        fun assertDetached() {
            assertTrue(moves.isEmpty()); assertTrue(scales.isEmpty())
            assertTrue(rotates.isEmpty()); assertTrue(shoves.isEmpty()); assertTrue(lifecycle.isEmpty())
        }
    }

    @Test
    fun `pinch ending does not clear a continuing pan`() {
        val f = Fixture(); val detector = MapCameraChangeDetector(f.map)
        var active = false; var interaction = false
        detector.onCameraChanged = { _, a, i -> active = a; interaction = i }
        detector.attach()
        f.moves.single().onMoveBegin(mock())
        f.scales.single().onScaleBegin(mock())
        f.scales.single().onScaleEnd(mock())
        f.camera.run(mock())
        assertTrue(active); assertTrue(interaction)
        f.moves.single().onMoveEnd(mock())
        f.camera.run(mock())
        assertFalse(active); assertFalse(interaction)
    }

    @Test
    fun `gesture animation remains active until all animator instances finish`() {
        val f = Fixture(); val detector = MapCameraChangeDetector(f.map)
        var interaction = false
        detector.onCameraChanged = { _, _, i -> interaction = i }
        detector.attach()
        val center = mock<ValueAnimator>(); val zoom = mock<ValueAnimator>()
        val l = f.lifecycle.single(); val owner = MapAnimationOwnerRegistry.GESTURES
        l.onAnimatorStarting(CameraAnimatorType.CENTER, center, owner)
        l.onAnimatorStarting(CameraAnimatorType.ZOOM, zoom, owner)
        l.onAnimatorEnding(CameraAnimatorType.CENTER, center, owner)
        f.camera.run(mock()); assertTrue(interaction)
        l.onAnimatorCancelling(CameraAnimatorType.ZOOM, zoom, owner)
        l.onAnimatorEnding(CameraAnimatorType.ZOOM, zoom, owner)
        f.camera.run(mock()); assertFalse(interaction)
    }

    @Test
    fun `cancelled old animator cannot erase replacement or concurrent inertia`() {
        val f = Fixture(); val detector = MapCameraChangeDetector(f.map)
        var interaction = false
        detector.onCameraChanged = { _, _, i -> interaction = i }
        detector.attach()
        val old = mock<ValueAnimator>(); val replacement = mock<ValueAnimator>(); val zoom = mock<ValueAnimator>()
        val l = f.lifecycle.single(); val owner = MapAnimationOwnerRegistry.GESTURES
        l.onAnimatorStarting(CameraAnimatorType.CENTER, old, owner)
        l.onAnimatorStarting(CameraAnimatorType.ZOOM, zoom, owner)
        l.onAnimatorStarting(CameraAnimatorType.CENTER, replacement, "developer")
        l.onAnimatorInterrupting(CameraAnimatorType.CENTER, old, owner, replacement, "developer")
        l.onAnimatorCancelling(CameraAnimatorType.CENTER, old, owner)
        f.camera.run(mock()); assertTrue(interaction)
        l.onAnimatorEnding(CameraAnimatorType.ZOOM, zoom, owner)
        f.camera.run(mock()); assertFalse(interaction)
    }

    @Test
    fun `developer and location animations are not user interaction`() {
        val f = Fixture(); val detector = MapCameraChangeDetector(f.map)
        var interaction = true
        detector.onCameraChanged = { _, _, i -> interaction = i }
        detector.attach()
        val l = f.lifecycle.single()
        l.onAnimatorStarting(CameraAnimatorType.CENTER, mock(), "developer")
        l.onAnimatorStarting(CameraAnimatorType.ZOOM, mock(), MapAnimationOwnerRegistry.LOCATION)
        f.camera.run(mock()); assertFalse(interaction)
    }

    @Test
    fun `camera observer repeatedly detaches every listener and ignores late callbacks`() {
        val f = Fixture(); val detector = MapCameraChangeDetector(f.map)
        var callbacks = 0
        detector.onCameraChanged = { _, _, _ -> callbacks++ }
        repeat(100) {
            detector.attach(); detector.attach()
            assertEquals(1, f.moves.size); assertEquals(1, f.lifecycle.size)
            detector.detach(); detector.detach()
            f.assertDetached()
            f.camera.run(mock())
        }
        assertEquals(0, callbacks)
        Mockito.verify(f.subscription, Mockito.times(100)).cancel()
    }

    private fun withHandler(body: (MutableList<Pair<Runnable, Long>>) -> Unit) {
        val pending = mutableListOf<Pair<Runnable, Long>>()
        Mockito.mockStatic(Looper::class.java).use { looper ->
            looper.`when`<Looper> { Looper.getMainLooper() }.thenReturn(mock())
            Mockito.mockConstruction(Handler::class.java) { handler, _ ->
                Mockito.doAnswer { pending.add(it.getArgument<Runnable>(0) to it.getArgument<Long>(1)); true }.`when`(handler).postDelayed(any(), any())
                Mockito.doAnswer { val r = it.getArgument<Runnable>(0); pending.removeAll { p -> p.first === r }; Unit }.`when`(handler).removeCallbacks(any())
            }.use { body(pending) }
        }
    }

    @Test
    fun `timeout cancels quiet check and changing timing props reschedules timers`() = withHandler { pending ->
        val f = Fixture(); val detector = MapSteadyDetector(f.map, 200.0, 100.0)
        var timeouts = 0; var steady = 0
        detector.onTimeout = { timeouts++ }; detector.onSteady = { _, _ -> steady++ }
        detector.attach()
        f.moves.single().onMoveBegin(mock()); f.moves.single().onMoveEnd(mock())
        assertEquals(setOf(100L, 200L), pending.map { it.second }.toSet())
        val timeout = pending.first { it.second == 100L }; pending.remove(timeout); timeout.first.run()
        assertEquals(1, timeouts); assertEquals(0, steady)
        assertFalse(pending.any { it.second == 200L })
        detector.maxIntervalMs = 400.0
        assertEquals(listOf(400L), pending.map { it.second })
        detector.quietPeriodMs = 700.0
        assertEquals(setOf(400L, 700L), pending.map { it.second }.toSet())
        detector.detach(); assertTrue(pending.isEmpty()); f.assertDetached()
    }

    @Test
    fun `steady detector does not settle overlapping gestures or cancelled duplicate animators`() = withHandler { pending ->
        val f = Fixture(); val detector = MapSteadyDetector(f.map, 0.0)
        var steady = 0
        detector.onSteady = { _, _ -> steady++ }; detector.attach()
        f.moves.single().onMoveBegin(mock()); f.scales.single().onScaleBegin(mock())
        f.scales.single().onScaleEnd(mock()); assertEquals(0, steady)
        val l = f.lifecycle.single(); val animator = mock<ValueAnimator>(); val owner = MapAnimationOwnerRegistry.GESTURES
        l.onAnimatorStarting(CameraAnimatorType.ZOOM, animator, owner)
        f.moves.single().onMoveEnd(mock()); assertEquals(0, steady)
        l.onAnimatorCancelling(CameraAnimatorType.ZOOM, animator, owner); assertEquals(1, steady)
        l.onAnimatorEnding(CameraAnimatorType.ZOOM, animator, owner); assertEquals(1, steady)
        val move = f.moves.single()
        detector.detach(); move.onMoveBegin(mock())
        assertTrue(pending.isEmpty()); f.assertDetached()
    }
}
