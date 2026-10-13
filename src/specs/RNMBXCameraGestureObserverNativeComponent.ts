import type { ViewProps } from 'react-native';
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
// prettier-ignore
// @ts-ignore - CI environment type resolution issue for CodegenTypes
import { DirectEventHandler, Double } from 'react-native/Libraries/Types/CodegenTypes';
import type { UnsafeMixed } from './codegenUtils';

export type OnMapSteadyEvent = {
  reason: 'steady' | 'timeout';
  idleDurationMs?: Double;
  lastGestureType?: string | null | undefined;
  timestamp: Double;
};

export type OnCameraChangedEvent = {
  properties: {
    center: Double[];
    bounds: { ne: Double[]; sw: Double[] };
    zoom: Double;
    heading: Double;
    pitch: Double;
  };
  gestures: {
    isGestureActive: boolean;
    isUserInteraction: boolean;
  };
  timestamp: Double;
};

export interface NativeProps extends ViewProps {
  quietPeriodMs?: UnsafeMixed<number>;
  maxIntervalMs?: UnsafeMixed<number>;
  hasOnMapSteady: UnsafeMixed<boolean>;
  onMapSteady?: DirectEventHandler<OnMapSteadyEvent>;
  hasOnCameraChanged: UnsafeMixed<boolean>;
  onCameraChanged?: DirectEventHandler<OnCameraChangedEvent>;
}

export default codegenNativeComponent<NativeProps>(
  'RNMBXCameraGestureObserver',
);
