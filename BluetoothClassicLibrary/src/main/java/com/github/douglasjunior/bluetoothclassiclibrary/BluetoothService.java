package com.github.douglasjunior.bluetoothclassiclibrary;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

/**
 * Created by douglas on 23/03/15.
 */
public abstract class BluetoothService {
    // Debugging
    private static final String TAG = BluetoothService.class.getSimpleName();
    protected static final boolean D = true;

    protected static BluetoothService mDefaultServiceInstance;
    protected BluetoothConfiguration mConfig;
    protected BluetoothStatus mStatus;

    private final Handler handler;

    protected OnBluetoothEventCallback onEventCallback;

    protected OnBluetoothScanCallback onScanCallback;

    private static BluetoothConfiguration mDefaultConfiguration;

    protected BluetoothService(BluetoothConfiguration config) {
        this.mConfig = config;
        this.mStatus = BluetoothStatus.NONE;
        if (config.callListenersInMainThread) {
            handler = new Handler();
        } else {
            handler = null;
        }
    }

    public static void setDefaultConfiguration(BluetoothConfiguration config) {
        mDefaultConfiguration = config;
    }

    public synchronized static BluetoothService getDefaultInstance() {
        if (mDefaultServiceInstance == null) {
            try {
                Constructor<? extends BluetoothService> constructor =
                        (Constructor<? extends BluetoothService>) mDefaultConfiguration.bluetoothServiceClass.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                BluetoothService bluetoothService = constructor.newInstance(mDefaultConfiguration);
                mDefaultServiceInstance = bluetoothService;
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return mDefaultServiceInstance;
    }

    public void setOnEventCallback(OnBluetoothEventCallback onEventCallback) {
        this.onEventCallback = onEventCallback;
    }

    public void setOnScanCallback(OnBluetoothScanCallback onScanCallback) {
        this.onScanCallback = onScanCallback;
    }

    public abstract void write(byte[] bytes);

    public BluetoothConfiguration getConfiguration() {
        return mConfig;
    }

    protected synchronized void updateState(final BluetoothStatus status) {
        Log.v(TAG, "updateStatus() " + mStatus + " -> " + status);
        mStatus = status;

        // Give the new state to the Handler so the UI Activity can update
        if (onEventCallback != null)
            runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    onEventCallback.onStatusChange(status);
                }
            });
    }

    protected void runOnMainThread(Runnable runnable, long delayMillis) {
        if (!mConfig.callListenersInMainThread || Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            if (delayMillis > 0)
                handler.postDelayed(runnable, delayMillis);
            else
                handler.post(runnable);
        }
    }

    protected void runOnMainThread(Runnable runnable) {
        runOnMainThread(runnable, 0);
    }

    public synchronized BluetoothStatus getStatus() {
        return mStatus;
    }

    public abstract void stopService();

    public abstract void connect(BluetoothDevice device);

    public abstract void startScan();

    public abstract void stopScan();

    public interface OnBluetoothEventCallback {
        void onDataRead(byte[] buffer, int length);

        void onStatusChange(BluetoothStatus status);

        void onDeviceName(String deviceName);

        void onToast(String message);

        void onDataWrite(byte[] buffer);
    }

    public interface OnBluetoothScanCallback {
        void onDeviceDiscovered(BluetoothDevice device, int rssi);

        void onStartScan();

        void onStopScan();
    }

    public static class BluetoothConfiguration {
        public Class<? extends BluetoothService> bluetoothServiceClass;
        public Context context;
        public String deviceName;
        public int bufferSize;
        public char characterDelimiter;
        public UUID uuid;
        public UUID uuidService;
        public UUID uuidCharacteristic;
        public boolean callListenersInMainThread = true;
    }
}
