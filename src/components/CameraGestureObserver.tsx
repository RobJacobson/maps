import { memo, useCallback } from 'react';
import { Platform, type ViewProps } from 'react-native';
import RNMBXCameraGestureObserverNativeComponent, {
  type OnMapSteadyEvent,
  type OnCameraChangedEvent,
} from '../specs/RNMBXCameraGestureObserverNativeComponent';
import type { MapState } from './MapView';

type Props = ViewProps & {
  /**
   * Time in milliseconds to wait after the last user gesture or gesture animation
   * before emitting 'steady'. Default is 200ms.
   */
  quietPeriodMs?: number;
  /** Maximum time before emitting 'timeout' during continuous activity. */
  maxIntervalMs?: number;
  /** Callback when user gestures and their camera animations have settled. */
  onMapSteady?: (event: { nativeEvent: OnMapSteadyEvent }) => void;
  /**
   * Callback on camera changes, using the same MapState shape as MapView.onCameraChanged.
   * gestures.isGestureActive reports active touch gestures; gestures.isUserInteraction
   * also includes camera animations owned by gestures (such as fling inertia).
   * Programmatic camera changes report false, even while the location puck is pulsing.
   * This event is independent of MapIdle and is currently supported on Android only.
   * @platform android
   */
  onCameraChanged?: (state: MapState) => void;
};

/**
 * CameraGestureObserver
 *
 * Observes user gesture settlement and camera changes independently of renderer idle.
 */
export default memo((props: Props) => {
  const { onCameraChanged, ...rest } = props;
  const handleCameraChanged = useCallback(
    (event: { nativeEvent: OnCameraChangedEvent }) => {
      onCameraChanged?.(event.nativeEvent);
    },
    [onCameraChanged],
  );
  const enabled = Platform.OS === 'android' && !!onCameraChanged;
  return (
    <RNMBXCameraGestureObserverNativeComponent
      {...rest}
      hasOnMapSteady={!!props.onMapSteady}
      hasOnCameraChanged={enabled}
      onCameraChanged={enabled ? handleCameraChanged : undefined}
    />
  );
});
