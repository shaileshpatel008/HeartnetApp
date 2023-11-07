package in.dibc.kavitul.bluetoothmodule;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import java.io.IOException;

@SuppressLint("MissingPermission")
public class AppBluetoothConnectionThread extends Thread {
    String TAG = "DIBC_Bluetooth_BluetoothConnection";

    AppBluetoothHelper btHelper;
    public BluetoothSocket bluetoothSocket;
    BluetoothSocket tmp = null;
    BluetoothDevice device;
    private byte[] mmBuffer; // mmBuffer store for the stream
    String connectionStatus = "";
    public AppBluetoothConnectionThread(BluetoothDevice dev, AppBluetoothHelper bth) {
        super("ConnectionThread");
        btHelper = bth;
        device = dev;
    }
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
        Log.d(TAG, "run: ");
        // Cancel discovery because it otherwise slows down the connection.
        if(btHelper.bluetoothAdapter.isDiscovering()){
            btHelper.bluetoothAdapter.cancelDiscovery();
        }
        // Use a temporary object that is later assigned to bluetoothSocket
        // because bluetoothSocket is final.
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            tmp = device.createRfcommSocketToServiceRecord(AppBluetoothHelper.APP_UUID);
            Log.d(TAG, "AppBluetoothConnectionThread: created socket for device" + device.getName());
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        bluetoothSocket = tmp;

        Log.d(TAG, "run: conecting");
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "AppBluetoothConnectionThread: already bonded to device.connecting");
                bluetoothSocket.connect();
            } else {
                Log.d(TAG, "run: createbond");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    device.createBond();
                } else {
                    Log.d(TAG, "run: device below kitkat. bond manually..");
                }
            }

        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            Log.d(TAG, "run: IO exeption: "+connectException);
            try {
                Log.e("","trying fallback...");
                if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "run: device already bonded");
                    bluetoothSocket = (BluetoothSocket) device.getClass()
                            .getMethod("createRfcommSocket", int.class)
                            .invoke(device, 1);
                    try {
                        bluetoothSocket.connect();
                        Log.e("","Connected");
                    } catch (IOException closeException) {
                        Log.e(TAG, "Could not establish a the bt socket", closeException);
                        bluetoothSocket = null;
                    }
                } else {
                    Log.d(TAG, "run: createbond");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        device.createBond();
                    } else {
                        Log.d(TAG, "run: device below kitkat. bond manually..");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // The connection attempt succeeded. Perform work associated with
        // the connection in a separate thread.
        if(bluetoothSocket != null){
            btHelper.bluetoothSocket = bluetoothSocket;
            connectionStatus = "connected";
            btHelper.connectedDevice = device;
            btHelper.connectedDeviceName = device.getName();
        }
        sendHandlerMsg();
    }

    // Closes the client socket and causes the thread to finish.
    public void disconnect() {
        Log.d(TAG, "disconnect: ");
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public AppDeviceConnectedThread getConnectedThread() {
        if (bluetoothSocket != null) {
            AppDeviceConnectedThread conn = new AppDeviceConnectedThread(bluetoothSocket);
            conn.getId();
            conn.start();
            return conn;
        } else {
            return null;
        }
    }

    private void sendHandlerMsg(){
        Bundle bundle = new Bundle();
        if(btHelper.mHandlersbluetooth != null) {
            Message msg = btHelper.mHandlersbluetooth.obtainMessage();
            bundle.putInt("deviceResponce", -1);
            bundle.putString("connectionStatus", connectionStatus);
            bundle.putString("deviceName", device.getName());
            msg.setData(bundle);
            btHelper.mHandlersbluetooth.sendMessage(msg);
        }
    }

}
