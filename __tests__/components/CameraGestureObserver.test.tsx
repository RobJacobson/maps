import { render } from '@testing-library/react-native';
import { Platform } from 'react-native';
import CameraGestureObserver from '../../src/components/CameraGestureObserver';
import NativeObserver from '../../src/specs/RNMBXCameraGestureObserverNativeComponent';
import type { MapState } from '../../src/components/MapView';

jest.mock('../../src/specs/RNMBXCameraGestureObserverNativeComponent', () => ({
  __esModule: true,
  default: 'RNMBXCameraGestureObserver',
}));

const state: MapState = {
  properties: {
    center: [0, 0],
    bounds: { ne: [1, 1], sw: [-1, -1] },
    zoom: 10,
    heading: 0,
    pitch: 0,
  },
  gestures: { isGestureActive: false, isUserInteraction: false },
  timestamp: 123,
};

describe('CameraGestureObserver', () => {
  const originalOS = Platform.OS;
  afterEach(() => {
    Platform.OS = originalOS;
  });

  test('passes MapState directly and uses the latest callback after a prop update', () => {
    Platform.OS = 'android';
    const first = jest.fn();
    const next = jest.fn();
    const result = render(<CameraGestureObserver onCameraChanged={first} />);
    let native = result.UNSAFE_getByType(NativeObserver);
    expect(native.props.hasOnCameraChanged).toBe(true);
    native.props.onCameraChanged({ nativeEvent: state });
    expect(first).toHaveBeenCalledWith(state);
    result.rerender(<CameraGestureObserver onCameraChanged={next} />);
    native = result.UNSAFE_getByType(NativeObserver);
    native.props.onCameraChanged({ nativeEvent: state });
    expect(first).toHaveBeenCalledTimes(1);
    expect(next).toHaveBeenCalledWith(state);
  });

  test('does not register the Android camera event on iOS', () => {
    Platform.OS = 'ios';
    const result = render(
      <CameraGestureObserver onCameraChanged={jest.fn()} />,
    );
    const native = result.UNSAFE_getByType(NativeObserver);
    expect(native.props.hasOnCameraChanged).toBe(false);
    expect(native.props.onCameraChanged).toBeUndefined();
  });
});
