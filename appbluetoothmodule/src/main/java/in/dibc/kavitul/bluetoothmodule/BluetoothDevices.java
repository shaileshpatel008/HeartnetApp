package in.dibc.kavitul.bluetoothmodule;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Process;
import android.util.Log;
import android.view.View;

import java.util.HashSet;
import java.util.Set;


@SuppressLint("MissingPermission")
public class BluetoothDevices implements Runnable {
    String TAG = "DIBC_Bluetooth_Devices";

    public static Set<BluetoothDevice> pairedDevices = new HashSet<>();
    public static Set<BluetoothDevice> searchedDevices = new HashSet<>();
    private final AppBluetoothHelper btHelper;
    private static Resources res;

    public BluetoothDevices(AppBluetoothHelper bth) {
        btHelper = bth;
        res = btHelper.res;
    }

    public void getPairedDevices() {
        if (btHelper.bluetoothAdapter == null) {
            btHelper.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        Set<BluetoothDevice> allPairedDevices = btHelper.bluetoothAdapter.getBondedDevices();
        if (allPairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : allPairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (deviceName.startsWith(btHelper.res.getString(R.string.bt_prefix))) {
                    pairedDevices.add(device);
//                    ScanConnect.updateDeviceList(); dont list paired devices
                }
            }
        }
    }
    public static void addToSearchedDevices(BluetoothDevice device){
        try {
            String deviceName = device.getName();
            if(deviceName != null) {
                if (deviceName.startsWith(res.getString(R.string.bt_prefix))) {
                    searchedDevices.add(device);
                    ScanConnect.updateDeviceList();
                }
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
    }
    public void scanAvailableBluetoothDevices(View view){
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        btHelper.sourceActivity.registerReceiver(btHelper.bluetoothReceiver, filter);
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btHelper.sourceActivity.registerReceiver(btHelper.bluetoothReceiver, filter1);
        IntentFilter filter2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btHelper.sourceActivity.registerReceiver(btHelper.bluetoothReceiver, filter2);

        if(btHelper.bluetoothAdapter.isDiscovering()){
            btHelper.bluetoothAdapter.cancelDiscovery();
        }
            searchedDevices.clear();
            pairedDevices.clear();
            btHelper.bluetoothAdapter.startDiscovery();
            Log.d(TAG, "scanAvailableBluetoothDevices: search");

    }

}
