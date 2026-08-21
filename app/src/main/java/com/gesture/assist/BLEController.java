package com.gesture.assist;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class BLEController {
    private static final String TAG = "BLEController";
    private static final String ESP32_MAC = "30:ED:A0:5A:36:A6";
    private static final UUID SERVICE_UUID = UUID.fromString("7c9e0001-1111-2222-3333-444444444444");
    private static final UUID CHAR_UUID = UUID.fromString("7c9e0002-1111-2222-3333-444444444444");

    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic characteristic;

    public void init() {
        BluetoothManager manager = (BluetoothManager) App.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        BluetoothDevice device = adapter.getRemoteDevice(ESP32_MAC);
        gatt = device.connectGatt(App.getContext(), false, new GattCallback());
    }

    public void sendDelta(int dx, int dy) {
        if (characteristic == null) return;
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) dx);
        buffer.putShort((short) dy);
        characteristic.setValue(buffer.array());
        gatt.writeCharacteristic(characteristic);
    }

    public void close() {
        if (gatt != null) gatt.close();
    }

    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    characteristic = service.getCharacteristic(CHAR_UUID);
                }
            }
        }
    }
}
