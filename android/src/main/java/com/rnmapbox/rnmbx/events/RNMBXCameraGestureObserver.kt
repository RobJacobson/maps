package com.rnmapbox.rnmbx.events

import android.content.Context
import com.rnmapbox.rnmbx.components.AbstractMapFeature
import com.rnmapbox.rnmbx.components.RemovalReason
import com.rnmapbox.rnmbx.components.mapview.MapGestureType
import com.rnmapbox.rnmbx.components.mapview.RNMBXMapView
import com.rnmapbox.rnmbx.components.mapview.helpers.MapCameraChangeDetector
import com.rnmapbox.rnmbx.components.mapview.helpers.MapSteadyDetector

class RNMBXCameraGestureObserver(
    context: Context,
    private val manager: RNMBXCameraGestureObserverManager
) : AbstractMapFeature(context) {
    var hasOnMapSteady = false
        set(value) { field = value; updateDetectors() }
    var hasOnCameraChanged = false
        set(value) { field = value; updateDetectors() }
    var quietPeriodMs: Double? = null
        set(value) { field = value; mapSteadyDetector?.quietPeriodMs = value ?: 200.0 }
    var maxIntervalMs: Double? = null
        set(value) { field = value; mapSteadyDetector?.maxIntervalMs = value }
    override var requiresStyleLoad = false
    private var mapSteadyDetector: MapSteadyDetector? = null
    private var mapCameraChangeDetector: MapCameraChangeDetector? = null

    private fun normalizeGestureType(type: MapGestureType?): String? = when (type) {
        MapGestureType.Move -> "pan"
        MapGestureType.Scale -> "pinch"
        MapGestureType.Rotate -> "rotate"
        MapGestureType.Shove -> "pitch"
        MapGestureType.Fling -> "fling"
        null -> null
    }

    override fun addToMap(mapView: RNMBXMapView) {
        super.addToMap(mapView)
        updateDetectors()
    }

    private fun updateDetectors() {
        val mapView = mMapView ?: return
        if (!hasOnMapSteady) {
            mapSteadyDetector?.detach()
            mapSteadyDetector = null
        }
        if (!hasOnCameraChanged) {
            mapCameraChangeDetector?.detach()
            mapCameraChangeDetector = null
        }
        mapView.getMapAsync { mapboxMap ->
            if (mMapView !== mapView) return@getMapAsync
            if (hasOnMapSteady && mapSteadyDetector == null) {
                val detector = MapSteadyDetector(mapboxMap, quietPeriodMs ?: 200.0, maxIntervalMs)
                detector.onSteady = { duration, gesture ->
                    manager.handleEvent(MapSteadyEvent.make(this, "steady", duration, normalizeGestureType(gesture)))
                }
                detector.onTimeout = { gesture ->
                    manager.handleEvent(MapSteadyEvent.make(this, "timeout", null, normalizeGestureType(gesture)))
                }
                mapSteadyDetector = detector
                detector.attach()
            }
            if (hasOnCameraChanged && mapCameraChangeDetector == null) {
                val detector = MapCameraChangeDetector(mapboxMap)
                detector.onCameraChanged = { camera, active, interaction ->
                    manager.handleEvent(MapCameraChangeEvent.make(this, mapboxMap, camera, active, interaction))
                }
                mapCameraChangeDetector = detector
                detector.attach()
            }
        }
    }

    override fun removeFromMap(mapView: RNMBXMapView, reason: RemovalReason): Boolean {
        mapSteadyDetector?.detach()
        mapSteadyDetector = null
        mapCameraChangeDetector?.detach()
        mapCameraChangeDetector = null
        return super.removeFromMap(mapView, reason)
    }
}
