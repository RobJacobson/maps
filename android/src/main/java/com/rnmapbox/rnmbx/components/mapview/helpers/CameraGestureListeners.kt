package com.rnmapbox.rnmbx.components.mapview.helpers

import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnRotateListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.OnShoveListener
import com.rnmapbox.rnmbx.components.mapview.MapGestureType

/** Retains the exact listener instances needed to unregister from the SDK. */
internal class CameraGestureListeners(
    private val begin: (MapGestureType) -> Unit,
    private val end: (MapGestureType) -> Unit,
) {
    private val move = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) = begin(MapGestureType.Move)
        override fun onMove(detector: MoveGestureDetector): Boolean = false
        override fun onMoveEnd(detector: MoveGestureDetector) = end(MapGestureType.Move)
    }
    private val scale = object : OnScaleListener {
        override fun onScaleBegin(detector: StandardScaleGestureDetector) = begin(MapGestureType.Scale)
        override fun onScale(detector: StandardScaleGestureDetector) {}
        override fun onScaleEnd(detector: StandardScaleGestureDetector) = end(MapGestureType.Scale)
    }
    private val rotate = object : OnRotateListener {
        override fun onRotateBegin(detector: RotateGestureDetector) = begin(MapGestureType.Rotate)
        override fun onRotate(detector: RotateGestureDetector) {}
        override fun onRotateEnd(detector: RotateGestureDetector) = end(MapGestureType.Rotate)
    }
    private val shove = object : OnShoveListener {
        override fun onShoveBegin(detector: ShoveGestureDetector) = begin(MapGestureType.Shove)
        override fun onShove(detector: ShoveGestureDetector) {}
        override fun onShoveEnd(detector: ShoveGestureDetector) = end(MapGestureType.Shove)
    }

    fun attach(mapboxMap: MapboxMap) {
        mapboxMap.gesturesPlugin {
            addOnMoveListener(move)
            addOnScaleListener(scale)
            addOnRotateListener(rotate)
            addOnShoveListener(shove)
        }
    }

    fun detach(mapboxMap: MapboxMap) {
        mapboxMap.gesturesPlugin {
            removeOnMoveListener(move)
            removeOnScaleListener(scale)
            removeOnRotateListener(rotate)
            removeOnShoveListener(shove)
        }
    }
}
